package dev.orbita.client.nova.video;

import com.mojang.blaze3d.platform.NativeImage;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/**
 * Reproduce un video (GIF o MP4, sin sonido, en bucle) en un hilo aparte y lo deja en una textura de Minecraft.
 * Se decodifica solo mientras algo lo esta dibujando (si no se usa durante 1.5 s, se pausa para no gastar CPU).
 */
public final class VideoPlayer {
    /** Video de los menus y del cielo. */
    public static final VideoPlayer MENU = new VideoPlayer("video_dynamic");
    /** Video del inventario (puede ser otro distinto). */
    public static final VideoPlayer INVENTORY = new VideoPlayer("video_inventory");

    public final Identifier TEX;

    private record Frame(int[] px, int w, int h, long seq) { }

    private volatile Frame latest;
    private volatile int gen;
    private volatile long lastUse;
    public volatile String error = "";
    private DynamicTexture texture;
    private int texW, texH;
    private long uploaded = -1;
    private Path current;
    private int currentW;

    private VideoPlayer(String id) { TEX = Identifier.fromNamespaceAndPath("nova", id); }

    public boolean isOpen(Path p, int width) { return p != null && p.equals(current) && width == currentW; }

    /** Abre (o reabre si cambio) un video. Ancho de textura en px; el alto es 16:9. */
    public void open(Path p, int width) {
        if (isOpen(p, width)) return;
        close();
        current = p; currentW = width; error = ""; lastUse = System.currentTimeMillis();
        final int g = ++gen, w = width, h = width * 9 / 16;
        Thread t = new Thread(() -> run(p, w, h, g), "nova-video");
        t.setDaemon(true);
        t.setPriority(Thread.NORM_PRIORITY - 1);
        t.start();
    }

    public void close() {
        gen++;
        current = null; latest = null; uploaded = -1;
        if (texture != null) {
            Minecraft.getInstance().getTextureManager().release(TEX);
            texture = null;
        }
    }

    private static FrameSource create(Path p) throws Exception {
        String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
        if (n.endsWith(".gif")) return new GifSource(p.toFile());
        if (n.endsWith(".mp4") || n.endsWith(".mov") || n.endsWith(".m4v")) return new Mp4Source(p.toFile());
        throw new IllegalArgumentException("Formato no soportado (usa .mp4 o .gif)");
    }

    private void run(Path p, int w, int h, int myGen) {
        try (FrameSource src = create(p)) {
            long seq = 0, next = System.nanoTime();
            int emptyRuns = 0;
            while (gen == myGen) {
                if (System.currentTimeMillis() - lastUse > 1500) { Thread.sleep(150); next = System.nanoTime(); continue; }
                BufferedImage img = src.next();
                if (img == null) { // fin: vuelve a empezar (bucle)
                    if (++emptyRuns > 3) throw new IllegalStateException("El video no tiene fotogramas legibles");
                    src.restart();
                    continue;
                }
                emptyRuns = 0;
                latest = new Frame(FrameScaler.cover(img, w, h), w, h, ++seq);
                next += src.delayMs() * 1_000_000L;
                long wait = next - System.nanoTime();
                if (wait > 0) Thread.sleep(wait / 1_000_000L, (int) (wait % 1_000_000L));
                else if (wait < -200_000_000L) next = System.nanoTime(); // vamos tarde: no acumular retraso
            }
        } catch (Throwable e) {
            if (gen == myGen) error = e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    /** Llamar en el hilo principal cada vez que se va a dibujar. Sube el fotograma nuevo a la GPU. @return true si hay imagen. */
    public boolean pump() {
        lastUse = System.currentTimeMillis();
        Frame f = latest;
        if (f == null) return texture != null;
        if (f.seq() != uploaded) {
            if (texture == null || texW != f.w() || texH != f.h()) {
                if (texture != null) Minecraft.getInstance().getTextureManager().release(TEX);
                texture = new DynamicTexture(() -> "nova", f.w(), f.h(), false);
                texW = f.w(); texH = f.h();
                Minecraft.getInstance().getTextureManager().register(TEX, texture);
                dev.orbita.client.nova.compat.V.filter(texture, true, false);
            }
            NativeImage img = texture.getPixels();
            if (img == null) return false;
            int[] px = f.px();
            for (int y = 0; y < texH; y++)
                for (int x = 0; x < texW; x++)
                    dev.orbita.client.nova.compat.V.setArgb(img, x, y, px[y * texW + x]);
            texture.upload();
            uploaded = f.seq();
        }
        return true;
    }

    public int width() { return texW; }
    public int height() { return texH; }
}
