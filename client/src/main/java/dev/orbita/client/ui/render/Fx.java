package dev.orbita.client.ui.render;

import dev.orbita.client.ui.theme.OrbitaTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Efectos gráficos de la interfaz de Órbita: partículas, arcos, brillos y texto con degradado animado. */
public final class Fx {
    private Fx() {
    }

    // ------------------------------------------------------------------ partículas

    private static final class P {
        float x, y, vx, vy, life, max, size;
        int color;
        boolean glow;
    }

    private static final List<P> PARTICLES = new ArrayList<>();
    private static final Random RANDOM = new Random();
    private static long lastUpdate;
    private static float lastMouseX = -1, lastMouseY = -1;
    private static final int MAX = 260;

    /** Estela de chispas detrás del cursor (llamar cada fotograma con la posición del ratón). */
    public static void cursorTrail(int mouseX, int mouseY) {
        if (lastMouseX >= 0) {
            float dx = mouseX - lastMouseX;
            float dy = mouseY - lastMouseY;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            int n = Math.min(4, (int) (dist / 5f));
            for (int i = 0; i < n; i++) {
                float k = i / (float) Math.max(1, n);
                P p = add(lastMouseX + dx * k, lastMouseY + dy * k,
                        (RANDOM.nextFloat() - 0.5f) * 12f, (RANDOM.nextFloat() - 0.5f) * 12f + 6f,
                        0.45f + RANDOM.nextFloat() * 0.35f, 1.5f + RANDOM.nextFloat() * 1.5f,
                        OrbitaTheme.lerpColor(OrbitaTheme.ACCENT, 0xFFFFFFFF, RANDOM.nextFloat() * 0.6f));
                if (p != null) {
                    p.glow = RANDOM.nextFloat() < 0.35f;
                }
            }
        }
        lastMouseX = mouseX;
        lastMouseY = mouseY;
    }

    /** Explosión de chispas (clics, activar un módulo...). */
    public static void burst(float x, float y, int color, int count, float speed) {
        for (int i = 0; i < count; i++) {
            float a = RANDOM.nextFloat() * Mth.TWO_PI;
            float v = speed * (0.35f + RANDOM.nextFloat() * 0.65f);
            P p = add(x, y, Mth.cos(a) * v, Mth.sin(a) * v, 0.5f + RANDOM.nextFloat() * 0.5f,
                    1.5f + RANDOM.nextFloat() * 2f, OrbitaTheme.lerpColor(color, 0xFFFFFFFF, RANDOM.nextFloat() * 0.5f));
            if (p != null) {
                p.glow = true;
            }
        }
    }

    private static P add(float x, float y, float vx, float vy, float life, float size, int color) {
        if (PARTICLES.size() >= MAX) {
            return null;
        }
        P p = new P();
        p.x = x;
        p.y = y;
        p.vx = vx;
        p.vy = vy;
        p.life = p.max = life;
        p.size = size;
        p.color = color;
        PARTICLES.add(p);
        return p;
    }

    /** Mueve y dibuja todas las partículas. */
    public static void renderParticles(GuiGraphics g) {
        long now = System.nanoTime();
        float dt = lastUpdate == 0 ? 0f : Math.min(0.05f, (now - lastUpdate) / 1_000_000_000f);
        lastUpdate = now;
        for (int i = PARTICLES.size() - 1; i >= 0; i--) {
            P p = PARTICLES.get(i);
            p.life -= dt;
            if (p.life <= 0) {
                PARTICLES.remove(i);
                continue;
            }
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            p.vx *= 1f - 2.2f * dt;
            p.vy = p.vy * (1f - 2.2f * dt) + 10f * dt;
            float k = p.life / p.max;
            float s = p.size * (0.4f + 0.6f * k);
            if (p.glow && SpaceArt.ready()) {
                SpaceScene.glow(g, p.x, p.y, s * 3f, s * 3f, OrbitaTheme.withAlpha(p.color, k * 0.5f));
            }
            int is = Math.max(1, Math.round(s));
            g.fill(Math.round(p.x - is / 2f), Math.round(p.y - is / 2f), Math.round(p.x - is / 2f) + is,
                    Math.round(p.y - is / 2f) + is, OrbitaTheme.withAlpha(p.color, k));
        }
    }

    // ------------------------------------------------------------------ formas

    /** Arco de circunferencia (ángulos en radianes) hecho con puntos: anillos de carga, halos giratorios. */
    public static void arc(GuiGraphics g, float cx, float cy, float r, float start, float sweep, float thick, int color,
                           boolean fadeTail) {
        int steps = Math.max(6, (int) (Math.abs(sweep) * r / 1.2f));
        int t = Math.max(1, Math.round(thick));
        for (int i = 0; i <= steps; i++) {
            float k = i / (float) steps;
            float a = start + sweep * k;
            int x = Math.round(cx + Mth.cos(a) * r - t / 2f);
            int y = Math.round(cy + Mth.sin(a) * r - t / 2f);
            int c = fadeTail ? OrbitaTheme.withAlpha(color, k) : color;
            g.fill(x, y, x + t, y + t, c);
        }
    }

    /** Banda de brillo que cruza un rectángulo (efecto cristal). progress 0..1. */
    public static void sweep(GuiGraphics g, int x, int y, int w, int h, float progress, int color) {
        if (progress <= 0f || progress >= 1f) {
            return;
        }
        int band = Math.max(10, w / 4);
        int center = x - band + Math.round((w + band * 2) * progress);
        g.enableScissor(x, y, x + w, y + h);
        for (int i = -band; i <= band; i += 2) {
            float k = 1f - Math.abs(i) / (float) band;
            int col = OrbitaTheme.withAlpha(color, k * k);
            // Inclinada: cada franja horizontal se desplaza un poco.
            for (int row = 0; row < h; row += 4) {
                int sx = center + i + (h - row) / 3;
                g.fill(sx, y + row, sx + 2, y + Math.min(h, row + 4), col);
            }
        }
        g.disableScissor();
    }

    /** Borde con un destello que recorre el perímetro del rectángulo. */
    public static void runningBorder(GuiGraphics g, int x, int y, int w, int h, float time, int color) {
        int perim = 2 * (w + h);
        int len = Math.max(40, perim / 7);
        for (int seg = 0; seg < 2; seg++) {
            int head = (int) ((time * 90f + seg * perim / 2f) % perim);
            for (int i = 0; i < len; i += 2) {
                int d = (head - i + perim) % perim;
                float k = 1f - i / (float) len;
                int c = OrbitaTheme.withAlpha(color, k * k);
                int px, py;
                if (d < w) {
                    px = x + d;
                    py = y;
                } else if (d < w + h) {
                    px = x + w - 1;
                    py = y + d - w;
                } else if (d < 2 * w + h) {
                    px = x + w - 1 - (d - w - h);
                    py = y + h - 1;
                } else {
                    px = x;
                    py = y + h - 1 - (d - 2 * w - h);
                }
                g.fill(px, py, px + 2, py + 2, c);
            }
        }
    }

    /** Texto con degradado que ondula entre dos colores (por letra). Devuelve el ancho dibujado. */
    public static int shimmerText(GuiGraphics g, Font font, String text, FontDescription fontDesc, int x, int y,
                                  int from, int to, float time, float alpha, boolean shadow) {
        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            Component c = Component.literal(ch).withStyle(s -> s.withFont(fontDesc));
            // Posición según el ancho del texto anterior completo (igual que al dibujarlo de una vez).
            String prefix = text.substring(0, i);
            int cx = x + font.width(Component.literal(prefix).withStyle(s -> s.withFont(fontDesc)));
            float k = 0.5f + 0.5f * Mth.sin(time * 2.2f - i * 0.55f);
            int col = OrbitaTheme.withAlpha(OrbitaTheme.lerpColor(from, to, k), alpha);
            g.drawString(font, c, cx, y, col, shadow);
        }
        return font.width(Component.literal(text).withStyle(s -> s.withFont(fontDesc)));
    }

    /** Rectángulo con degradado vertical recortado a esquinas redondeadas (aprox. con un margen). */
    public static void gradientCard(GuiGraphics g, int x, int y, int w, int h, int r, int top, int bottom) {
        if (h < 2 * r || w < 2 * r) {
            g.fillGradient(x, y, x + w, y + h, top, bottom);
            return;
        }
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = (int) Math.round(r - Math.sqrt(r * r - dy * dy));
            g.fill(x + inset, y + i, x + w - inset, y + i + 1, top);
            g.fill(x + inset, y + h - 1 - i, x + w - inset, y + h - i, bottom);
        }
        g.fillGradient(x, y + r, x + w, y + h - r, top, bottom);
    }
}
