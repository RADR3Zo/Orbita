package dev.orbita.client.feature;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import dev.orbita.client.OrbitaClient;
import dev.orbita.client.config.OrbitaConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Portadas de las canciones de Spotify. Como la app de escritorio no expone la carátula, se busca por
 * "artista + canción" en la API pública de iTunes (y, si falla, en la de Deezer). Sin claves ni cuentas.
 * Se puede desactivar en las opciones del módulo de Spotify.
 */
public final class CoverArt {
    public static final int SIZE = 96;
    private static final int MAX_CACHED = 24;

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(6))
            .build();
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Orbita-Covers");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
    });

    /** clave "artista|canción" → textura (o null si no se encontró portada). Solo se toca en el hilo de render. */
    private static final Map<String, Identifier> CACHE = new LinkedHashMap<>(16, 0.75f, true);
    private static final Map<String, Boolean> PENDING = new java.util.concurrent.ConcurrentHashMap<>();
    private static int counter;

    private CoverArt() {
    }

    private static String key(String artist, String song) {
        return (artist + "|" + song).toLowerCase(Locale.ROOT);
    }

    /** Portada ya cargada para la canción, o null mientras no exista. Llamar desde el hilo de render. */
    @Nullable
    public static Identifier get(String artist, String song) {
        return CACHE.get(key(artist, song));
    }

    /** Pide la portada en segundo plano (si está activado y no se pidió ya). */
    public static void request(String artist, String song) {
        if (!OrbitaConfig.get().spotifyCovers || song.isBlank()) {
            return;
        }
        String key = key(artist, song);
        if (PENDING.putIfAbsent(key, Boolean.TRUE) != null) {
            return;
        }
        WORKER.submit(() -> {
            int[] pixels = null;
            try {
                String url = findArtwork(artist, song);
                if (url != null) {
                    pixels = download(url);
                }
            } catch (Exception e) {
                OrbitaClient.LOG.debug("Portada no disponible para {} - {}: {}", artist, song, e.toString());
            }
            int[] finalPixels = pixels;
            Minecraft.getInstance().execute(() -> upload(key, finalPixels));
        });
    }

    private static void upload(String key, @Nullable int[] pixels) {
        if (pixels == null) {
            // Se deja marcada como pedida para no repetir la búsqueda en cada fotograma.
            return;
        }
        NativeImage image = new NativeImage(SIZE, SIZE, false);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                image.setPixel(x, y, pixels[y * SIZE + x] | 0xFF000000);
            }
        }
        Identifier id = OrbitaClient.id("spotify/cover_" + (counter++));
        Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> "orbita_cover", image));
        CACHE.put(key, id);
        if (CACHE.size() > MAX_CACHED) {
            Iterator<Map.Entry<String, Identifier>> it = CACHE.entrySet().iterator();
            Map.Entry<String, Identifier> eldest = it.next();
            it.remove();
            PENDING.remove(eldest.getKey());
            Minecraft.getInstance().getTextureManager().release(eldest.getValue());
        }
    }

    @Nullable
    private static String findArtwork(String artist, String song) throws Exception {
        String term = URLEncoder.encode(artist + " " + song, StandardCharsets.UTF_8);
        try {
            JsonObject itunes = getJson("https://itunes.apple.com/search?media=music&entity=song&limit=1&term=" + term);
            JsonArray results = itunes.getAsJsonArray("results");
            if (results != null && !results.isEmpty()) {
                String art = results.get(0).getAsJsonObject().get("artworkUrl100").getAsString();
                return art.replace("100x100bb", "300x300bb");
            }
        } catch (Exception ignored) {
            // se intenta con Deezer
        }
        JsonObject deezer = getJson("https://api.deezer.com/search?limit=1&q=" + term);
        JsonArray data = deezer.getAsJsonArray("data");
        if (data != null && !data.isEmpty()) {
            return data.get(0).getAsJsonObject().getAsJsonObject("album").get("cover_medium").getAsString();
        }
        return null;
    }

    private static JsonObject getJson(String url) throws Exception {
        HttpResponse<String> res = HTTP.send(HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .header("User-Agent", "Orbita/" + OrbitaClient.version())
                .GET().build(), HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) {
            throw new IllegalStateException("HTTP " + res.statusCode());
        }
        return JsonParser.parseString(res.body()).getAsJsonObject();
    }

    private static int[] download(String url) throws Exception {
        HttpResponse<byte[]> res = HTTP.send(HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10)).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
        if (res.statusCode() != 200) {
            throw new IllegalStateException("HTTP " + res.statusCode());
        }
        BufferedImage src = ImageIO.read(new ByteArrayInputStream(res.body()));
        if (src == null) {
            throw new IllegalStateException("Formato de imagen no soportado");
        }
        BufferedImage scaled = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src, 0, 0, SIZE, SIZE, null);
        g.dispose();
        return scaled.getRGB(0, 0, SIZE, SIZE, null, 0, SIZE);
    }
}
