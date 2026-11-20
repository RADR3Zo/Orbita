package dev.orbita.launcher.core.launch;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.orbita.launcher.LauncherInfo;
import dev.orbita.launcher.core.GamePaths;
import dev.orbita.launcher.core.LauncherConfig;
import dev.orbita.launcher.core.auth.Account;
import dev.orbita.launcher.core.install.LaunchSpec;
import dev.orbita.launcher.core.install.Rules;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Construye la línea de comandos del juego y arranca el proceso de Minecraft. */
public final class GameLauncher {
    private static final Pattern VAR = Pattern.compile("\\$\\{([a-zA-Z0-9_]+)}");

    /** Flags comunes: sin GC forzados por mods y sin escrituras periódicas a disco de hsperfdata. */
    private static final List<String> COMMON_FLAGS = List.of(
            "-XX:+DisableExplicitGC",
            "-XX:+PerfDisableSharedMem",
            "-Dstdout.encoding=UTF-8",
            "-Dstderr.encoding=UTF-8");

    /**
     * ZGC generacional (Java 21): recolecta casi todo en paralelo al juego, con pausas de menos de 1 ms.
     * Es la opción con menos tirones. Usa algo más de CPU y memoria que G1.
     */
    private static final List<String> ZGC_FLAGS = List.of(
            "-XX:+UseZGC",
            "-XX:+ZGenerational");

    /** G1 afinado (basado en los flags de Aikar), por si ZGC no va bien en algún equipo. */
    private static final List<String> G1_FLAGS = List.of(
            "-XX:+UseG1GC",
            "-XX:+ParallelRefProcEnabled",
            "-XX:MaxGCPauseMillis=130",
            "-XX:+UnlockExperimentalVMOptions",
            "-XX:G1NewSizePercent=28",
            "-XX:G1MaxNewSizePercent=40",
            "-XX:G1HeapRegionSize=16M",
            "-XX:G1ReservePercent=20",
            "-XX:G1MixedGCCountTarget=3",
            "-XX:G1HeapWastePercent=5",
            "-XX:G1RSetUpdatingPauseTimePercent=0",
            "-XX:SurvivorRatio=32",
            "-XX:MaxTenuringThreshold=1",
            "-XX:InitiatingHeapOccupancyPercent=15");

    private GameLauncher() {
    }

    public static Process launch(LaunchSpec spec, Account account, LauncherConfig cfg, Consumer<String> log) throws IOException {
        Path gameDir = GamePaths.root();
        Files.createDirectories(GamePaths.natives());

        Map<String, String> vars = new HashMap<>();
        vars.put("auth_player_name", account.name());
        vars.put("version_name", spec.versionId());
        vars.put("game_directory", gameDir.toString());
        vars.put("assets_root", GamePaths.assets().toString());
        vars.put("assets_index_name", spec.assetIndex());
        vars.put("auth_uuid", account.uuid());
        vars.put("auth_access_token", account.accessToken() == null ? "0" : account.accessToken());
        vars.put("clientid", "");
        vars.put("auth_xuid", account.xuid() == null ? "0" : account.xuid());
        vars.put("user_type", account.isMicrosoft() ? "msa" : "legacy");
        vars.put("version_type", "Órbita");
        vars.put("natives_directory", GamePaths.natives().toString());
        vars.put("launcher_name", "Orbita");
        vars.put("launcher_version", LauncherInfo.VERSION);
        vars.put("classpath", spec.classpath().stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator)));
        vars.put("classpath_separator", File.pathSeparator);
        vars.put("library_directory", GamePaths.libraries().toString());

        List<String> args = new ArrayList<>();
        args.add("-Xmx" + cfg.ramMb + "M");
        // La memoria empieza pequeña y crece solo si hace falta: en equipos con poca RAM libre, reservar
        // todo desde el principio obliga a Windows a usar el disco como memoria y provoca tirones.
        args.add("-Xms" + Math.min(cfg.ramMb, 1024) + "M");
        args.addAll(COMMON_FLAGS);
        long freeMb = freeRamMb();
        // ZGC gasta algo más de memoria: solo se usa si sobra RAM; si no, G1, que es más contenido.
        boolean zgc = cfg.useZgc && freeMb >= cfg.ramMb + 1536L;
        args.addAll(zgc ? ZGC_FLAGS : G1_FLAGS);
        log.accept("[Órbita] RAM libre: " + freeMb + " MB · recolector: " + (zgc ? "ZGC" : "G1")
                + (cfg.useZgc && !zgc ? " (poca RAM libre para ZGC)" : ""));
        if (cfg.jvmArgs != null && !cfg.jvmArgs.isBlank()) {
            for (String a : cfg.jvmArgs.trim().split("\\s+")) {
                args.add(a);
            }
        }
        args.addAll(resolve(arguments(spec.vanilla(), "jvm"), vars));
        args.addAll(resolve(arguments(spec.fabric(), "jvm"), vars));
        args.add(spec.mainClass());
        args.addAll(resolve(arguments(spec.vanilla(), "game"), vars));
        args.addAll(resolve(arguments(spec.fabric(), "game"), vars));

        // Se usa un @argfile para no chocar con el límite de longitud de la línea de comandos de Windows.
        Path argFile = gameDir.resolve("orbita-launch.args");
        Files.writeString(argFile, args.stream().map(GameLauncher::quote).collect(Collectors.joining("\n")), StandardCharsets.UTF_8);

        log.accept("[Órbita] Java: " + javaExecutable());
        log.accept("[Órbita] Versión: " + spec.versionId() + " · RAM: " + cfg.ramMb + " MB · Jugador: " + account.name());

        Process process = new ProcessBuilder(javaExecutable(), "@" + argFile)
                .directory(gameDir.toFile())
                .redirectErrorStream(true)
                .start();
        // El archivo lleva el token de la sesión: se borra en cuanto Java lo ha leído (lo lee al arrancar),
        // no al cerrar el juego, para que no se quede en disco mientras juegas.
        Runnable deleteArgFile = () -> {
            try {
                Files.deleteIfExists(argFile);
            } catch (IOException ignored) {
                // no crítico: se vuelve a intentar al cerrar el juego
            }
        };
        java.util.concurrent.CompletableFuture.delayedExecutor(10, java.util.concurrent.TimeUnit.SECONDS).execute(deleteArgFile);
        process.onExit().thenRun(deleteArgFile);

        Thread reader = new Thread(() -> {
            try (BufferedReader r = process.inputReader(StandardCharsets.UTF_8)) {
                String line;
                while ((line = r.readLine()) != null) {
                    log.accept(line);
                }
            } catch (IOException ignored) {
                // el proceso terminó
            }
        }, "orbita-game-log");
        reader.setDaemon(true);
        reader.start();
        return process;
    }

    /** RAM física libre del sistema en MB (o un valor alto si no se puede leer). */
    public static long freeRamMb() {
        try {
            var os = (com.sun.management.OperatingSystemMXBean) java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            return os.getFreeMemorySize() / (1024 * 1024);
        } catch (Throwable t) {
            return Long.MAX_VALUE / 4;
        }
    }

    /** El juego usa el mismo runtime Java 21 que ejecuta el launcher. */
    public static String javaExecutable() {
        Path home = Path.of(System.getProperty("java.home"));
        Path javaw = home.resolve("bin").resolve("javaw.exe");
        if (Files.exists(javaw)) {
            return javaw.toString();
        }
        Path exe = home.resolve("bin").resolve("java.exe");
        return Files.exists(exe) ? exe.toString() : home.resolve("bin").resolve("java").toString();
    }

    private static JsonArray arguments(JsonObject version, String kind) {
        JsonObject args = version.getAsJsonObject("arguments");
        if (args == null || !args.has(kind)) {
            return new JsonArray();
        }
        return args.getAsJsonArray(kind);
    }

    private static List<String> resolve(JsonArray array, Map<String, String> vars) {
        List<String> out = new ArrayList<>();
        for (JsonElement e : array) {
            if (e.isJsonPrimitive()) {
                out.add(substitute(e.getAsString(), vars));
                continue;
            }
            JsonObject obj = e.getAsJsonObject();
            if (!Rules.allowed(obj.getAsJsonArray("rules"))) {
                continue;
            }
            JsonElement value = obj.get("value");
            if (value.isJsonArray()) {
                for (JsonElement v : value.getAsJsonArray()) {
                    out.add(substitute(v.getAsString(), vars));
                }
            } else {
                out.add(substitute(value.getAsString(), vars));
            }
        }
        return out;
    }

    private static String substitute(String s, Map<String, String> vars) {
        Matcher m = VAR.matcher(s);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String v = vars.get(m.group(1));
            m.appendReplacement(sb, Matcher.quoteReplacement(v != null ? v : m.group(0)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /** Formato de @argfile de Java: entre comillas y con las barras invertidas escapadas. */
    private static String quote(String arg) {
        return "\"" + arg.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
