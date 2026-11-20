package dev.orbita.client.nova.video;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

/** Escala "cover" (llena y recorta el centro) a w x h y devuelve pixeles ARGB. */
public final class FrameScaler {
    private FrameScaler() { }

    public static int[] cover(BufferedImage src, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        double s = Math.max(w / (double) src.getWidth(), h / (double) src.getHeight());
        int dw = (int) Math.ceil(src.getWidth() * s), dh = (int) Math.ceil(src.getHeight() * s);
        g.drawImage(src, (w - dw) / 2, (h - dh) / 2, dw, dh, null);
        g.dispose();
        int[] px = ((DataBufferInt) out.getRaster().getDataBuffer()).getData();
        int[] copy = new int[px.length];
        for (int i = 0; i < px.length; i++) copy[i] = px[i] | 0xFF000000; // sin transparencia
        return copy;
    }
}
