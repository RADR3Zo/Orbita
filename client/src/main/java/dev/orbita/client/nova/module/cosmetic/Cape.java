package dev.orbita.client.nova.module.cosmetic;

import com.mojang.blaze3d.platform.NativeImage;
import dev.orbita.client.nova.NovaClient;
import dev.orbita.client.nova.core.Setting;
import dev.orbita.client.nova.module.Category;
import dev.orbita.client.nova.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/**
 * Capa de verdad: Minecraft la dibuja con su fisica normal (se mueve al caminar, tambien sirve para la elytra),
 * pero con un diseño animado y en alta resolucion (4x) generado por Nova.
 */
public final class Cape extends Module {
    public static final Identifier TEX = Identifier.fromNamespaceAndPath("nova", "cape_dynamic");
    private static final int W = 256, H = 128, SCALE = 4; // la textura de capa normal es 64x32

    public final Setting.Choice style = add(new Setting.Choice("Style", 0, "Galaxy", "Aurora", "Flames", "Nova", "Gradient"));
    public final Setting.Col color = add(new Setting.Col("Color", 0.75f, 0.55f));
    public final Setting.Bool animated = add(new Setting.Bool("Animated", true));

    private DynamicTexture texture;
    private int ticks;

    public Cape() { super("Cape", "Flowing cape of light", Category.COSMETICS); }
    @Override public String info() { return dev.orbita.client.nova.core.Lang.t(style.get()); }

    /** Lo usa la mixin de la skin: la capa de Nova para el jugador local, o null para dejar la suya. */
    public static Identifier textureFor(AbstractClientPlayer p) {
        if (NovaClient.modules == null || p != Minecraft.getInstance().player) return null;
        Cape c = NovaClient.modules.get(Cape.class);
        return c != null && c.enabled && c.texture != null ? TEX : null;
    }

    private volatile int[] ready;
    private final java.util.concurrent.atomic.AtomicBoolean busy = new java.util.concurrent.atomic.AtomicBoolean();
    private int lastStyle = -1, lastRgb = -1;

    @Override public void onTick() {
        if (texture == null) {
            texture = new DynamicTexture(() -> "nova", W, H, false);
            dev.orbita.client.nova.compat.V.filter(texture, true, false);
            Minecraft.getInstance().getTextureManager().register(TEX, texture);
            lastStyle = -1;
        }
        int[] px = ready; // sube a la GPU el ultimo diseño calculado
        if (px != null) {
            ready = null;
            NativeImage img = texture.getPixels();
            if (img != null) {
                for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) dev.orbita.client.nova.compat.V.setArgb(img, x, y, px[y * W + x]);
                texture.upload();
            }
        }
        int rgb = color.rgb(), st = style.index;
        boolean changed = st != lastStyle || rgb != lastRgb;
        if ((changed || (animated.value && !dev.orbita.client.nova.module.ClientSettings.eco() && ++ticks % 2 == 0)) && busy.compareAndSet(false, true)) {
            lastStyle = st; lastRgb = rgb;
            var w = Minecraft.getInstance().level;
            final float t = w != null ? w.getGameTime() % 100000 : 0;
            Thread th = new Thread(() -> {
                try { ready = paint(st, rgb, t); } finally { busy.set(false); }
            }, "nova-cape");
            th.setDaemon(true);
            th.start();
        }
    }

    @Override public void onDisable() {
        if (texture != null) { Minecraft.getInstance().getTextureManager().release(TEX); texture = null; }
        ready = null;
    }

    /** Calcula el diseño completo (en un hilo aparte). */
    private static int[] paint(int style, int rgb, float t) {
        int[] out = new int[W * H];
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                float cx = x / (float) SCALE, cy = y / (float) SCALE; // coordenadas de la textura de 64x32
                float u, v;
                if (cy >= 1 && cy < 17 && cx >= 1 && cx < 11) { u = (cx - 1) / 10f; v = (cy - 1) / 16f; }        // cara de fuera
                else if (cy >= 1 && cy < 17 && cx >= 12 && cx < 22) { u = 1 - (cx - 12) / 10f; v = (cy - 1) / 16f; } // cara de dentro
                else if (cx >= 22 && cx < 46 && cy < 22) { u = (cx - 22) / 24f; v = cy / 22f; }                    // elytra
                else { u = cx / 22f; v = Math.min(1, cy / 17f); }                                               // bordes
                out[y * W + x] = 0xFF000000 | pixel(style, rgb, u, v, t);
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ diseños (u, v en 0..1; v hacia abajo)
    private static int pixel(int style, int rgb, float u, float v, float t) {
        double r = ((rgb >> 16) & 255) / 255.0, g = ((rgb >> 8) & 255) / 255.0, b = (rgb & 255) / 255.0;
        double cr, cg, cb;
        switch (style) {
            case 0 -> { // galaxia: nebulosa que se mueve y estrellas que titilan
                double n = fbm(u * 3 + t * 0.004, v * 4.5 - t * 0.002, 4);
                double neb = Math.pow(Math.max(0, n - 0.35) * 1.6, 1.6);
                double base = 0.05 + 0.08 * (1 - v);
                cr = base * 0.4 + neb * r; cg = base * 0.3 + neb * g; cb = base + neb * b;
                double s = hash((int) (u * 60), (int) (v * 96));
                if (s > 0.965) { double tw = 0.5 + 0.5 * Math.sin(t * 0.15 + s * 400); double k = (s - 0.965) / 0.035 * tw; cr += k; cg += k; cb += k; }
            }
            case 1 -> { // aurora: cortinas que ondean
                double w = Math.sin(u * 9 + Math.sin(v * 3 + t * 0.05) * 2 + t * 0.03);
                double band = Math.pow(Math.max(0, w), 3) * (0.4 + 0.6 * fbm(u * 4, v * 2 + t * 0.01, 3)) * (1.1 - v * 0.6);
                cr = 0.02 + band * (r * 0.5 + 0.1); cg = 0.04 + band * (g + 0.4) * 0.8; cb = 0.08 + band * b;
            }
            case 2 -> { // llamas que suben
                double n = fbm(u * 4, v * 3 + t * 0.06, 4);
                double heat = Math.max(0, v * 1.3 - 0.15 + (n - 0.5) * 0.9);
                heat = Math.min(1, heat * 1.2);
                cr = Math.min(1, heat * 1.6) * (0.5 + 0.5 * r) + heat * heat * 0.4; cg = Math.pow(heat, 2.2) * (0.6 + 0.4 * g); cb = Math.pow(heat, 5) * (0.3 + 0.7 * b);
            }
            case 3 -> { // Nova: degradado con la estrella del logo latiendo
                double x = (u - 0.5) * 2, y = (v - 0.38) * 2 * 1.6;
                double d = starDist(x, y, 0.62);
                double line = Math.exp(-d * d * 900), halo = Math.exp(-d * d * 60) * (0.35 + 0.15 * Math.sin(t * 0.12));
                double base = 0.12 + 0.25 * (1 - v);
                cr = base * r + line + halo * r; cg = base * g + line + halo * g; cb = base * b + line + halo * b;
            }
            default -> { // degradado con un brillo que cruza en diagonal
                double k = 0.25 + 0.75 * (1 - v);
                double shine = Math.exp(-Math.pow(((u + v * 0.6 - (t * 0.01 % 2.6) + 0.6)) * 6, 2)) * 0.5;
                cr = r * k + shine; cg = g * k + shine; cb = b * k + shine;
            }
        }
        return (clamp(cr) << 16) | (clamp(cg) << 8) | clamp(cb);
    }

    private static int clamp(double c) { return (int) Math.round(255 * Math.max(0, Math.min(1, c))); }

    private static double hash(int x, int y) {
        int n = x * 374761393 + y * 668265263;
        n = (n ^ (n >>> 13)) * 1274126177;
        return ((n ^ (n >>> 16)) & 0xFFFFFF) / (double) 0x1000000;
    }

    private static double noise(double x, double y) {
        int xi = (int) Math.floor(x), yi = (int) Math.floor(y);
        double fx = x - xi, fy = y - yi;
        fx = fx * fx * (3 - 2 * fx); fy = fy * fy * (3 - 2 * fy);
        double a = hash(xi, yi), b = hash(xi + 1, yi), c = hash(xi, yi + 1), d = hash(xi + 1, yi + 1);
        return a + (b - a) * fx + (c - a) * fy + (a - b - c + d) * fx * fy;
    }

    private static double fbm(double x, double y, int oct) {
        double s = 0, amp = 0.5, norm = 0;
        for (int i = 0; i < oct; i++) { s += noise(x, y) * amp; norm += amp; x = x * 2.03 + 5.3; y = y * 2.03 + 1.7; amp *= 0.5; }
        return s / norm;
    }

    private static double starDist(double x, double y, double r) {
        double best = 9;
        for (int sq = 0; sq < 2; sq++)
            for (int k = 0; k < 4; k++) {
                double a0 = sq * Math.PI / 4 + k * Math.PI / 2, a1 = a0 + Math.PI / 2;
                double ax = Math.cos(a0) * r, ay = Math.sin(a0) * r, bx = Math.cos(a1) * r, by = Math.sin(a1) * r;
                double dx = bx - ax, dy = by - ay, tt = Math.max(0, Math.min(1, ((x - ax) * dx + (y - ay) * dy) / (dx * dx + dy * dy)));
                double ex = ax + dx * tt - x, ey = ay + dy * tt - y;
                best = Math.min(best, Math.sqrt(ex * ex + ey * ey));
            }
        return best;
    }
}
