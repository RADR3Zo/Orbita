package dev.orbita.client.hud.lyrics;

import dev.orbita.client.config.OrbitaConfig;
import dev.orbita.client.feature.LyricsService;
import dev.orbita.client.feature.SpotifyWatcher;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Estado actual de la letra: qué línea suena, cuándo empezó y cuándo acaba, y qué palabra se está cantando.
 * Las LRC solo marcan el inicio de cada línea; las palabras se reparten dentro de la línea según su longitud.
 */
public final class LyricsClock {
    /** Línea actual. words: palabras de la línea; wordStarts: instante (ms) en que empieza cada una. */
    public record State(String key, int index, String text, String[] words, long[] wordStarts,
                        long start, long end, long position, boolean playing) {
        /** Índice de la palabra que suena ahora (-1 antes de la primera). */
        public int wordIndex() {
            int w = -1;
            for (int i = 0; i < wordStarts.length; i++) {
                if (position >= wordStarts[i]) {
                    w = i;
                }
            }
            return w;
        }

        /** Progreso 0..1 de la línea. */
        public float progress() {
            return end > start ? Math.max(0f, Math.min(1f, (position - start) / (float) (end - start))) : 1f;
        }
    }

    private static State cached;

    private LyricsClock() {
    }

    /** Estado de la línea actual, o null si no hay letra sincronizada utilizable ahora mismo. */
    @Nullable
    public static State current() {
        SpotifyWatcher.NowPlaying np = SpotifyWatcher.current();
        if (!np.running() || np.song().isEmpty() || !SpotifyWatcher.isSynced()) {
            return null;
        }
        LyricsService.Lyrics lyrics = LyricsService.get(np.artist(), np.song());
        if (lyrics == null || !lyrics.synced() || lyrics.lines().isEmpty()) {
            return null;
        }
        long pos = SpotifyWatcher.positionMs() + OrbitaConfig.get().lyricsOffsetMs;
        List<LyricsService.Line> lines = lyrics.lines();
        int index = lyrics.indexAt(pos);
        if (index < 0) {
            return null;
        }
        String key = np.artist() + "|" + np.song();
        State c = cached;
        if (c != null && c.index() == index && c.key().equals(key)) {
            c = new State(c.key(), c.index(), c.text(), c.words(), c.wordStarts(), c.start(), c.end(), pos, np.playing());
            cached = c;
            return c;
        }
        LyricsService.Line line = lines.get(index);
        long start = line.timeMs();
        long end = index + 1 < lines.size() ? lines.get(index + 1).timeMs() : start + 4000;
        // Si la siguiente línea tarda mucho (instrumental), la actual no se queda "cantándose" para siempre.
        end = Math.min(end, start + 8000);
        String text = line.text().isBlank() ? "♪" : line.text();
        String[] words = text.trim().split("\\s+");
        long[] starts = new long[words.length];
        int totalChars = 0;
        for (String w : words) {
            totalChars += w.length() + 1;
        }
        long span = (long) ((end - start) * 0.8f);
        int acc = 0;
        for (int i = 0; i < words.length; i++) {
            starts[i] = start + span * acc / Math.max(1, totalChars);
            acc += words[i].length() + 1;
        }
        c = new State(key, index, text, words, starts, start, end, pos, np.playing());
        cached = c;
        return c;
    }
}
