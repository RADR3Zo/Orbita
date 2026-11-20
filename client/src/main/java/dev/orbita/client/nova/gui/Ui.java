package dev.orbita.client.nova.gui;

import dev.orbita.client.nova.gfx.Tex;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

/**
 * Herramientas de dibujo para una interfaz redondeada y suave: rectangulos con bordes redondos y antialias,
 * circulos, resplandor y texto con fuente TrueType propia (nova:ui / nova:ui_bold).
 */
public final class Ui {
    private static final Style REGULAR = Style.EMPTY.withFont(new net.minecraft.network.chat.FontDescription.Resource(Identifier.fromNamespaceAndPath("nova", "ui")));
    private static final Style BOLD = Style.EMPTY.withFont(new net.minecraft.network.chat.FontDescription.Resource(Identifier.fromNamespaceAndPath("nova", "ui_bold")));

    private Ui() { }

    // ------------------------------------------------------------------ color
    public static int alpha(int argb, float mul) {
        int a = (int) (((argb >>> 24) & 255) * Math.max(0f, Math.min(1f, mul)));
        return (a << 24) | (argb & 0xFFFFFF);
    }

    public static int rgba(int rgb, int a) { return (Math.max(0, Math.min(255, a)) << 24) | (rgb & 0xFFFFFF); }

    public static int mix(int c1, int c2, float t) {
        int a = (int) (((c1 >>> 24) & 255) * (1 - t) + ((c2 >>> 24) & 255) * t);
        int r = (int) (((c1 >> 16) & 255) * (1 - t) + ((c2 >> 16) & 255) * t);
        int g = (int) (((c1 >> 8) & 255) * (1 - t) + ((c2 >> 8) & 255) * t);
        int b = (int) ((c1 & 255) * (1 - t) + (c2 & 255) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static float ease(float t) { t = Math.max(0, Math.min(1, t)); return 1 - (1 - t) * (1 - t) * (1 - t); }

    // ------------------------------------------------------------------ texto
    /** El mismo texto con la letra suave de Nova (los hijos la heredan). */
    public static Component styled(Component t, boolean bold) { return t.copy().withStyle(bold ? BOLD : REGULAR); }

    public static Component t(String s, boolean bold) { return Component.literal(s).setStyle(bold ? BOLD : REGULAR); }

    /** Texto con una fuente propia de Nova por nombre (p. ej. "greatvibes", "dancing") para la letra de la música. */
    public static Component font(String s, String font) { return Component.literal(s).setStyle(Style.EMPTY.withFont(new net.minecraft.network.chat.FontDescription.Resource(Identifier.fromNamespaceAndPath("nova", font)))); }

    public static int width(Font tr, String s, boolean bold) { return tr.width(t(s, bold)); }

    public static void text(GuiGraphics c, Font tr, String s, int x, int y, int color, boolean bold) {
        c.drawString(tr, t(s, bold), x, y, color, false);
    }

    public static void text(GuiGraphics c, Font tr, String s, int x, int y, int color, boolean bold, boolean shadow) {
        c.drawString(tr, t(s, bold), x, y, color, shadow);
    }

    public static void ctext(GuiGraphics c, Font tr, String s, int cx, int y, int color, boolean bold) {
        text(c, tr, s, cx - width(tr, s, bold) / 2, y, color, bold);
    }

    public static void rtext(GuiGraphics c, Font tr, String s, int right, int y, int color, boolean bold) {
        text(c, tr, s, right - width(tr, s, bold), y, color, bold);
    }

    // ------------------------------------------------------------------ formas
    // Las esquinas y las lineas usan texturas suaves con coordenadas con decimales: se ven lisas a cualquier escala.

    private static void corner(GuiGraphics c, int x, int y, int r, float u, float v, int color) {
        dev.orbita.client.nova.compat.V.tex(c, Tex.CIRCLE, x, y, u, v, r, r, 64, 64, 128, 128, color);
    }

    public static void roundRect(GuiGraphics c, int x, int y, int w, int h, int r, int color) {
        r = Math.min(r, Math.min(w, h) / 2);
        if (r <= 0) { c.fill(x, y, x + w, y + h, color); return; }
        Tex.ensure();
        corner(c, x, y, r, 0, 0, color);
        corner(c, x + w - r, y, r, 64, 0, color);
        corner(c, x, y + h - r, r, 0, 64, color);
        corner(c, x + w - r, y + h - r, r, 64, 64, color);
        if (w > 2 * r) {
            c.fill(x + r, y, x + w - r, y + r, color);
            c.fill(x + r, y + h - r, x + w - r, y + h, color);
        }
        if (h > 2 * r) c.fill(x, y + r, x + w, y + h - r, color);
    }

    /** Rectangulo redondeado con degradado vertical. */
    public static void roundRectV(GuiGraphics c, int x, int y, int w, int h, int r, int top, int bottom) {
        r = Math.min(r, Math.min(w, h) / 2);
        if (r <= 0) { c.fillGradient(x, y, x + w, y + h, top, bottom); return; }
        Tex.ensure();
        int ct = mix(top, bottom, r / 2f / h), cb = mix(top, bottom, 1 - r / 2f / h);
        corner(c, x, y, r, 0, 0, ct);
        corner(c, x + w - r, y, r, 64, 0, ct);
        corner(c, x, y + h - r, r, 0, 64, cb);
        corner(c, x + w - r, y + h - r, r, 64, 64, cb);
        if (w > 2 * r) {
            c.fill(x + r, y, x + w - r, y + r, ct);
            c.fill(x + r, y + h - r, x + w - r, y + h, cb);
        }
        if (h > 2 * r) c.fillGradient(x, y + r, x + w, y + h - r, ct, cb);
    }

    /** Borde de 1 px redondeado (sin rellenar el interior), con esquinas suaves. */
    public static void outline(GuiGraphics c, int x, int y, int w, int h, int r, int color) {
        r = Math.min(r, Math.min(w, h) / 2);
        if (w > 2 * r) {
            c.fill(x + r, y, x + w - r, y + 1, color);
            c.fill(x + r, y + h - 1, x + w - r, y + h, color);
        }
        if (h > 2 * r) {
            c.fill(x, y + r, x + 1, y + h - r, color);
            c.fill(x + w - 1, y + r, x + w, y + h - r, color);
        }
        if (r <= 0) return;
        float rr = r - 0.5f;
        arc(c, x + r, y + r, rr, Math.PI, 1.5 * Math.PI, color);
        arc(c, x + w - r, y + r, rr, 1.5 * Math.PI, 2 * Math.PI, color);
        arc(c, x + w - r, y + h - r, rr, 0, 0.5 * Math.PI, color);
        arc(c, x + r, y + h - r, rr, 0.5 * Math.PI, Math.PI, color);
    }

    private static void arc(GuiGraphics c, double cx, double cy, double r, double a0, double a1, int color) {
        int segs = Math.max(3, (int) Math.ceil(r * 0.8));
        double px = cx + Math.cos(a0) * r, py = cy + Math.sin(a0) * r;
        for (int i = 1; i <= segs; i++) {
            double a = a0 + (a1 - a0) * i / segs, nx = cx + Math.cos(a) * r, ny = cy + Math.sin(a) * r;
            lineW(c, px, py, nx, ny, 1f, color);
            px = nx; py = ny;
        }
    }

    public static void circle(GuiGraphics c, int cx, int cy, int r, int color) {
        roundRect(c, cx - r, cy - r, r * 2, r * 2, r, color);
    }

    /** Resplandor suave detras de un rectangulo redondeado. */
    public static void glow(GuiGraphics c, int x, int y, int w, int h, int r, int rgb, int strength, int reach) {
        if (dev.orbita.client.nova.module.ClientSettings.eco() && dev.orbita.client.nova.NovaClient.modules != null && c != null
                && net.minecraft.client.Minecraft.getInstance().level != null) return; // ahorro: sin resplandores en el juego
        for (int i = reach; i >= 1; i--) {
            float f = 1f - i / (float) (reach + 1);
            roundRect(c, x - i, y - i, w + 2 * i, h + 2 * i, r + i, rgba(rgb, (int) (strength * f * f / 3f)));
        }
    }

    /** Mancha de luz circular: un degradado radial suave (textura generada), sin escalones. */
    public static void blob(GuiGraphics c, int cx, int cy, int radius, int rgb, int maxAlpha) {
        if (radius <= 0) return;
        Tex.ensure();
        int a = Math.min(255, Math.round(maxAlpha * 1.25f)), d = radius * 2;
        dev.orbita.client.nova.compat.V.tex(c, Tex.GLOW, cx - radius, cy - radius, 0, 0, d, d, d, d, rgba(rgb, a));
    }

    public static void line(GuiGraphics c, double x0, double y0, double x1, double y1, int color) {
        lineW(c, x0, y0, x1, y1, 1f, color);
    }

    /** Linea con grosor w y bordes suaves (antialias), con coordenadas con decimales. */
    public static void lineW(GuiGraphics c, double x0, double y0, double x1, double y1, float w, int color) {
        double dx = x1 - x0, dy = y1 - y0, len = Math.sqrt(dx * dx + dy * dy);
        if (len < 1e-4) return;
        Tex.ensure();
        float half = w / 2f + 0.6f;
        float nx = (float) (-dy / len * half), ny = (float) (dx / len * half);
        // se alarga un poco para que las lineas seguidas se unan sin huecos
        float ex = (float) (dx / len * 0.2), ey = (float) (dy / len * 0.2);
        float ax = (float) x0 - ex, ay = (float) y0 - ey, bx = (float) x1 + ex, by = (float) y1 + ey;
        dev.orbita.client.nova.compat.V.lineQuad(c, Tex.LINE, ax, ay, bx, by, nx, ny, color);
    }

    /** Estrella de 8 puntas (logo de Nova) con bordes suaves, girada rot radianes. */
    public static void logo(GuiGraphics c, double cx, double cy, double r, double rot, int color) {
        Tex.ensure();
        int s = (int) Math.round(r * 2 / 0.86); // la estrella ocupa el 86 % de la textura
        dev.orbita.client.nova.compat.V.push(c);
        dev.orbita.client.nova.compat.V.translate(c, cx, cy);
        dev.orbita.client.nova.compat.V.rotate(c, (float) rot);
        dev.orbita.client.nova.compat.V.tex(c, Tex.LOGO, -s / 2, -s / 2, 0, 0, s, s, s, s, color);
        dev.orbita.client.nova.compat.V.pop(c);
    }
}
