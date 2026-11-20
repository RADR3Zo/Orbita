package dev.orbita.launcher.core.mods;

import dev.orbita.launcher.LauncherInfo;
import dev.orbita.launcher.core.GamePaths;
import dev.orbita.launcher.core.Hashing;
import dev.orbita.launcher.core.Http;
import dev.orbita.launcher.core.Progress;
import dev.orbita.launcher.core.mods.ModrinthClient.ModVersion;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/** Gestor de mods estilo Feather: instala, actualiza, activa/desactiva y elimina mods de la carpeta mods/. */
public final class ModManager {
    public static final String CORE_FILE = "orbita-client.jar";
    public static final String FABRIC_API = "P7dR8mSH";
    private static final String DISABLED = ".disabled";

    /** Mods de rendimiento recomendados: slug de Modrinth → descripción. */
    public static final Map<String, String> PERFORMANCE_PACK = new LinkedHashMap<>();

    static {
        PERFORMANCE_PACK.put("sodium", "Motor de render moderno: muchos más FPS");
        PERFORMANCE_PACK.put("lithium", "Optimiza la física, la IA y los ticks del mundo");
        PERFORMANCE_PACK.put("ferrite-core", "Reduce el uso de memoria RAM");
        PERFORMANCE_PACK.put("immediatelyfast", "Acelera el render de HUD, texto y entidades");
        PERFORMANCE_PACK.put("entityculling", "No dibuja entidades que no se ven");
        PERFORMANCE_PACK.put("modernfix", "Arranque más rápido y menos tirones");
        PERFORMANCE_PACK.put("dynamic-fps", "Baja los FPS en segundo plano y libera CPU/GPU");
        PERFORMANCE_PACK.put("moreculling", "Oculta caras de bloques que no se ven");
    }

    /** Sube este número al añadir mods al pack: se instalarán los nuevos una vez. */
    public static final int PACK_VERSION = 2;

    public static final String IRIS = "iris";

    public static final class InstalledMod {
        public final Path file;
        public final boolean enabled;
        public final String sha512;
        public final boolean core;
        public String projectId;
        public String title;
        public String description;
        public String versionNumber;
        public String iconUrl;
        public ModVersion update;

        InstalledMod(Path file, boolean enabled, String sha512, boolean core) {
            this.file = file;
            this.enabled = enabled;
            this.sha512 = sha512;
            this.core = core;
        }

        public String fileName() {
            return file.getFileName().toString();
        }
    }

    private record CacheKey(String name, long size, long modified) {
    }

    private final ModrinthClient api = new ModrinthClient();
    private final Map<CacheKey, String> hashCache = new ConcurrentHashMap<>();

    public ModrinthClient api() {
        return api;
    }

    public Path dir() {
        return GamePaths.mods();
    }

    /** Lee la carpeta mods/; con online=true identifica cada archivo en Modrinth y busca actualizaciones. */
    public List<InstalledMod> scan(boolean online) throws IOException {
        Files.createDirectories(dir());
        List<InstalledMod> mods = new ArrayList<>();
        try (Stream<Path> files = Files.list(dir())) {
            for (Path f : (Iterable<Path>) files::iterator) {
                String name = f.getFileName().toString();
                boolean enabled = name.endsWith(".jar");
                if (!enabled && !name.endsWith(".jar" + DISABLED)) {
                    continue;
                }
                String base = enabled ? name : name.substring(0, name.length() - DISABLED.length());
                InstalledMod mod = new InstalledMod(f, enabled, hash(f), CORE_FILE.equals(base));
                mod.title = mod.core ? "Órbita (núcleo)" : base.replaceFirst("\\.jar$", "");
                if (mod.core) {
                    mod.versionNumber = LauncherInfo.VERSION;
                    mod.description = "Menú, HUD, zoom y cosméticos de Órbita";
                }
                mods.add(mod);
            }
        }

        if (online) {
            List<String> hashes = mods.stream().filter(m -> !m.core).map(m -> m.sha512).toList();
            Map<String, ModVersion> current = api.versionsByHash(hashes);
            Map<String, ModVersion> latest = api.latestByHash(hashes);
            Set<String> projectIds = new HashSet<>();
            for (InstalledMod m : mods) {
                ModVersion v = current.get(m.sha512);
                if (v != null) {
                    m.projectId = v.projectId();
                    m.versionNumber = v.versionNumber();
                    projectIds.add(v.projectId());
                    ModVersion newest = latest.get(m.sha512);
                    if (newest != null && !newest.id().equals(v.id())) {
                        m.update = newest;
                    }
                }
            }
            var projects = api.projects(projectIds);
            for (InstalledMod m : mods) {
                var p = m.projectId == null ? null : projects.get(m.projectId);
                if (p != null) {
                    m.title = p.title();
                    m.description = p.description();
                    m.iconUrl = p.iconUrl();
                }
            }
        }
        mods.sort(Comparator.<InstalledMod, Boolean>comparing(m -> !m.core)
                .thenComparing(m -> m.title.toLowerCase(Locale.ROOT)));
        return mods;
    }

    /** Instala un proyecto de Modrinth y sus dependencias obligatorias. Devuelve los archivos instalados. */
    public List<String> install(String projectIdOrSlug, Set<String> installedProjects, Progress p) throws IOException {
        List<String> installed = new ArrayList<>();
        installRecursive(projectIdOrSlug, installedProjects, new HashSet<>(), installed, p);
        return installed;
    }

    private void installRecursive(String id, Set<String> installedProjects, Set<String> visited,
                                  List<String> out, Progress p) throws IOException {
        if (!visited.add(id) || installedProjects.contains(id)) {
            return;
        }
        p.status("Buscando " + id + " para Minecraft " + LauncherInfo.MINECRAFT + "…");
        Optional<ModVersion> found = api.latestVersion(id);
        if (found.isEmpty()) {
            throw new IOException("No hay versión de \"" + id + "\" para Fabric " + LauncherInfo.MINECRAFT + ".");
        }
        ModVersion v = found.get();
        if (installedProjects.contains(v.projectId())) {
            return;
        }
        for (ModrinthClient.Dependency d : v.dependencies()) {
            if ("required".equals(d.type()) && d.projectId() != null) {
                installRecursive(d.projectId(), installedProjects, visited, out, p);
            }
        }
        p.status("Descargando " + v.fileName() + "…");
        Path target = dir().resolve(v.fileName());
        if (!Files.exists(target)) {
            Http.download(v.url(), target, "SHA-512", v.sha512());
        }
        installedProjects.add(v.projectId());
        out.add(v.fileName());
        p.log("[Mods] Instalado " + v.fileName());
    }

    public void update(InstalledMod mod, Progress p) throws IOException {
        if (mod.update == null) {
            return;
        }
        ModVersion v = mod.update;
        p.status("Actualizando " + mod.title + "…");
        Path target = dir().resolve(v.fileName() + (mod.enabled ? "" : DISABLED));
        Http.download(v.url(), target, "SHA-512", v.sha512());
        if (!target.equals(mod.file)) {
            Files.deleteIfExists(mod.file);
        }
        p.log("[Mods] " + mod.title + " actualizado a " + v.versionNumber());
    }

    public void setEnabled(InstalledMod mod, boolean enabled) throws IOException {
        if (mod.core || mod.enabled == enabled) {
            return;
        }
        String name = mod.fileName();
        String newName = enabled ? name.substring(0, name.length() - DISABLED.length()) : name + DISABLED;
        Files.move(mod.file, mod.file.resolveSibling(newName), StandardCopyOption.REPLACE_EXISTING);
    }

    public void delete(InstalledMod mod) throws IOException {
        if (!mod.core) {
            Files.deleteIfExists(mod.file);
        }
    }

    /** Copia el mod de Órbita incluido en el launcher y garantiza que Fabric API esté instalado y activo. */
    public void ensureCore(Progress p) throws IOException {
        Files.createDirectories(dir());
        Path core = dir().resolve(CORE_FILE);
        Files.deleteIfExists(dir().resolve(CORE_FILE + DISABLED));
        try (InputStream in = ModManager.class.getResourceAsStream("/bundled/" + CORE_FILE)) {
            if (in == null) {
                p.log("[Mods] Aviso: este launcher no incluye orbita-client.jar (build de desarrollo).");
            } else {
                byte[] bytes = in.readAllBytes();
                String bundledHash = Hashing.hash(new java.io.ByteArrayInputStream(bytes), "SHA-512");
                if (!Files.exists(core) || !bundledHash.equals(Hashing.sha512(core))) {
                    Files.write(core, bytes);
                    p.log("[Mods] Núcleo de Órbita instalado");
                }
            }
        }

        boolean hasFabricApi = false;
        try (Stream<Path> files = Files.list(dir())) {
            for (Path f : (Iterable<Path>) files::iterator) {
                String name = f.getFileName().toString();
                if (name.startsWith("fabric-api-") && name.endsWith(".jar")) {
                    hasFabricApi = true;
                } else if (name.startsWith("fabric-api-") && name.endsWith(".jar" + DISABLED)) {
                    Files.move(f, f.resolveSibling(name.substring(0, name.length() - DISABLED.length())),
                            StandardCopyOption.REPLACE_EXISTING);
                    hasFabricApi = true;
                    p.log("[Mods] Fabric API reactivado (Órbita lo necesita)");
                }
            }
        }
        if (!hasFabricApi) {
            install(FABRIC_API, new HashSet<>(), p);
        }
    }

    /**
     * Si la carpeta mods/ es de otra versión de Minecraft, la guarda en mods-backup-&lt;versión&gt;/ y
     * reinstala los mismos proyectos de Modrinth para la versión actual (los que no existan se avisan).
     */
    public void migrateIfNeeded(Progress p) throws IOException {
        Files.createDirectories(dir());
        Path marker = dir().resolve(".orbita-version");
        String current = LauncherInfo.MINECRAFT;
        String previous = Files.exists(marker) ? Files.readString(marker).trim() : null;
        if (current.equals(previous)) {
            return;
        }
        List<Path> jars;
        try (Stream<Path> files = Files.list(dir())) {
            jars = files.filter(f -> {
                String n = f.getFileName().toString();
                return n.endsWith(".jar") || n.endsWith(".jar" + DISABLED);
            }).toList();
        }
        if (!jars.isEmpty()) {
            String from = previous == null ? "anterior" : previous;
            p.status("Migrando mods de Minecraft " + from + " a " + current + "…");
            Set<String> projects = new HashSet<>();
            try {
                List<String> hashes = new ArrayList<>();
                for (Path j : jars) {
                    if (!j.getFileName().toString().startsWith(CORE_FILE)) {
                        hashes.add(hash(j));
                    }
                }
                api.versionsByHash(hashes).values().forEach(v -> projects.add(v.projectId()));
            } catch (IOException e) {
                p.log("[Mods] No se pudieron identificar los mods antiguos: " + e.getMessage());
            }
            Path backup = GamePaths.root().resolve("mods-backup-" + from.replace(' ', '_'));
            Files.createDirectories(backup);
            for (Path j : jars) {
                Files.move(j, backup.resolve(j.getFileName()), StandardCopyOption.REPLACE_EXISTING);
            }
            p.log("[Mods] Mods de " + from + " guardados en " + backup);
            Set<String> installed = new HashSet<>();
            for (String id : projects) {
                try {
                    install(id, installed, p);
                } catch (IOException e) {
                    p.log("[Mods] Sin versión para " + current + ": " + id + " (" + e.getMessage() + ")");
                }
            }
        }
        Files.writeString(marker, current);
    }

    /**
     * Aparta a mods-incompatibles/ los .jar activos que Modrinth identifica como hechos para otra versión
     * de Minecraft (p. ej. un Sodium de 1.21.8 copiado a mano), para que Fabric no falle al arrancar.
     * Los mods que Modrinth no conoce se dejan tal cual. Sin conexión no hace nada.
     */
    public void quarantineIncompatible(Progress p) {
        try {
            Files.createDirectories(dir());
            List<Path> jars;
            try (Stream<Path> files = Files.list(dir())) {
                jars = files.filter(f -> f.getFileName().toString().endsWith(".jar")
                        && !f.getFileName().toString().equals(CORE_FILE)).toList();
            }
            Map<String, Path> byHash = new LinkedHashMap<>();
            for (Path j : jars) {
                byHash.put(hash(j), j);
            }
            Map<String, ModVersion> known = api.versionsByHash(byHash.keySet());
            Path target = GamePaths.root().resolve("mods-incompatibles");
            for (Map.Entry<String, Path> e : byHash.entrySet()) {
                ModVersion v = known.get(e.getKey());
                if (v != null && !v.gameVersions().isEmpty() && !v.gameVersions().contains(LauncherInfo.MINECRAFT)) {
                    Files.createDirectories(target);
                    Files.move(e.getValue(), target.resolve(e.getValue().getFileName()), StandardCopyOption.REPLACE_EXISTING);
                    p.log("[Mods] " + e.getValue().getFileName() + " es para Minecraft " + String.join(", ", v.gameVersions())
                            + "; movido a mods-incompatibles");
                }
            }
        } catch (IOException e) {
            p.log("[Mods] No se pudo comprobar la compatibilidad de los mods: " + e.getMessage());
        }
    }

    /** Instala los mods de rendimiento que tengan versión compatible; los demás se omiten con un aviso. */
    public void installPerformancePack(Set<String> installedProjects, Progress p) {
        for (String slug : PERFORMANCE_PACK.keySet()) {
            try {
                install(slug, installedProjects, p);
            } catch (IOException e) {
                p.log("[Mods] Se omite " + slug + ": " + e.getMessage());
            }
        }
    }

    private String hash(Path file) throws IOException {
        CacheKey key = new CacheKey(file.getFileName().toString(), Files.size(file), Files.getLastModifiedTime(file).toMillis());
        String cached = hashCache.get(key);
        if (cached != null) {
            return cached;
        }
        String h = Hashing.sha512(file);
        hashCache.put(key, h);
        return h;
    }
}
