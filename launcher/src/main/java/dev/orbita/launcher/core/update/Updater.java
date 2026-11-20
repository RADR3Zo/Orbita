package dev.orbita.launcher.core.update;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.orbita.launcher.LauncherInfo;
import dev.orbita.launcher.core.Http;
import dev.orbita.launcher.core.Progress;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Actualizaciones automáticas desde GitHub Releases.
 *
 * <p>Cómo funciona:
 * <ol>
 *   <li>Al abrir el launcher se pregunta a GitHub cuál es la última release del repositorio
 *       ({@code github_repo} en gradle.properties).</li>
 *   <li>Si su versión es más nueva que la instalada, se ofrece actualizar.</li>
 *   <li>Se descarga {@code orbita-launcher-<versión>.jar} (que ya lleva dentro el mod cliente) y se comprueba
 *       con su {@code .sha256}. Se guarda junto al jar actual en la carpeta {@code app} de Órbita.</li>
 *   <li>Se cambia {@code app\Orbita.cfg} para que apunte al jar nuevo y se reinicia Orbita.exe.</li>
 *   <li>Al siguiente arranque se borran los jars viejos.</li>
 * </ol>
 *
 * <p>Si una release dice {@code [completa]} en sus notas (por ejemplo, porque cambió el Java incluido),
 * no se actualiza solo el jar: se abre la página de descarga para bajar el .zip entero.
 */
public final class Updater {
    /** Marca en las notas de la release para pedir descargar el .zip completo. */
    public static final String FULL_DOWNLOAD_TAG = "[completa]";
    private static final Pattern JAR_NAME = Pattern.compile("orbita-launcher-(.+)\\.jar", Pattern.CASE_INSENSITIVE);
    private static final Pattern CFG_CLASSPATH = Pattern.compile("(?m)^app\\.classpath=.*$");
    private static final Pattern CFG_VERSION = Pattern.compile("-Djpackage\\.app-version=[^\\s\\r\\n]+");

    /** Lo que se sabe de una release de GitHub. {@code jarUrl} es null si la release no trae el jar. */
    public record Release(Version version, String tag, String notes, String pageUrl, String jarUrl, String jarName,
                          String sha256Url, String zipUrl) {
        public boolean needsFullDownload() {
            return jarUrl == null || sha256Url == null
                    || (notes != null && notes.toLowerCase(Locale.ROOT).contains(FULL_DOWNLOAD_TAG));
        }
    }

    private Updater() {
    }

    /** Repositorio configurado ("usuario/repo"), o vacío si no hay. */
    public static String repo() {
        String r = LauncherInfo.GITHUB_REPO;
        return r == null || r.isBlank() || r.contains("${") || r.startsWith("TU_USUARIO") ? "" : r.trim();
    }

    /**
     * Pregunta a GitHub por la última release. Devuelve null si no hay nada más nuevo que {@code current},
     * si no hay repositorio configurado o si se está ejecutando una versión de desarrollo.
     */
    public static Release check(String repo, String current) throws IOException {
        Version installed = Version.tryParse(current);
        if (repo == null || repo.isBlank() || installed == null) {
            return null;
        }
        Http.Response r = Http.get("https://api.github.com/repos/" + repo + "/releases/latest",
                "Accept", "application/vnd.github+json");
        if (r.status() == 404) {
            return null; // todavía no hay ninguna release publicada
        }
        if (!r.ok()) {
            throw new IOException("GitHub respondió " + r.status());
        }
        Release latest = parseRelease(r.json().getAsJsonObject());
        return latest != null && latest.version().isNewerThan(installed) ? latest : null;
    }

    /** Lee la respuesta de la API de GitHub. Separado para poder probarlo sin internet. */
    static Release parseRelease(JsonObject o) {
        if (o.has("draft") && o.get("draft").getAsBoolean()) {
            return null;
        }
        String tag = str(o, "tag_name");
        Version v = Version.tryParse(tag);
        if (v == null) {
            return null;
        }
        String jarUrl = null;
        String jarName = null;
        String shaUrl = null;
        String zipUrl = null;
        JsonArray assets = o.has("assets") ? o.getAsJsonArray("assets") : new JsonArray();
        for (JsonElement e : assets) {
            JsonObject a = e.getAsJsonObject();
            String name = str(a, "name");
            String url = str(a, "browser_download_url");
            if (name == null || url == null) {
                continue;
            }
            String lower = name.toLowerCase(Locale.ROOT);
            if (JAR_NAME.matcher(name).matches()) {
                jarUrl = url;
                jarName = name;
            } else if (lower.startsWith("orbita-launcher-") && lower.endsWith(".jar.sha256")) {
                shaUrl = url;
            } else if (lower.endsWith("-windows-x64.zip")) {
                zipUrl = url;
            }
        }
        return new Release(v, tag, str(o, "body"), str(o, "html_url"), jarUrl, jarName, shaUrl, zipUrl);
    }

    /** Carpeta {@code app} de Órbita (donde están el jar y Orbita.cfg), o null si no se abrió desde Orbita.exe. */
    public static Path appDir() {
        String exe = System.getProperty("jpackage.app-path");
        if (exe == null || exe.isBlank()) {
            return null;
        }
        Path dir = Path.of(exe).toAbsolutePath().getParent().resolve("app");
        return Files.isDirectory(dir) ? dir : null;
    }

    /** ¿Se puede actualizar sola esta instalación? (abierta desde Orbita.exe y con permiso de escritura) */
    public static boolean canSelfUpdate() {
        Path app = appDir();
        return app != null && Files.isWritable(app) && Files.exists(app.resolve("Orbita.cfg"));
    }

    /** Descarga e instala el jar nuevo. Al terminar hay que llamar a {@link #restart()}. */
    public static void install(Release release, Progress p) throws IOException {
        Path app = appDir();
        if (app == null || release.needsFullDownload()) {
            throw new IOException("Esta actualización hay que descargarla completa desde GitHub.");
        }
        p.status("Comprobando la actualización " + release.version() + "…");
        Http.Response sha = Http.get(release.sha256Url());
        if (!sha.ok()) {
            throw new IOException("No se pudo descargar el .sha256 (HTTP " + sha.status() + ")");
        }
        String expected = parseSha256(sha.body());
        if (expected == null) {
            throw new IOException("El archivo .sha256 no es válido");
        }

        p.status("Descargando Órbita " + release.version() + "…");
        p.progress(-1);
        Path jar = app.resolve(release.jarName());
        Http.download(release.jarUrl(), jar, "SHA-256", expected);

        p.status("Instalando…");
        Path cfg = app.resolve("Orbita.cfg");
        String text = Files.readString(cfg, StandardCharsets.UTF_8);
        Path tmp = cfg.resolveSibling("Orbita.cfg.tmp");
        Files.writeString(tmp, rewriteCfg(text, release.jarName(), release.version().toString()), StandardCharsets.UTF_8);
        Path backup = cfg.resolveSibling("Orbita.cfg.bak");
        Files.copy(cfg, backup, StandardCopyOption.REPLACE_EXISTING);
        Files.move(tmp, cfg, StandardCopyOption.REPLACE_EXISTING);
        p.status("Actualización lista. Reiniciando…");
    }

    /** Cambia el jar y la versión en el Orbita.cfg que genera jpackage. */
    static String rewriteCfg(String cfg, String jarName, String version) {
        String out = CFG_CLASSPATH.matcher(cfg).replaceFirst(Matcher.quoteReplacement("app.classpath=$APPDIR\\" + jarName));
        return CFG_VERSION.matcher(out).replaceAll(Matcher.quoteReplacement("-Djpackage.app-version=" + version));
    }

    /** Acepta tanto "abc123..." como el formato de sha256sum: "abc123...  archivo.jar". */
    static String parseSha256(String body) {
        if (body == null) {
            return null;
        }
        String first = body.trim().split("\\s+")[0];
        return first.matches("(?i)[0-9a-f]{64}") ? first.toLowerCase(Locale.ROOT) : null;
    }

    /** Vuelve a abrir Orbita.exe y cierra este proceso. */
    public static void restart() throws IOException {
        String exe = System.getProperty("jpackage.app-path");
        if (exe == null) {
            throw new IOException("No se sabe dónde está Orbita.exe");
        }
        new ProcessBuilder(exe).start();
        System.exit(0);
    }

    /** Borra los jars de versiones anteriores que quedaron en la carpeta app. */
    public static void cleanupOldJars() {
        Path app = appDir();
        if (app == null) {
            return;
        }
        String current = "orbita-launcher-" + LauncherInfo.VERSION + ".jar";
        try (DirectoryStream<Path> s = Files.newDirectoryStream(app, "orbita-launcher-*.jar")) {
            for (Path f : s) {
                if (!f.getFileName().toString().equalsIgnoreCase(current)) {
                    try {
                        Files.deleteIfExists(f);
                    } catch (IOException ignored) {
                        // sigue en uso o sin permiso: se intenta la próxima vez
                    }
                }
            }
        } catch (IOException ignored) {
            // no crítico
        }
    }

    private static String str(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : null;
    }
}
