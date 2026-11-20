package dev.orbita.launcher.core.install;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.orbita.launcher.LauncherInfo;
import dev.orbita.launcher.core.GamePaths;
import dev.orbita.launcher.core.Hashing;
import dev.orbita.launcher.core.Http;
import dev.orbita.launcher.core.Progress;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Descarga Minecraft (cliente, librerías, assets) desde los servidores de Mojang y el perfil de
 * Fabric Loader desde meta.fabricmc.net. Todo se verifica con los hashes oficiales.
 */
public final class GameInstaller {
    private static final String MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    private static final String RESOURCES = "https://resources.download.minecraft.net/";
    private static final String FABRIC_META = "https://meta.fabricmc.net/v2/versions/loader/";
    private static final String FABRIC_MAVEN = "https://maven.fabricmc.net/";

    private record Task(String url, Path target, String sha1, long size) {
    }

    private final boolean verifyHashes;

    public GameInstaller(boolean verifyHashes) {
        this.verifyHashes = verifyHashes;
    }

    public LaunchSpec install(Progress p) throws IOException {
        String mc = LauncherInfo.MINECRAFT;
        String loader = LauncherInfo.FABRIC_LOADER;
        p.progress(-1);
        p.status("Consultando Minecraft " + mc + "…");
        JsonObject vanilla = vanillaJson(mc);
        p.status("Consultando Fabric Loader " + loader + "…");
        JsonObject fabric = fabricProfile(mc, loader);

        List<Task> tasks = new ArrayList<>();
        Map<String, Path> classpath = new LinkedHashMap<>();

        // Fabric primero: si una librería aparece en ambos perfiles, gana la versión de Fabric.
        for (JsonElement e : fabric.getAsJsonArray("libraries")) {
            JsonObject lib = e.getAsJsonObject();
            String name = lib.get("name").getAsString();
            String repo = lib.has("url") ? lib.get("url").getAsString() : FABRIC_MAVEN;
            if (!repo.endsWith("/")) {
                repo += "/";
            }
            String rel = Maven.path(name);
            Path target = GamePaths.libraries().resolve(rel);
            tasks.add(new Task(repo + rel, target, str(lib, "sha1"), lib.has("size") ? lib.get("size").getAsLong() : -1));
            classpath.putIfAbsent(Maven.key(name), target);
        }

        for (JsonElement e : vanilla.getAsJsonArray("libraries")) {
            JsonObject lib = e.getAsJsonObject();
            if (!Rules.allowed(lib.getAsJsonArray("rules"))) {
                continue;
            }
            JsonObject downloads = lib.getAsJsonObject("downloads");
            if (downloads == null || !downloads.has("artifact")) {
                continue;
            }
            JsonObject art = downloads.getAsJsonObject("artifact");
            Path target = GamePaths.libraries().resolve(art.get("path").getAsString());
            tasks.add(new Task(art.get("url").getAsString(), target, str(art, "sha1"), art.get("size").getAsLong()));
            classpath.putIfAbsent(Maven.key(lib.get("name").getAsString()), target);
        }

        JsonObject client = vanilla.getAsJsonObject("downloads").getAsJsonObject("client");
        Path clientJar = GamePaths.versions().resolve(mc).resolve(mc + ".jar");
        tasks.add(new Task(client.get("url").getAsString(), clientJar, client.get("sha1").getAsString(), client.get("size").getAsLong()));
        classpath.put("net.minecraft:client", clientJar);

        // Assets (sonidos, idiomas...)
        JsonObject assetIndex = vanilla.getAsJsonObject("assetIndex");
        String assetId = assetIndex.get("id").getAsString();
        Path indexFile = GamePaths.assets().resolve("indexes").resolve(assetId + ".json");
        ensure(new Task(assetIndex.get("url").getAsString(), indexFile, assetIndex.get("sha1").getAsString(), assetIndex.get("size").getAsLong()));
        JsonObject objects = JsonParser.parseString(Files.readString(indexFile, StandardCharsets.UTF_8))
                .getAsJsonObject().getAsJsonObject("objects");
        for (Map.Entry<String, JsonElement> entry : objects.entrySet()) {
            JsonObject obj = entry.getValue().getAsJsonObject();
            String hash = obj.get("hash").getAsString();
            String sub = hash.substring(0, 2) + "/" + hash;
            tasks.add(new Task(RESOURCES + sub, GamePaths.assets().resolve("objects").resolve(sub), hash, obj.get("size").getAsLong()));
        }

        runDownloads(tasks, p);
        Files.createDirectories(GamePaths.natives());
        Files.createDirectories(GamePaths.mods());

        String versionId = fabric.has("id") ? fabric.get("id").getAsString() : "fabric-loader-" + loader + "-" + mc;
        return new LaunchSpec(versionId, fabric.get("mainClass").getAsString(), new ArrayList<>(classpath.values()),
                vanilla, fabric, assetId);
    }

    private JsonObject vanillaJson(String mc) throws IOException {
        Path file = GamePaths.versions().resolve(mc).resolve(mc + ".json");
        try {
            if (!Files.exists(file) || verifyHashes) {
                JsonObject manifest = Http.getJson(MANIFEST).getAsJsonObject();
                JsonObject entry = null;
                for (JsonElement e : manifest.getAsJsonArray("versions")) {
                    if (mc.equals(e.getAsJsonObject().get("id").getAsString())) {
                        entry = e.getAsJsonObject();
                        break;
                    }
                }
                if (entry == null) {
                    throw new IOException("Mojang no publica la versión " + mc);
                }
                ensure(new Task(entry.get("url").getAsString(), file, str(entry, "sha1"), -1));
            }
        } catch (IOException e) {
            if (!Files.exists(file)) {
                throw new IOException("No se pudo descargar la información de Minecraft. ¿Hay conexión a internet? (" + e.getMessage() + ")", e);
            }
        }
        return JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private JsonObject fabricProfile(String mc, String loader) throws IOException {
        String id = "fabric-loader-" + loader + "-" + mc;
        Path file = GamePaths.versions().resolve(id).resolve(id + ".json");
        if (!Files.exists(file)) {
            ensure(new Task(FABRIC_META + mc + "/" + loader + "/profile/json", file, null, -1));
        }
        return JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private void ensure(Task t) throws IOException {
        if (needsDownload(t)) {
            Http.download(t.url(), t.target(), t.sha1() == null ? null : "SHA-1", t.sha1());
        }
    }

    private boolean needsDownload(Task t) {
        try {
            if (!Files.exists(t.target())) {
                return true;
            }
            if (t.size() >= 0 && Files.size(t.target()) != t.size()) {
                return true;
            }
            return verifyHashes && t.sha1() != null && !t.sha1().equalsIgnoreCase(Hashing.sha1(t.target()));
        } catch (IOException e) {
            return true;
        }
    }

    private void runDownloads(List<Task> tasks, Progress p) throws IOException {
        p.status(verifyHashes ? "Verificando archivos…" : "Comprobando archivos…");
        List<Task> needed = tasks.parallelStream().filter(this::needsDownload).toList();
        if (needed.isEmpty()) {
            return;
        }
        long totalBytes = needed.stream().mapToLong(t -> Math.max(t.size(), 1)).sum();
        AtomicLong doneBytes = new AtomicLong();
        AtomicInteger doneCount = new AtomicInteger();
        int total = needed.size();
        p.status("Descargando " + total + " archivos…");
        p.progress(0);

        try (ExecutorService pool = Executors.newFixedThreadPool(16)) {
            List<Future<?>> futures = new ArrayList<>(total);
            for (Task t : needed) {
                futures.add(pool.submit(() -> {
                    Http.download(t.url(), t.target(), t.sha1() == null ? null : "SHA-1", t.sha1());
                    int c = doneCount.incrementAndGet();
                    p.progress(doneBytes.addAndGet(Math.max(t.size(), 1)) / (double) totalBytes);
                    p.status("Descargando archivos del juego (" + c + "/" + total + ")");
                    return null;
                }));
            }
            for (Future<?> f : futures) {
                try {
                    f.get();
                } catch (ExecutionException e) {
                    pool.shutdownNow();
                    throw e.getCause() instanceof IOException io ? io : new IOException(e.getCause());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    pool.shutdownNow();
                    throw new IOException("Instalación interrumpida", e);
                }
            }
        }
    }

    private static String str(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : null;
    }
}
