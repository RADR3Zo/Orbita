package dev.orbita.client.ui.render;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Primitivas 2D baratas (compatibles con Sodium/Iris). Cuando las texturas generadas ya están listas, los
 * círculos y las esquinas redondeadas se dibujan con una sola pieza suavizada en vez de una por fila de
 * píxeles: muchas menos piezas por fotograma y bordes más finos.
 */
public final class UiDraw {
    private UiDraw() {
    }

    private static boolean fast() {
        return SpaceArt.ready();
    }

    public static void roundedRect(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0) {
            return;
        }
        if (r <= 0 || w < 2 * r || h < 2 * r) {
            g.fill(x, y, x + w, y + h, color);
            return;
        }
        if (fast() && r >= 2) {
            g.fill(x + r, y, x + w - r, y + h, color);
            g.fill(x, y + r, x + r, y + h - r, color);
            g.fill(x + w - r, y + r, x + w, y + h - r, color);
            corner(g, x, y, r, 0, 0, color);
            corner(g, x + w - r, y, r, 1, 0, color);
            corner(g, x, y + h - r, r, 0, 1, color);
            corner(g, x + w - r, y + h - r, r, 1, 1, color);
            return;
        }
        g.fill(x, y + r, x + w, y + h - r, color);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = (int) Math.round(r - Math.sqrt(r * r - dy * dy));
            g.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
            g.fill(x + inset, y + h - 1 - i, x + w - inset, y + h - i, color);
        }
    }

    /** Rectángulo con solo las esquinas de abajo redondeadas (barras al pie de una tarjeta). */
    public static void roundedBottom(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
        if (!fast() || r <= 0 || h < r || w < 2 * r) {
            g.fill(x, y, x + w, y + h, color);
            return;
        }
        g.fill(x, y, x + w, y + h - r, color);
        g.fill(x + r, y + h - r, x + w - r, y + h, color);
        corner(g, x, y + h - r, r, 0, 1, color);
        corner(g, x + w - r, y + h - r, r, 1, 1, color);
    }

    /** Un cuarto del disco: qx/qy = 0 izquierda/arriba, 1 derecha/abajo. */
    private static void corner(GuiGraphics g, int x, int y, int r, int qx, int qy, int color) {
        int half = SpaceArt.DISC / 2;
        g.blit(RenderPipelines.GUI_TEXTURED, SpaceArt.disc(), x, y, qx * half, qy * half, r, r, half, half,
                SpaceArt.DISC, SpaceArt.DISC, color);
    }

    public static void circle(GuiGraphics g, int cx, int cy, int radius, int color) {
        if (radius <= 0) {
            return;
        }
        if (fast()) {
            g.blit(RenderPipelines.GUI_TEXTURED, SpaceArt.disc(), cx - radius, cy - radius, 0f, 0f, radius * 2, radius * 2,
                    SpaceArt.DISC, SpaceArt.DISC, SpaceArt.DISC, SpaceArt.DISC, color);
            return;
        }
        for (int dy = -radius; dy < radius; dy++) {
            double yy = dy + 0.5;
            int half = (int) Math.round(Math.sqrt(radius * radius - yy * yy));
            if (half > 0) {
                g.fill(cx - half, cy + dy, cx + half, cy + dy + 1, color);
            }
        }
    }

    /**
     * Arco orbital de una sola pieza (estela de ~200° detrás de la cabeza) centrado en (cx,cy), con la cabeza
     * en el ángulo headAngle (radianes). Devuelve false si la textura aún no está lista.
     */
    public static boolean orbitArc(GuiGraphics g, float cx, float cy, float radius, float headAngle, int color) {
        if (!fast()) {
            return false;
        }
        Identifier id = SpaceArt.orbitArc();
        float size = radius / 0.86f;
        g.pose().pushMatrix();
        g.pose().translate(cx, cy);
        g.pose().rotate(headAngle);
        int s = Math.round(size);
        g.blit(RenderPipelines.GUI_TEXTURED, id, -s, -s, 0f, 0f, s * 2, s * 2, SpaceArt.ARC, SpaceArt.ARC,
                SpaceArt.ARC, SpaceArt.ARC, color);
        g.pose().popMatrix();
        return true;
    }
}
