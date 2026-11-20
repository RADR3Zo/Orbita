package dev.orbita.client.feature;

import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.ptr.IntByReference;
import dev.orbita.client.OrbitaClient;
import net.minecraft.client.Minecraft;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Lee la canción actual de la app de escritorio de Spotify (Windows) a partir del título de su ventana,
 * que es "Artista - Canción" mientras suena y "Spotify ..." en pausa, y le envía órdenes de reproducción
 * (WM_APPCOMMAND) solo a esa ventana. No necesita cuenta ni claves de API.
 *
 * <p>Pensado para gastar lo mínimo: una vez localizada la ventana solo se lee su título (una llamada al
 * sistema) cada 1,5 s; con el juego en segundo plano cada 5 s; y si el módulo está desactivado, el hilo
 * queda dormido sin hacer nada.</p>
 */
public final class SpotifyWatcher {
    public record NowPlaying(boolean running, boolean playing, String song, String artist) {
        static final NowPlaying NONE = new NowPlaying(false, false, "", "");
    }

    /** Órdenes de reproducción (códigos APPCOMMAND de Windows). */
    public enum Command {
        PLAY_PAUSE(14), NEXT(11), PREVIOUS(12);

        final int code;

        Command(int code) {
            this.code = code;
        }
    }

    private static final int WM_APPCOMMAND = 0x0319;
    private static final Object LOCK = new Object();

    private static volatile NowPlaying current = NowPlaying.NONE;
    private static volatile NowPlaying devFake;
    private static volatile long changedAt = System.currentTimeMillis();
    private static volatile long window;
    private static volatile boolean enabled = true;
    private static boolean wake;
    private static Thread thread;

    /** pid → ¿es Spotify.exe? (para no consultar el mismo proceso dos veces). */
    private static final Map<Integer, Boolean> SPOTIFY_PIDS = new HashMap<>();
    private static final char[] BUF = new char[512];

    private SpotifyWatcher() {
    }

    public static NowPlaying current() {
        NowPlaying fake = devFake;
        return fake != null ? fake : current;
    }

    /** Solo para las capturas de desarrollo: simula una canción sin tocar el Spotify real. */
    public static void devFake(NowPlaying fake) {
        devFake = fake;
        changedAt = System.currentTimeMillis();
        songStart = changedAt;
        pausedAt = 0;
        synced = fake != null;
    }

    /** Momento (ms) en que cambió la canción; sirve para animar el texto. */
    public static long changedAt() {
        return changedAt;
    }

    /** Activa o duerme el lector (lo llama el módulo de Spotify al activarse/desactivarse). */
    public static void setEnabled(boolean value) {
        enabled = value;
        if (!value) {
            current = NowPlaying.NONE;
        }
        updateMediaSession();
        wakeUp();
    }

    private static volatile boolean lyricsWanted;

    /** El ayudante de Windows solo corre si el widget de Spotify o el de letras están activados. */
    private static void updateMediaSession() {
        MediaSession.setWanted(enabled || lyricsWanted);
    }

    /** Llamado por MediaSession con cada cambio que informa Windows (canción, pausa, posición). */
    static void onMediaState(MediaSession.State s) {
        if (devFake != null) {
            return;
        }
        boolean ad = s.running() && isAd(s.title(), s.artist());
        NowPlaying next = !s.running() ? NowPlaying.NONE
                : ad || s.title().isEmpty() ? new NowPlaying(true, s.playing(), "", "")
                : new NowPlaying(true, s.playing(), s.title(), s.artist());
        NowPlaying prev = current;
        long now = System.currentTimeMillis();
        if (!next.song().equals(prev.song()) || !next.artist().equals(prev.artist())) {
            changedAt = now;
            // Spotify no siempre avisa a Windows de la nueva posición al cambiar de canción (sobre todo después
            // de un anuncio): hasta que llegue una marca de tiempo nueva se cuenta desde el cambio.
            trackChangedAt = now;
            staleAt = s.at();
            ownStart = now - 200;
            ownPausedAt = s.playing() ? 0 : now;
            if (!next.song().isEmpty()) {
                CoverArt.request(next.artist(), next.song());
                LyricsService.request(next.artist(), next.song());
            }
        } else if (prev.playing() && !next.playing()) {
            ownPausedAt = now;
        } else if (!prev.playing() && next.playing() && ownPausedAt != 0) {
            ownStart += now - ownPausedAt;
            ownPausedAt = 0;
        }
        current = next;
    }

    private static volatile long trackChangedAt;
    private static volatile long staleAt;
    private static volatile long ownStart;
    private static volatile long ownPausedAt;

    /** Anuncios de Spotify Free: no tienen letra y no deben tomarse como canción. */
    static boolean isAd(String title, String artist) {
        String t = title == null ? "" : title.trim().toLowerCase(java.util.Locale.ROOT);
        String a = artist == null ? "" : artist.trim().toLowerCase(java.util.Locale.ROOT);
        return t.equals("advertisement") || t.equals("anuncio") || t.equals("spotify") || a.isEmpty() || a.equals("spotify");
    }

    /** Posición desde el reloj propio (cuando la de Windows quedó desfasada). */
    private static long ownPosition() {
        long end = ownPausedAt != 0 ? ownPausedAt : System.currentTimeMillis();
        return Math.max(0, end - ownStart);
    }

    public static synchronized void start() {
        if (thread != null || !Platform.isWindows()) {
            return;
        }
        thread = new Thread(SpotifyWatcher::loop, "Orbita-Spotify");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    /** Envía una orden a la ventana de Spotify. Devuelve false si Spotify no está abierto. */
    public static boolean send(Command command) {
        long hwnd = window;
        if (hwnd == 0 || !Platform.isWindows()) {
            return false;
        }
        try {
            User32.INSTANCE.PostMessage(new WinDef.HWND(new Pointer(hwnd)), WM_APPCOMMAND, new WinDef.WPARAM(hwnd),
                    new WinDef.LPARAM((long) command.code << 16));
            wakeUp();
            return true;
        } catch (Throwable t) {
            OrbitaClient.LOG.warn("Spotify: no se pudo enviar la orden {}: {}", command, t.toString());
            return false;
        }
    }

    private static void wakeUp() {
        synchronized (LOCK) {
            wake = true;
            LOCK.notifyAll();
        }
    }

    private static void loop() {
        while (true) {
            try {
                long delay;
                if (!enabled) {
                    delay = 60_000;
                } else {
                    poll();
                    boolean focused = Minecraft.getInstance().isWindowActive();
                    delay = window == 0 ? 6000 : !focused ? 5000 : fastPoll ? 500 : 1500;
                    lastDelay = delay;
                }
                synchronized (LOCK) {
                    if (!wake) {
                        LOCK.wait(delay);
                    }
                    if (wake) {
                        wake = false;
                        LOCK.wait(250); // deja que Spotify actualice su título tras una orden
                    }
                }
            } catch (InterruptedException e) {
                return;
            } catch (Throwable t) {
                OrbitaClient.LOG.warn("Spotify: no se pudo leer el estado ({}). Se desactiva el lector.", t.toString());
                current = NowPlaying.NONE;
                window = 0;
                return;
            }
        }
    }

    private static void poll() {
        long hwnd = window;
        String title = null;
        if (hwnd != 0) {
            WinDef.HWND h = new WinDef.HWND(new Pointer(hwnd));
            if (User32.INSTANCE.IsWindow(h)) {
                title = text(h);
            }
        }
        if (title == null || !isSpotifyTitle(title)) {
            hwnd = find();
            window = hwnd;
            title = hwnd == 0 ? null : text(new WinDef.HWND(new Pointer(hwnd)));
        }
        update(title);
    }

    // ----------------------------------------------------------------- reloj de reproducción
    // La app de escritorio no publica la posición de la canción. Se estima desde el momento en que
    // cambia el título (inicio de canción), descontando las pausas. Si la canción ya sonaba cuando
    // empezamos a mirar, no se conoce su inicio: synced = false hasta la siguiente canción.

    private static volatile long songStart;
    private static volatile long pausedAt;
    private static volatile boolean synced;
    private static volatile boolean fastPoll;
    private static long lastDelay = 1500;

    /** Posición estimada de la canción actual, en ms. */
    public static long positionMs() {
        MediaSession.State ms = MediaSession.state();
        if (ms != null && ms.running() && devFake == null) {
            boolean stale = trackChangedAt != 0 && ms.at() <= staleAt;
            long p = ms.positionNow();
            if (stale || (ms.dur() > 0 && p > ms.dur() + 1000)) {
                return ownPosition();
            }
            return p;
        }
        long now = System.currentTimeMillis();
        NowPlaying np = current();
        long end = np.playing() ? now : (pausedAt != 0 ? pausedAt : now);
        return Math.max(0, end - songStart);
    }

    /** true si se vio empezar la canción y por tanto la posición es fiable. */
    public static boolean isSynced() {
        MediaSession.State ms = MediaSession.state();
        return (ms != null && ms.running() && devFake == null) || synced;
    }

    /** Lectura más frecuente (2 veces por segundo) mientras el widget de letras está activo. */
    public static void setFastPoll(boolean value) {
        fastPoll = value;
        lyricsWanted = value;
        updateMediaSession();
        wakeUp();
    }

    private static void update(String title) {
        MediaSession.State ms = MediaSession.state();
        if (ms != null && devFake == null) {
            return; // Windows ya da la canción y la posición exactas; la ventana solo sirve para enviar órdenes
        }
        NowPlaying prev = current;
        NowPlaying next;
        if (title == null) {
            next = NowPlaying.NONE;
        } else if (title.contains(" - ")) {
            int i = title.indexOf(" - ");
            next = new NowPlaying(true, true, title.substring(i + 3).trim(), title.substring(0, i).trim());
        } else {
            // En pausa se conserva la última canción conocida para mostrarla atenuada.
            NowPlaying last = current;
            next = new NowPlaying(true, false, last.song(), last.artist());
        }
        if (!next.equals(prev)) {
            long now = System.currentTimeMillis();
            boolean songChanged = !next.song().equals(prev.song()) || !next.artist().equals(prev.artist());
            if (songChanged) {
                changedAt = now;
                if (next.playing()) {
                    // El cambio ocurrió en algún momento desde la lectura anterior: de media, medio intervalo antes.
                    songStart = now - lastDelay / 2;
                    pausedAt = 0;
                    synced = !prev.song().isEmpty() && prev.running();
                    CoverArt.request(next.artist(), next.song());
                    LyricsService.request(next.artist(), next.song());
                }
            } else if (prev.playing() && !next.playing()) {
                pausedAt = now;
            } else if (!prev.playing() && next.playing() && pausedAt != 0) {
                songStart += now - pausedAt;
                pausedAt = 0;
            }
            current = next;
        }
    }

    /** Busca la ventana principal de Spotify recorriendo las ventanas (solo cuando no la conocemos). */
    private static long find() {
        long[] found = {0};
        IntByReference pid = new IntByReference();
        User32.INSTANCE.EnumWindows((hwnd, data) -> {
            String title = text(hwnd);
            if (title == null || !isSpotifyTitle(title)) {
                return true;
            }
            User32.INSTANCE.GetWindowThreadProcessId(hwnd, pid);
            int p = pid.getValue();
            boolean spotify = SPOTIFY_PIDS.computeIfAbsent(p, k -> ProcessHandle.of(k)
                    .flatMap(ph -> ph.info().command())
                    .map(c -> c.toLowerCase(Locale.ROOT).endsWith("spotify.exe"))
                    .orElse(false));
            if (spotify) {
                found[0] = Pointer.nativeValue(hwnd.getPointer());
                return false;
            }
            return true;
        }, null);
        if (SPOTIFY_PIDS.size() > 512) {
            SPOTIFY_PIDS.clear();
        }
        return found[0];
    }

    private static boolean isSpotifyTitle(String title) {
        return title.contains(" - ") || title.toLowerCase(Locale.ROOT).startsWith("spotify");
    }

    private static String text(WinDef.HWND hwnd) {
        int len = User32.INSTANCE.GetWindowText(hwnd, BUF, BUF.length);
        if (len <= 0) {
            return null;
        }
        String t = new String(BUF, 0, len).trim();
        if (t.isEmpty() || t.startsWith("GDI+") || t.equals("Default IME") || t.equals("MSCTFIME UI")) {
            return null;
        }
        return t;
    }
}
