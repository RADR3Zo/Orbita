package dev.orbita.client.feature;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.jna.Platform;
import dev.orbita.client.OrbitaClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Posición exacta de Spotify desde los controles multimedia de Windows (SMTC), a través del pequeño
 * ayudante orbita-media.exe (incluido en el mod). Permite sincronizar las letras al instante aunque la
 * canción ya estuviera sonando o se adelante/retroceda. Solo corre mientras Spotify o las letras están activos.
 */
public final class MediaSession {
    /** Último estado recibido de Windows. at = instante (ms Unix) en que se midió pos. */
    public record State(boolean running, boolean playing, String title, String artist, long pos, long dur, long at,
                        long receivedAt) {
        /** Posición actual extrapolada. */
        public long positionNow() {
            long p = playing ? pos + (System.currentTimeMillis() - at) : pos;
            return dur > 0 ? Math.min(p, dur) : p;
        }
    }

    private static volatile State state;
    private static volatile Process process;
    private static volatile boolean wanted;
    private static Thread reader;

    private MediaSession() {
    }

    /** Estado reciente de Spotify según Windows, o null si el ayudante no está disponible. */
    public static State state() {
        State s = state;
        // Si el ayudante dejó de responder (latido cada 10 s), se vuelve al método del título de la ventana.
        return s != null && System.currentTimeMillis() - s.receivedAt() < 25_000 ? s : null;
    }

    public static synchronized void setWanted(boolean value) {
        wanted = value;
        if (value) {
            start();
        } else {
            stop();
        }
    }

    private static synchronized void start() {
        if (!Platform.isWindows() || (process != null && process.isAlive())) {
            return;
        }
        try {
            Path exe = extract();
            if (exe == null) {
                return;
            }
            process = new ProcessBuilder(exe.toString(), Long.toString(ProcessHandle.current().pid()))
                    .redirectErrorStream(true)
                    .start();
            Process p = process;
            reader = new Thread(() -> read(p), "Orbita-Media");
            reader.setDaemon(true);
            reader.setPriority(Thread.MIN_PRIORITY);
            reader.start();
            Runtime.getRuntime().addShutdownHook(new Thread(p::destroy, "Orbita-Media-stop"));
        } catch (Exception e) {
            OrbitaClient.LOG.warn("No se pudo iniciar el lector de medios de Windows: {}", e.toString());
        }
    }

    private static synchronized void stop() {
        Process p = process;
        process = null;
        state = null;
        if (p != null) {
            p.destroy();
        }
    }

    private static void read(Process p) {
        try (BufferedReader r = p.inputReader(StandardCharsets.UTF_8)) {
            String line;
            while ((line = r.readLine()) != null) {
                try {
                    JsonObject o = JsonParser.parseString(line).getAsJsonObject();
                    if (o.has("error")) {
                        continue;
                    }
                    boolean running = o.has("running") && o.get("running").getAsBoolean();
                    State s = running
                            ? new State(true, o.get("playing").getAsBoolean(), o.get("title").getAsString(),
                            o.get("artist").getAsString(), o.get("pos").getAsLong(), o.get("dur").getAsLong(),
                            o.get("at").getAsLong(), System.currentTimeMillis())
                            : new State(false, false, "", "", 0, 0, 0, System.currentTimeMillis());
                    if (state == null) {
                        OrbitaClient.LOG.info("Lector de medios de Windows activo: Spotify {} en el segundo {}",
                                s.running() ? (s.playing() ? "reproduciendo" : "en pausa") : "cerrado", s.positionNow() / 1000);
                    }
                    state = s;
                    SpotifyWatcher.onMediaState(s);
                } catch (Exception ignored) {
                    // línea incompleta o latido vacío
                }
            }
        } catch (Exception ignored) {
            // el proceso terminó
        }
        if (wanted && p == process) {
            state = null;
        }
    }

    /** Copia el ayudante desde el mod a la carpeta del juego (solo si cambió). */
    private static Path extract() throws Exception {
        try (InputStream in = MediaSession.class.getResourceAsStream("/orbita-native/orbita-media.exe")) {
            if (in == null) {
                return null;
            }
            byte[] bytes = in.readAllBytes();
            Path target = FabricLoader.getInstance().getGameDir().resolve("orbita-media.exe");
            if (!Files.exists(target) || !Arrays.equals(Files.readAllBytes(target), bytes)) {
                Files.write(target, bytes);
            }
            return target;
        }
    }
}
