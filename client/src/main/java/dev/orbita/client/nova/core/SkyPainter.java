package dev.orbita.client.nova.core;

/**
 * Genera el cielo procedural (textura equirectangular, 360 grados) sin depender de Minecraft.
 * Se usa ruido 3D sobre la direccion, asi que no hay costuras ni deformacion en los polos.
 */
public final class SkyPainter {
    public static final String[] MODES = {"Nebula", "Aurora", "Cells", "Stars", "Gradient"};
    /** Resoluciones por calidad (ancho x alto de la textura 360 grados). */
    public static final String[] QUALITY = {"Low", "Medium", "High", "Ultra (GPU)"};
    public static final int[] QW = {256, 512, 1024, 256}; // en Ultra la imagen solo sirve para el color de la niebla

    private SkyPainter() { }

    private static float hash(int x, int y, int z, int salt) {
        int n = x * 374761393 + y * 668265263 + z * 1013904223 + salt * 362437;
        n = (n ^ (n >>> 13)) * 1274126177;
        n ^= n >>> 16;
        return (n & 0xFFFFFF) / (float) 0x1000000;
    }

    private static double sm(double t) { return t * t * (3 - 2 * t); }

    private static double noise(double x, double y, double z) {
        int xi = (int) Math.floor(x), yi = (int) Math.floor(y), zi = (int) Math.floor(z);
        double fx = sm(x - xi), fy = sm(y - yi), fz = sm(z - zi);
        double c00 = lerp(hash(xi, yi, zi, 0), hash(xi + 1, yi, zi, 0), fx);
        double c10 = lerp(hash(xi, yi + 1, zi, 0), hash(xi + 1, yi + 1, zi, 0), fx);
        double c01 = lerp(hash(xi, yi, zi + 1, 0), hash(xi + 1, yi, zi + 1, 0), fx);
        double c11 = lerp(hash(xi, yi + 1, zi + 1, 0), hash(xi + 1, yi + 1, zi + 1, 0), fx);
        return lerp(lerp(c00, c10, fy), lerp(c01, c11, fy), fz);
    }

    private static double lerp(double a, double b, double t) { return a + (b - a) * t; }

    private static double fbm(double x, double y, double z, int oct) {
        double sum = 0, amp = 0.5, norm = 0;
        for (int i = 0; i < oct; i++) {
            sum += noise(x, y, z) * amp; norm += amp;
            x = x * 2.03 + 11.7; y = y * 2.03 + 3.1; z = z * 2.03 + 7.9; amp *= 0.5;
        }
        return sum / norm;
    }

    private static double clamp01(double v) { return v < 0 ? 0 : (v > 1 ? 1 : v); }

    /** Color medio de la franja justo encima del horizonte (para la niebla), un poco mas oscuro. */
    public static int horizonColor(int[] px, int W, int H) {
        long r = 0, g = 0, b = 0, n = 0;
        for (int y = Math.max(0, H / 2 - H / 14); y < H / 2; y++) {
            for (int x = 0; x < W; x++) {
                int c = px[y * W + x];
                r += (c >> 16) & 255; g += (c >> 8) & 255; b += c & 255; n++;
            }
        }
        if (n == 0) return 0;
        return (int) (r * 0.8 / n) << 16 | (int) (g * 0.8 / n) << 8 | (int) (b * 0.8 / n);
    }

    /** @param t tiempo en ticks; @param bright brillo 0.3..2 */
    public static void paint(int[] out, int W, int H, int mode, int rgb, double t, double bright) {
        double tr = ((rgb >> 16) & 255) / 255.0, tg = ((rgb >> 8) & 255) / 255.0, tb = (rgb & 255) / 255.0;
        java.util.stream.IntStream.range(0, H).parallel().forEach(y -> {
            double el = (0.5 - (y + 0.5) / H) * Math.PI;
            double dy = Math.sin(el), cy = Math.cos(el);
            for (int x = 0; x < W; x++) {
                double az = (x + 0.5) / W * Math.PI * 2;
                double dx = cy * Math.cos(az), dz = cy * Math.sin(az);
                double glow, core = 0, base = 0;
                switch (mode) {
                    case 0 -> { // Nebula: humo/energia en filamentos
                        double px = dx * 2.2, py = dy * 2.2, pz = dz * 2.2, s = t * 0.012;
                        double qx = fbm(px + s, py, pz, 3), qy = fbm(px + 5.2, py + 1.3 - s, pz + 2.8, 3), qz = fbm(px + 1.7, py + 9.2, pz + s, 3);
                        double n = fbm(px + 2.4 * qx, py + 2.4 * qy, pz + 2.4 * qz, 5);
                        double ridge = Math.pow(1 - Math.abs(2 * n - 1), 4.6);
                        glow = ridge * 1.9; core = Math.pow(ridge, 6) * 0.9; base = 0.06;
                    }
                    case 1 -> { // Aurora: cortinas verticales
                        double w = fbm(dx * 2.2 + t * 0.006, dz * 2.2, dy * 1.2 + t * 0.004, 3);
                        double ridge = Math.pow(1 - Math.abs(2 * w - 1), 4);
                        double streak = 0.55 + 0.45 * noise(dx * 26, dz * 26, t * 0.03);
                        double band = sm(clamp01(dy * 4 + 0.15)) * (1 - sm(clamp01((dy - 0.55) * 2.2)));
                        glow = ridge * streak * band * 2.2; core = glow * 0.15; base = 0.03 + 0.05 * (1 - Math.abs(dy));
                    }
                    case 2 -> { // Cells: celdas voronoi con borde brillante
                        double px = dx * 3.4 + Math.sin(t * 0.004) * 0.3, py = dy * 3.4, pz = dz * 3.4;
                        int ix = (int) Math.floor(px), iy = (int) Math.floor(py), iz = (int) Math.floor(pz);
                        double f1 = 9, f2 = 9;
                        for (int a = -1; a <= 1; a++) for (int b = -1; b <= 1; b++) for (int c = -1; c <= 1; c++) {
                            int cx = ix + a, cyy = iy + b, cz = iz + c;
                            double ph = hash(cx, cyy, cz, 3) * 6.283 + t * 0.01;
                            double fx = cx + 0.5 + 0.38 * Math.sin(ph + hash(cx, cyy, cz, 1) * 6);
                            double fy = cyy + 0.5 + 0.38 * Math.sin(ph * 1.3 + hash(cx, cyy, cz, 2) * 6);
                            double fz = cz + 0.5 + 0.38 * Math.cos(ph * 0.9 + hash(cx, cyy, cz, 4) * 6);
                            double d = Math.sqrt((px - fx) * (px - fx) + (py - fy) * (py - fy) + (pz - fz) * (pz - fz));
                            if (d < f1) { f2 = f1; f1 = d; } else if (d < f2) f2 = d;
                        }
                        double edge = f2 - f1;
                        glow = Math.exp(-edge * 17) * 1.5; core = Math.exp(-edge * 90) * 1.0; base = 0.02;
                    }
                    case 3 -> { // Stars
                        double px = dx * 40, py = dy * 40, pz = dz * 40;
                        int ix = (int) Math.floor(px), iy = (int) Math.floor(py), iz = (int) Math.floor(pz);
                        float h = hash(ix, iy, iz, 9);
                        double star = 0;
                        if (h > 0.985) {
                            double fx = px - ix - 0.5, fy = py - iy - 0.5, fz = pz - iz - 0.5;
                            double d = Math.sqrt(fx * fx + fy * fy + fz * fz);
                            star = Math.max(0, 1 - d * 2.2) * (0.6 + 0.4 * Math.sin(t * 0.1 + h * 200));
                        }
                        double neb = Math.pow(fbm(dx * 1.6, dy * 1.6 + t * 0.001, dz * 1.6, 4), 3) * 1.4;
                        glow = neb; core = star * 2.0; base = 0.015;
                    }
                    default -> { // Gradient
                        double hz = Math.pow(1 - Math.abs(dy), 3.5);
                        glow = hz * 0.9; base = 0.04 + 0.06 * Math.max(0, dy);
                        core = hz * hz * 0.25;
                    }
                }
                double fade = clamp01(1 + dy * 5); // abajo del horizonte se apaga
                double r = (tr * (glow + base) + core * (0.6 + 0.4 * tr)) * bright * fade;
                double g = (tg * (glow + base) + core * (0.6 + 0.4 * tg)) * bright * fade;
                double b = (tb * (glow + base) + core * (0.6 + 0.4 * tb)) * bright * fade;
                int ri = (int) (255 * (1 - Math.exp(-r * 1.4)));
                int gi = (int) (255 * (1 - Math.exp(-g * 1.4)));
                int bi = (int) (255 * (1 - Math.exp(-b * 1.4)));
                out[y * W + x] = 0xFF000000 | (ri << 16) | (gi << 8) | bi;
            }
        });
    }
}
