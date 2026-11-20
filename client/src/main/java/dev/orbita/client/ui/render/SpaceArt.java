package dev.orbita.client.ui.render;

import com.mojang.blaze3d.platform.NativeImage;
import dev.orbita.client.OrbitaClient;
import dev.orbita.client.ui.theme.OrbitaTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.util.Random;

/**
 * Arte espacial generado en tiempo de ejecución (sin imágenes externas): planeta sombreado,
 * anillo, luna con cráteres, cohete y un degradado radial reutilizable para brillos y neblina.
 * Se crea una sola vez, la primera vez que se dibuja el menú.
 */
public final class SpaceArt {
    public static final int PLANET = 512;
    public static final int RING = 512;
    public static final int MOON = 256;
    public static final int GLOW = 256;
    public static final int ROCKET_W = 32;
    public static final int ROCKET_H = 64;

    private static Identifier planet;
    private static Identifier ring;
    private static Identifier moon;
    private static Identifier glow;
    private static Identifier rocket;
    private static Identifier disc;
    private static Identifier orbitArc;
    public static final int DISC = 128;
    public static final int ARC = 128;

    private SpaceArt() {
    }

    /** Círculo blanco suavizado: círculos y esquinas redondeadas en una sola pieza. */
    public static Identifier disc() {
        ensure();
        return disc;
    }

    /** Arco orbital de ~200° con estela que se desvanece y la cabeza brillante en el ángulo 0. */
    public static Identifier orbitArc() {
        ensure();
        return orbitArc;
    }

    public static Identifier planet() {
        ensure();
        return planet;
    }

    public static Identifier ring() {
        ensure();
        return ring;
    }

    public static Identifier moon() {
        ensure();
        return moon;
    }

    public static Identifier glow() {
        ensure();
        return glow;
    }

    public static Identifier rocket() {
        ensure();
        return rocket;
    }

    private static volatile int state;
    private static long readyAt;

    /**
     * Las texturas se calculan en un hilo aparte (unos cientos de ms de matemáticas) para no congelar el
     * menú; solo la subida a la GPU ocurre en el hilo de render.
     */
    private static void ensure() {
        if (state != 0) {
            return;
        }
        state = 1;
        Thread t = new Thread(() -> {
            NativeImage[] images = {makePlanet(), makeRing(), makeMoon(), makeGlow(), makeRocket(), makeDisc(), makeArc()};
            Minecraft.getInstance().execute(() -> {
                planet = register("planet", images[0]);
                ring = register("ring", images[1]);
                moon = register("moon", images[2]);
                glow = register("glow", images[3]);
                rocket = register("rocket", images[4]);
                disc = register("disc", images[5]);
                orbitArc = register("orbit_arc", images[6]);
                readyAt = System.currentTimeMillis();
                state = 2;
            });
        }, "Orbita-SpaceArt");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    /** true cuando las texturas ya están en la GPU. Llamar desde el hilo de render. */
    public static boolean ready() {
        ensure();
        return state == 2;
    }

    /** Opacidad 0..1 del fundido de entrada tras cargarse las texturas. */
    public static float fade() {
        return state != 2 ? 0f : Math.min(1f, (System.currentTimeMillis() - readyAt) / 700f);
    }

    private static Identifier register(String name, NativeImage image) {
        Identifier id = OrbitaClient.id("generated/" + name);
        Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> "orbita_" + name, image));
        return id;
    }

    // ------------------------------------------------------------------ planeta

    private static NativeImage makePlanet() {
        int s = PLANET;
        NativeImage img = new NativeImage(s, s, true);
        float lx = -0.55f, ly = -0.55f, lz = 0.63f;
        int deep = 0xFF1A1240;
        int violet = 0xFF5B3FC4;
        int lilac = 0xFFA996FF;
        int cyan = 0xFF5CC8FF;
        for (int y = 0; y < s; y++) {
            for (int x = 0; x < s; x++) {
                float nx = (x + 0.5f) / s * 2f - 1f;
                float ny = (y + 0.5f) / s * 2f - 1f;
                float r2 = nx * nx + ny * ny;
                if (r2 > 1f) {
                    img.setPixel(x, y, 0);
                    continue;
                }
                float nz = (float) Math.sqrt(1f - r2);
                // Coordenadas esféricas para que las bandas sigan la curvatura.
                float lat = (float) Math.asin(ny);
                float lon = (float) Math.atan2(nx, nz);
                float turb = fbm(lon * 2.2f + 3f, lat * 5f, 7) * 1.6f;
                float band = 0.5f + 0.5f * (float) Math.sin(lat * 9f + turb * 2.4f);
                float storm = Math.max(0f, fbm(lon * 4f, lat * 10f, 31) - 0.55f) * 2.2f;

                int base = band < 0.5f
                        ? OrbitaTheme.lerpColor(deep, violet, band * 2f)
                        : OrbitaTheme.lerpColor(violet, lilac, (band - 0.5f) * 2f * 0.75f);
                base = OrbitaTheme.lerpColor(base, cyan, storm * 0.55f);

                float lambert = Math.max(0f, nx * lx + ny * ly + nz * lz);
                float light = 0.16f + 0.95f * lambert;
                float rim = (float) Math.pow(1f - nz, 3) * 0.9f;
                int lit = scale(base, light);
                lit = add(lit, OrbitaTheme.ACCENT_2, rim * 0.55f);
                // Borde suavizado (antialias)
                float edge = Math.min(1f, (1f - (float) Math.sqrt(r2)) * s * 0.5f);
                img.setPixel(x, y, ((int) (edge * 255) << 24) | (lit & 0xFFFFFF));
            }
        }
        return img;
    }

    // ------------------------------------------------------------------ anillo

    /** Anillo visto desde arriba: al dibujarlo se aplana en Y y se inclina. */
    private static NativeImage makeRing() {
        int s = RING;
        NativeImage img = new NativeImage(s, s, true);
        for (int y = 0; y < s; y++) {
            for (int x = 0; x < s; x++) {
                float nx = (x + 0.5f) / s * 2f - 1f;
                float ny = (y + 0.5f) / s * 2f - 1f;
                float r = (float) Math.sqrt(nx * nx + ny * ny);
                if (r < 0.62f || r > 1f) {
                    img.setPixel(x, y, 0);
                    continue;
                }
                float t = (r - 0.62f) / 0.38f;
                float bands = 0.55f + 0.45f * (float) Math.sin(t * 46f) * (float) Math.sin(t * 13f + 1.3f);
                float gap = (t > 0.58f && t < 0.64f) ? 0.15f : 1f;
                float fade = Math.min(1f, Math.min(t, 1f - t) * 14f);
                float a = Math.max(0f, Math.min(1f, bands * gap * fade)) * 0.85f;
                int color = OrbitaTheme.lerpColor(0xFFB7A8FF, 0xFF6FD3FF, t);
                color = OrbitaTheme.lerpColor(color, 0xFFFFFFFF, Math.max(0f, bands - 0.75f));
                img.setPixel(x, y, ((int) (a * 255) << 24) | (color & 0xFFFFFF));
            }
        }
        return img;
    }

    // ------------------------------------------------------------------ luna

    private static NativeImage makeMoon() {
        int s = MOON;
        NativeImage img = new NativeImage(s, s, true);
        Random rnd = new Random(42);
        float[][] craters = new float[22][3];
        for (float[] c : craters) {
            c[0] = rnd.nextFloat() * 1.6f - 0.8f;
            c[1] = rnd.nextFloat() * 1.6f - 0.8f;
            c[2] = 0.05f + rnd.nextFloat() * 0.16f;
        }
        for (int y = 0; y < s; y++) {
            for (int x = 0; x < s; x++) {
                float nx = (x + 0.5f) / s * 2f - 1f;
                float ny = (y + 0.5f) / s * 2f - 1f;
                float r2 = nx * nx + ny * ny;
                if (r2 > 1f) {
                    img.setPixel(x, y, 0);
                    continue;
                }
                float nz = (float) Math.sqrt(1f - r2);
                float shade = 0.82f + 0.18f * fbm(nx * 4f, ny * 4f, 5);
                for (float[] c : craters) {
                    float d = (float) Math.hypot(nx - c[0], ny - c[1]) / c[2];
                    if (d < 1f) {
                        shade *= 0.86f + 0.1f * d;
                    } else if (d < 1.15f) {
                        shade *= 1.06f;
                    }
                }
                float light = 0.35f + 0.75f * Math.max(0f, nx * -0.4f + ny * -0.5f + nz * 0.77f);
                int base = scale(0xFFE9E2CF, shade * light);
                float edge = Math.min(1f, (1f - (float) Math.sqrt(r2)) * s * 0.5f);
                img.setPixel(x, y, ((int) (edge * 255) << 24) | (base & 0xFFFFFF));
            }
        }
        return img;
    }

    // ------------------------------------------------------------------ brillo

    private static NativeImage makeGlow() {
        int s = GLOW;
        NativeImage img = new NativeImage(s, s, true);
        for (int y = 0; y < s; y++) {
            for (int x = 0; x < s; x++) {
                float nx = (x + 0.5f) / s * 2f - 1f;
                float ny = (y + 0.5f) / s * 2f - 1f;
                float r = (float) Math.sqrt(nx * nx + ny * ny);
                float a = Math.max(0f, 1f - r);
                a = a * a * (3f - 2f * a);
                img.setPixel(x, y, ((int) (a * 255) << 24) | 0xFFFFFF);
            }
        }
        return img;
    }

    // ------------------------------------------------------------------ planeta que gira

    public static final int SPIN_SIZE = 384;
    public static final int SPIN_FRAMES = 48;
    private static Identifier[] spinFrames;
    private static volatile int spinState;

    /**
     * Fotogramas del planeta girando sobre su eje (se calculan una vez en segundo plano, ~1 s). Devuelve null
     * mientras no estén listos; entonces se usa el planeta fijo.
     */
    public static Identifier[] spinFrames() {
        if (spinState == 0 && state == 2) {
            spinState = 1;
            Thread t = new Thread(() -> {
                NativeImage[] frames = new NativeImage[SPIN_FRAMES];
                for (int i = 0; i < SPIN_FRAMES; i++) {
                    frames[i] = makeSpinFrame(SPIN_SIZE, (float) (i * 2 * Math.PI / SPIN_FRAMES));
                }
                Minecraft.getInstance().execute(() -> {
                    Identifier[] ids = new Identifier[SPIN_FRAMES];
                    for (int i = 0; i < SPIN_FRAMES; i++) {
                        ids[i] = register("planet_spin_" + i, frames[i]);
                    }
                    spinFrames = ids;
                    spinState = 2;
                });
            }, "Orbita-PlanetSpin");
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            t.start();
        }
        return spinState == 2 ? spinFrames : null;
    }

    /** Planeta como makePlanet, pero la superficie (bandas y tormentas) girada rot radianes; la luz no se mueve. */
    private static NativeImage makeSpinFrame(int s, float rot) {
        NativeImage img = new NativeImage(s, s, true);
        float lx = -0.55f, ly = -0.55f, lz = 0.63f;
        int deep = 0xFF1A1240;
        int violet = 0xFF5B3FC4;
        int lilac = 0xFFA996FF;
        int cyan = 0xFF5CC8FF;
        float cr = (float) Math.cos(rot);
        float sr = (float) Math.sin(rot);
        for (int y = 0; y < s; y++) {
            for (int x = 0; x < s; x++) {
                float nx = (x + 0.5f) / s * 2f - 1f;
                float ny = (y + 0.5f) / s * 2f - 1f;
                float r2 = nx * nx + ny * ny;
                if (r2 > 1f) {
                    img.setPixel(x, y, 0);
                    continue;
                }
                float nz = (float) Math.sqrt(1f - r2);
                // Punto de la superficie girado alrededor del eje vertical: ruido 3D, sin costuras.
                float px = nx * cr + nz * sr;
                float pz = -nx * sr + nz * cr;
                float lat = (float) Math.asin(ny);
                float turb = fbm3(px * 3.2f + 3f, ny * 5f, pz * 3.2f, 7) * 1.6f;
                float band = 0.5f + 0.5f * (float) Math.sin(lat * 9f + turb * 2.4f);
                float storm = Math.max(0f, fbm3(px * 6f, ny * 10f, pz * 6f, 31) - 0.55f) * 2.2f;
                int base = band < 0.5f
                        ? OrbitaTheme.lerpColor(deep, violet, band * 2f)
                        : OrbitaTheme.lerpColor(violet, lilac, (band - 0.5f) * 2f * 0.75f);
                base = OrbitaTheme.lerpColor(base, cyan, storm * 0.55f);
                float lambert = Math.max(0f, nx * lx + ny * ly + nz * lz);
                float light = 0.16f + 0.95f * lambert;
                float rim = (float) Math.pow(1f - nz, 3) * 0.9f;
                int lit = scale(base, light);
                lit = add(lit, OrbitaTheme.ACCENT_2, rim * 0.55f);
                float edge = Math.min(1f, (1f - (float) Math.sqrt(r2)) * s * 0.5f);
                img.setPixel(x, y, ((int) (edge * 255) << 24) | (lit & 0xFFFFFF));
            }
        }
        return img;
    }

    private static float fbm3(float x, float y, float z, int seed) {
        float sum = 0f;
        float amp = 0.5f;
        float freq = 1f;
        for (int i = 0; i < 4; i++) {
            sum += amp * noise3(x * freq, y * freq, z * freq, seed + i * 101);
            amp *= 0.5f;
            freq *= 2f;
        }
        return sum / 0.9375f;
    }

    private static float noise3(float x, float y, float z, int seed) {
        int xi = (int) Math.floor(x), yi = (int) Math.floor(y), zi = (int) Math.floor(z);
        float xf = x - xi, yf = y - yi, zf = z - zi;
        float u = xf * xf * (3 - 2 * xf), v = yf * yf * (3 - 2 * yf), w = zf * zf * (3 - 2 * zf);
        float c000 = hash3(xi, yi, zi, seed), c100 = hash3(xi + 1, yi, zi, seed);
        float c010 = hash3(xi, yi + 1, zi, seed), c110 = hash3(xi + 1, yi + 1, zi, seed);
        float c001 = hash3(xi, yi, zi + 1, seed), c101 = hash3(xi + 1, yi, zi + 1, seed);
        float c011 = hash3(xi, yi + 1, zi + 1, seed), c111 = hash3(xi + 1, yi + 1, zi + 1, seed);
        float x00 = c000 + (c100 - c000) * u, x10 = c010 + (c110 - c010) * u;
        float x01 = c001 + (c101 - c001) * u, x11 = c011 + (c111 - c011) * u;
        float y0 = x00 + (x10 - x00) * v, y1 = x01 + (x11 - x01) * v;
        return y0 + (y1 - y0) * w;
    }

    private static float hash3(int x, int y, int z, int seed) {
        int h = x * 374761393 + y * 668265263 + z * 1442695041 + seed * 982451653;
        h = (h ^ (h >>> 13)) * 1274126177;
        return ((h ^ (h >>> 16)) & 0xFFFF) / 65535f;
    }

    // ------------------------------------------------------------------ disco y arco

    private static NativeImage makeDisc() {
        int s = DISC;
        NativeImage img = new NativeImage(s, s, true);
        float half = s / 2f;
        for (int y = 0; y < s; y++) {
            for (int x = 0; x < s; x++) {
                float d = (float) Math.hypot(x + 0.5f - half, y + 0.5f - half);
                // Borde suavizado de ~1,5 píxeles de textura.
                float a = Math.max(0f, Math.min(1f, (half - d) / 1.5f));
                img.setPixel(x, y, ((int) (a * 255) << 24) | 0xFFFFFF);
            }
        }
        return img;
    }

    /** Anillo fino (radio 0,86, grosor 0,09) visible en 200° detrás del ángulo 0, con punta brillante. */
    private static NativeImage makeArc() {
        int s = ARC;
        NativeImage img = new NativeImage(s, s, true);
        float half = s / 2f;
        float radius = half * 0.86f;
        float thick = half * 0.09f;
        float span = (float) Math.toRadians(200);
        for (int y = 0; y < s; y++) {
            for (int x = 0; x < s; x++) {
                float dx = x + 0.5f - half;
                float dy = y + 0.5f - half;
                float d = (float) Math.hypot(dx, dy);
                float ring = Math.max(0f, 1f - Math.abs(d - radius) / thick);
                float ang = (float) Math.atan2(dy, dx); // -π..π, 0 = cabeza
                float behind = ang <= 0 ? -ang : (float) (2 * Math.PI - ang);
                float tail = behind > span ? 0f : (float) Math.pow(1f - behind / span, 1.6f);
                float a = ring * tail;
                // Punta: un punto blanco más gordo justo en la cabeza.
                float hd = (float) Math.hypot(dx - radius, dy);
                a = Math.max(a, Math.max(0f, 1f - hd / (thick * 1.7f)));
                img.setPixel(x, y, ((int) (Math.min(1f, a) * 255) << 24) | 0xFFFFFF);
            }
        }
        return img;
    }

    // ------------------------------------------------------------------ cohete

    /** Cohete apuntando hacia arriba (32x64). */
    private static NativeImage makeRocket() {
        int w = ROCKET_W;
        int h = ROCKET_H;
        NativeImage img = new NativeImage(w, h, true);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                img.setPixel(x, y, 0);
            }
        }
        float cx = w / 2f;
        for (int y = 0; y < h; y++) {
            float t = y / (float) h;
            // Perfil del fuselaje: punta redondeada arriba, cuerpo recto, base estrecha.
            float half;
            if (t < 0.32f) {
                float k = t / 0.32f;
                half = 7.5f * (float) Math.sqrt(k);
            } else if (t < 0.78f) {
                half = 7.5f;
            } else {
                half = 7.5f - (t - 0.78f) / 0.22f * 2.5f;
            }
            for (int x = 0; x < w; x++) {
                float dx = x + 0.5f - cx;
                if (Math.abs(dx) <= half) {
                    float shade = 1f - Math.max(0f, dx / half) * 0.35f + Math.max(0f, -dx / half) * 0.05f;
                    int c = t < 0.26f ? 0xFF8B7CFF : 0xFFEDEBF7;
                    if (t > 0.70f && t < 0.75f) {
                        c = 0xFF8B7CFF;
                    }
                    img.setPixel(x, y, scale(c, shade) | 0xFF000000);
                }
            }
        }
        // Ventanilla
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float d = (float) Math.hypot(x + 0.5f - cx, y + 0.5f - 27f);
                if (d < 4.2f) {
                    img.setPixel(x, y, d < 3f ? 0xFF5CC8FF : 0xFF3A2E6E);
                }
            }
        }
        // Aletas
        for (int y = 44; y < 60; y++) {
            float k = (y - 44) / 16f;
            int reach = (int) (4 + k * 6);
            for (int i = 0; i < reach; i++) {
                int left = (int) (cx - 7.5f) - 1 - i;
                int right = (int) (cx + 7.5f) + i;
                if (left >= 0) {
                    img.setPixel(left, y, 0xFF6B4BD6);
                }
                if (right < w) {
                    img.setPixel(right, y, 0xFF5B3FC4);
                }
            }
        }
        return img;
    }

    // ------------------------------------------------------------------ utilidades

    private static int scale(int argb, float k) {
        int r = Math.min(255, (int) (((argb >> 16) & 0xFF) * k));
        int g = Math.min(255, (int) (((argb >> 8) & 0xFF) * k));
        int b = Math.min(255, (int) ((argb & 0xFF) * k));
        return (argb & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    private static int add(int argb, int tint, float k) {
        int r = Math.min(255, ((argb >> 16) & 0xFF) + (int) (((tint >> 16) & 0xFF) * k));
        int g = Math.min(255, ((argb >> 8) & 0xFF) + (int) (((tint >> 8) & 0xFF) * k));
        int b = Math.min(255, (argb & 0xFF) + (int) ((tint & 0xFF) * k));
        return (argb & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    static float fbm(float x, float y, int seed) {
        float sum = 0f;
        float amp = 0.5f;
        float freq = 1f;
        for (int i = 0; i < 4; i++) {
            sum += amp * noise(x * freq, y * freq, seed + i * 101);
            amp *= 0.5f;
            freq *= 2f;
        }
        return sum / 0.9375f;
    }

    private static float noise(float x, float y, int seed) {
        int xi = (int) Math.floor(x);
        int yi = (int) Math.floor(y);
        float xf = x - xi;
        float yf = y - yi;
        float u = xf * xf * (3 - 2 * xf);
        float v = yf * yf * (3 - 2 * yf);
        float a = hash(xi, yi, seed);
        float b = hash(xi + 1, yi, seed);
        float c = hash(xi, yi + 1, seed);
        float d = hash(xi + 1, yi + 1, seed);
        return a + (b - a) * u + (c - a) * v + (a - b - c + d) * u * v;
    }

    private static float hash(int x, int y, int seed) {
        int h = x * 374761393 + y * 668265263 + seed * 982451653;
        h = (h ^ (h >>> 13)) * 1274126177;
        return ((h ^ (h >>> 16)) & 0xFFFF) / 65535f;
    }
}
