package dev.orbita.client.nova.gfx;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/**
 * Texturas que se generan al arrancar (no hay archivos .png): un resplandor radial suave para las manchas de luz
 * y el logo de Nova con bordes suaves. Se dibujan tenidas con el color que toque, en un solo rectangulo.
 */
public final class Tex {
    public static final Identifier GLOW = Identifier.fromNamespaceAndPath("nova", "glow");
    public static final Identifier LOGO = Identifier.fromNamespaceAndPath("nova", "logo");
    /** Circulo con borde suave: sus cuatro cuartos son las esquinas redondeadas de la interfaz. */
    public static final Identifier CIRCLE = Identifier.fromNamespaceAndPath("nova", "circle");
    /** Perfil de linea (opaca en el centro, suave en los bordes) para lineas sin escalones. */
    public static final Identifier LINE = Identifier.fromNamespaceAndPath("nova", "line");
    /** Formas de particulas (blancas, se tiñen): estrella, corazon, destello y anillo. */
    public static final Identifier STAR = Identifier.fromNamespaceAndPath("nova", "fx_star");
    public static final Identifier HEART = Identifier.fromNamespaceAndPath("nova", "fx_heart");
    public static final Identifier SPARK = Identifier.fromNamespaceAndPath("nova", "fx_spark");
    public static final Identifier RING = Identifier.fromNamespaceAndPath("nova", "fx_ring");
    /** Blanco liso: los cosmeticos se tiñen con el color de cada vertice. */
    public static final Identifier WHITE = Identifier.fromNamespaceAndPath("nova", "white");
    /** Tela de pañuelo: blanco con lunares y bordado gris (se tiñe del color elegido). */
    public static final Identifier BANDANA = Identifier.fromNamespaceAndPath("nova", "bandana");
    private static boolean ready;

    private Tex() { }

    private interface Alpha { double at(double x, double y); }

    /** Crea las texturas la primera vez que se dibuja algo (ya en el hilo de render). */
    public static void ensure() {
        if (ready) return;
        ready = true;
        register(GLOW, 128, 1, (x, y) -> { // campana suave que llega a 0 justo en el borde
            double r2 = Math.min(1, x * x + y * y), edge = Math.exp(-3);
            return (Math.exp(-3 * r2) - edge) / (1 - edge);
        });
        register(LOGO, 128, 4, Tex::star);
        register(WHITE, 16, 1, (x, y) -> 1);
        register(STAR, 64, 4, (x, y) -> { // estrella de 5 puntas con halo
            double a = Math.atan2(y, x) + Math.PI / 2, r = Math.sqrt(x * x + y * y);
            double k = Math.cos(Math.floorMod((int) 0, 1) + (a % (2 * Math.PI / 5) + 2 * Math.PI / 5) % (2 * Math.PI / 5) - Math.PI / 5);
            double edge = 0.42 + 0.48 * Math.pow(Math.abs(Math.cos(2.5 * a)), 3);
            return Math.max(r < edge * 0.95 ? 1 : 0, Math.max(0, 1 - r) * 0.35);
        });
        register(HEART, 64, 4, (x, y) -> {
            double hx = x * 1.15, hy = -y * 1.15 + 0.15; // y hacia arriba
            double v = Math.pow(hx * hx + hy * hy - 0.45, 3) - hx * hx * hy * hy * hy;
            return v <= 0 ? 1 : 0;
        });
        register(SPARK, 64, 4, (x, y) -> { // destello de 4 puntas
            double ax = Math.abs(x), ay = Math.abs(y);
            double cross = Math.max(Math.max(0, 1 - ax * 9) * Math.max(0, 1 - ay), Math.max(0, 1 - ay * 9) * Math.max(0, 1 - ax));
            double core = Math.max(0, 1 - Math.sqrt(x * x + y * y) * 2.2);
            return Math.min(1, cross + core);
        });
        register(RING, 128, 2, (x, y) -> {
            double r = Math.sqrt(x * x + y * y);
            return Math.max(0, 1 - Math.abs(r - 0.82) / 0.08);
        });
        register(CIRCLE, 128, 4, (x, y) -> x * x + y * y <= 1 ? 1 : 0);
        register(LINE, 16, 1, (x, y) -> Math.max(0, Math.min(1, (1 - Math.abs(y)) * 1.6)));
        registerRgb(BANDANA, 64, 16, (px, py) -> {
            double cx = (px % 16) - 7.5, cy = (py % 16) - 7.5;
            boolean dot = cx * cx + cy * cy < 5.5 || (Math.abs((px + 8) % 16 - 7.5) < 1.2 && Math.abs((py + 8) % 16 - 7.5) < 1.2);
            boolean edge = py < 1 || py > 14;
            return edge ? 0xFF707070 : dot ? 0xFFFFFFFF : 0xFFA8A8A8;
        });
    }

    /** Estrella de 8 puntas (dos cuadrados girados 45 grados), solo el contorno. Coordenadas -1..1. */
    private static double star(double x, double y) {
        double r = 0.86, stroke = 0.06;
        double best = 9;
        for (int sq = 0; sq < 2; sq++) {
            for (int k = 0; k < 4; k++) {
                double a0 = sq * Math.PI / 4 + k * Math.PI / 2, a1 = a0 + Math.PI / 2;
                best = Math.min(best, segDist(x, y, Math.cos(a0) * r, Math.sin(a0) * r, Math.cos(a1) * r, Math.sin(a1) * r));
            }
        }
        return best < stroke ? 1 : 0;
    }

    private static double segDist(double px, double py, double ax, double ay, double bx, double by) {
        double dx = bx - ax, dy = by - ay;
        double t = Math.max(0, Math.min(1, ((px - ax) * dx + (py - ay) * dy) / (dx * dx + dy * dy)));
        double cx = ax + dx * t - px, cy = ay + dy * t - py;
        return Math.sqrt(cx * cx + cy * cy);
    }

    private interface Pixel { int at(int x, int y); }

    private static void registerRgb(Identifier id, int w, int h, Pixel p) {
        NativeImage img = new NativeImage(w, h, false);
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) dev.orbita.client.nova.compat.V.setArgb(img, x, y, p.at(x, y));
        DynamicTexture tex = new DynamicTexture(() -> "nova", img);
        dev.orbita.client.nova.compat.V.filter(tex, false, false);
        Minecraft.getInstance().getTextureManager().register(id, tex);
    }

    /** Textura blanca con alpha = forma(x, y), con supermuestreo para que los bordes salgan suaves. */
    private static void register(Identifier id, int size, int samples, Alpha shape) {
        NativeImage img = new NativeImage(size, size, false);
        for (int py = 0; py < size; py++) {
            for (int px = 0; px < size; px++) {
                double sum = 0;
                for (int sy = 0; sy < samples; sy++) {
                    for (int sx = 0; sx < samples; sx++) {
                        double x = (px + (sx + 0.5) / samples) / size * 2 - 1;
                        double y = (py + (sy + 0.5) / samples) / size * 2 - 1;
                        sum += shape.at(x, y);
                    }
                }
                int a = (int) Math.round(255 * Math.max(0, Math.min(1, sum / (samples * samples))));
                dev.orbita.client.nova.compat.V.setArgb(img, px, py, (a << 24) | 0xFFFFFF);
            }
        }
        DynamicTexture tex = new DynamicTexture(() -> "nova", img);
        dev.orbita.client.nova.compat.V.filter(tex, true, false);
        Minecraft.getInstance().getTextureManager().register(id, tex);
    }
}
