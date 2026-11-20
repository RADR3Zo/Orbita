package dev.orbita.client.feature;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.orbita.client.OrbitaClient;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Letras sincronizadas de la canción que suena en Spotify, obtenidas de LRCLIB (lrclib.net), una base de
 * datos pública de letras en formato LRC. Una sola petición por canción, en un hilo de fondo, con caché.
 * Solo se usa mientras el widget de letras está activado.
 */
public final class LyricsService {
    /** Una línea de letra con su instante de inicio (ms). */
    public record Line(long timeMs, String text) {
    }

    /** Letra de una canción: líneas ordenadas por tiempo; synced = false si LRCLIB solo tiene texto plano. */
    public record Lyrics(List<Line> lines, boolean synced, boolean instrumental) {
        static final Lyrics NONE = new Lyrics(List.of(), false, false);

        /** Índice de la línea que suena en el instante dado, o -1 antes de la primera. */
        public int indexAt(long timeMs) {
            int lo = 0;
            int hi = lines.size() - 1;
            int found = -1;
            while (lo <= hi) {
                int mid = (lo + hi) >>> 1;
                if (lines.get(mid).timeMs() <= timeMs) {
                    found = mid;
                    lo = mid + 1;
                } else {
                    hi = mid - 1;
                }
            }
            return found;
        }
    }

    public enum State { NONE, LOADING, FOUND, NOT_FOUND }

    private static final Pattern TAG = Pattern.compile("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?]");
    private static final int MAX_CACHED = 32;
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(6))
            .build();
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Orbita-Lyrics");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
    });

    private static final Map<String, Lyrics> CACHE = Collections.synchronizedMap(
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Lyrics> eldest) {
                    return size() > MAX_CACHED;
                }
            });
    private static final Map<String, Boolean> PENDING = new java.util.concurrent.ConcurrentHashMap<>();
    private static volatile boolean enabled;

    private LyricsService() {
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    private static String key(String artist, String song) {
        return (artist + "|" + song).toLowerCase(Locale.ROOT);
    }

    public static State state(String artist, String song) {
        String k = key(artist, song);
        Lyrics l = CACHE.get(k);
        if (l != null) {
            return l == Lyrics.NONE ? State.NOT_FOUND : State.FOUND;
        }
        return PENDING.containsKey(k) ? State.LOADING : State.NONE;
    }

    @Nullable
    public static Lyrics get(String artist, String song) {
        Lyrics l = CACHE.get(key(artist, song));
        return l == null || l == Lyrics.NONE ? null : l;
    }

    /** Solo para pruebas: guarda una letra a mano para una canción. */
    public static void put(String artist, String song, Lyrics lyrics) {
        CACHE.put(key(artist, song), lyrics);
    }

    /** Pide la letra en segundo plano (si el widget está activado y no se pidió ya). */
    public static void request(String artist, String song) {
        if (!enabled || song.isBlank()) {
            return;
        }
        String k = key(artist, song);
        if (CACHE.containsKey(k) || PENDING.putIfAbsent(k, Boolean.TRUE) != null) {
            return;
        }
        WORKER.submit(() -> {
            Lyrics result = Lyrics.NONE;
            try {
                result = fetch(artist, song);
                if (result == null) {
                    String clean = cleanTitle(song);
                    result = clean.equals(song) ? null : fetch(artist, clean);
                }
            } catch (Exception e) {
                OrbitaClient.LOG.debug("Letra no disponible para {} - {}: {}", artist, song, e.toString());
            }
            CACHE.put(k, result == null ? Lyrics.NONE : result);
            PENDING.remove(k);
        });
    }

    /** Quita sufijos habituales de Spotify que no están en LRCLIB: "- Remastered 2011", "(feat. X)"... */
    static String cleanTitle(String song) {
        String s = song.replaceAll("(?i)\\s*[(\\[](feat\\.?|ft\\.?|with)[^)\\]]*[)\\]]", "");
        s = s.replaceAll("(?i)\\s+-\\s+.*(remaster|version|edit|live|mix|mono|stereo).*$", "");
        return s.trim();
    }

    @Nullable
    private static Lyrics fetch(String artist, String song) throws Exception {
        String url = "https://lrclib.net/api/search?track_name=" + enc(song) + "&artist_name=" + enc(artist);
        HttpResponse<String> res = HTTP.send(HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .header("User-Agent", "Orbita/" + OrbitaClient.version())
                .GET().build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (res.statusCode() != 200) {
            return null;
        }
        JsonArray results = JsonParser.parseString(res.body()).getAsJsonArray();
        JsonObject plainOnly = null;
        for (JsonElement e : results) {
            JsonObject o = e.getAsJsonObject();
            if (o.has("instrumental") && o.get("instrumental").getAsBoolean()) {
                return new Lyrics(List.of(), false, true);
            }
            if (o.has("syncedLyrics") && !o.get("syncedLyrics").isJsonNull()) {
                List<Line> lines = parseLrc(o.get("syncedLyrics").getAsString());
                if (!lines.isEmpty()) {
                    return new Lyrics(lines, true, false);
                }
            }
            if (plainOnly == null && o.has("plainLyrics") && !o.get("plainLyrics").isJsonNull()) {
                plainOnly = o;
            }
        }
        if (plainOnly != null) {
            List<Line> lines = new ArrayList<>();
            for (String l : plainOnly.get("plainLyrics").getAsString().split("\\R")) {
                if (!l.isBlank()) {
                    lines.add(new Line(-1, l.trim()));
                }
            }
            return lines.isEmpty() ? null : new Lyrics(lines, false, false);
        }
        return null;
    }

    /** Lee el formato LRC: "[mm:ss.xx] texto" (una línea puede tener varias marcas de tiempo). */
    public static List<Line> parseLrc(String lrc) {
        List<Line> lines = new ArrayList<>();
        for (String raw : lrc.split("\\R")) {
            Matcher m = TAG.matcher(raw);
            List<Long> times = new ArrayList<>();
            int end = 0;
            while (m.find() && m.start() == end) {
                long min = Long.parseLong(m.group(1));
                long sec = Long.parseLong(m.group(2));
                String frac = m.group(3);
                long ms = frac == null ? 0 : Long.parseLong((frac + "00").substring(0, 3));
                times.add(min * 60_000 + sec * 1000 + ms);
                end = m.end();
            }
            if (times.isEmpty()) {
                continue;
            }
            String text = raw.substring(end).trim();
            for (long t : times) {
                lines.add(new Line(t, text));
            }
        }
        lines.sort((a, b) -> Long.compare(a.timeMs(), b.timeMs()));
        return lines;
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
