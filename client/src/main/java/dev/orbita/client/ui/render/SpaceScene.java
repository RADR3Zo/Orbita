package dev.orbita.client.ui.render;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Composición de la escena del menú: neblina, luna, planeta con anillo inclinado y cohete en órbita. */
public final class SpaceScene {
    private SpaceScene() {
    }

    /** Dibuja una textura completa escalada a (w,h) con un tinte ARGB. */
    public static void tex(GuiGraphics g, Identifier id, float x, float y, float w, float h, int texW, int texH, int color) {
        if (id == null) {
            return;
        }
        float fade = SpaceArt.fade();
        if (fade < 1f) {
            color = ((int) (((color >>> 24) & 0xFF) * fade) << 24) | (color & 0x00FFFFFF);
        }
        g.blit(RenderPipelines.GUI_TEXTURED, id, Math.round(x), Math.round(y), 0f, 0f,
                Math.max(1, Math.round(w)), Math.max(1, Math.round(h)), texW, texH, texW, texH, color);
    }

    public static void glow(GuiGraphics g, float cx, float cy, float rx, float ry, int color) {
        tex(g, SpaceArt.glow(), cx - rx, cy - ry, rx * 2, ry * 2, SpaceArt.GLOW, SpaceArt.GLOW, color);
    }

    /** Neblina violeta y verde en la parte inferior, que se desplaza lentamente. */
    public static void fog(GuiGraphics g, int width, int height, float time) {
        if (!SpaceArt.ready()) {
            return;
        }
        float drift = Mth.sin(time * 0.08f) * width * 0.05f;
        glow(g, width * 0.22f + drift, height * 1.02f, width * 0.55f, height * 0.32f, 0x445A2FD6);
        glow(g, width * 0.70f - drift, height * 1.05f, width * 0.50f, height * 0.26f, 0x2C2FD6A0);
        glow(g, width * 0.45f, height * 0.98f, width * 0.40f, height * 0.18f, 0x22B07CFF);
    }

    /** Nebulosas de colores en la parte alta que se mueven y respiran despacio. */
    public static void aurora(GuiGraphics g, int width, int height, float time) {
        if (!SpaceArt.ready()) {
            return;
        }
        float a = 0.75f + 0.25f * Mth.sin(time * 0.35f);
        glow(g, width * (0.30f + 0.04f * Mth.sin(time * 0.05f)), height * 0.10f, width * 0.42f, height * 0.30f,
                dev.orbita.client.ui.theme.OrbitaTheme.withAlpha(0x2E7A4CFF, a));
        glow(g, width * (0.52f - 0.05f * Mth.sin(time * 0.04f)), height * 0.22f, width * 0.30f, height * 0.20f,
                dev.orbita.client.ui.theme.OrbitaTheme.withAlpha(0x1E3FD0FF, 1.2f - a));
        glow(g, width * 0.05f, height * 0.55f, width * 0.25f, height * 0.40f, 0x183A2A9A);
        glow(g, width * 0.98f, height * 0.62f, width * 0.22f, height * 0.35f, 0x16FF5CC8);
    }

    /** Planeta lejano y pequeño (tintado) que da profundidad a la escena. */
    public static void distantPlanet(GuiGraphics g, float cx, float cy, float r, float time) {
        if (!SpaceArt.ready()) {
            return;
        }
        float bob = Mth.sin(time * 0.4f + 1.3f) * 1.5f;
        glow(g, cx, cy + bob, r * 2.2f, r * 2.2f, 0x3055E6D2);
        tex(g, SpaceArt.planet(), cx - r, cy - r + bob, r * 2, r * 2, SpaceArt.PLANET, SpaceArt.PLANET, 0xFF7FE8DA);
    }

    /** Destellos que recorren el anillo del planeta (solo la mitad delantera). */
    public static void ringSparkles(GuiGraphics g, float cx, float cy, float r, float tilt, float time) {
        if (!SpaceArt.ready()) {
            return;
        }
        float ringR = r * 1.85f;
        float b = ringR * 0.2f;
        float c = Mth.cos(tilt);
        float s = Mth.sin(tilt);
        for (int i = 0; i < 7; i++) {
            float theta = time * (0.18f + i * 0.013f) + i * 0.9f;
            float sn = Mth.sin(theta);
            if (sn < 0.05f) {
                continue; // detrás del planeta
            }
            float ox = ringR * Mth.cos(theta) * (0.9f + 0.08f * (i % 3));
            float oy = b * sn;
            float x = cx + ox * c - oy * s;
            float y = cy + ox * s + oy * c;
            float tw = 0.5f + 0.5f * Mth.sin(time * 3f + i * 2.1f);
            int alpha = (int) (200 * tw * sn);
            glow(g, x, y, 5 + 3 * tw, 5 + 3 * tw, (alpha << 24) | 0xCFE6FF);
            g.fill(Math.round(x), Math.round(y), Math.round(x) + 1, Math.round(y) + 1, (Math.min(255, alpha + 40) << 24) | 0xFFFFFF);
        }
    }

    /** Pequeño satélite que orbita la luna con una luz que parpadea. */
    public static void satellite(GuiGraphics g, float cx, float cy, float r, float time) {
        float theta = time * 0.55f;
        float x = cx + Mth.cos(theta) * r * 1.7f;
        float y = cy + Mth.sin(theta) * r * 0.45f - Mth.cos(theta) * r * 0.25f;
        if (Mth.sin(theta) < -0.2f && Math.abs(x - cx) < r) {
            return; // tapado por la luna
        }
        int ix = Math.round(x);
        int iy = Math.round(y);
        g.fill(ix - 4, iy, ix - 1, iy + 1, 0xFF5C7BD9);
        g.fill(ix + 2, iy, ix + 5, iy + 1, 0xFF5C7BD9);
        g.fill(ix - 1, iy - 1, ix + 2, iy + 2, 0xFFD9DEEA);
        boolean blink = (int) (time * 1.6f) % 2 == 0;
        if (blink && SpaceArt.ready()) {
            glow(g, ix, iy - 1, 4, 4, 0xC0FF5C5C);
        }
    }

    // Meteoros grandes con cabeza brillante, más llamativos que las estrellas fugaces del fondo.
    private static float meteorStart = -100f;
    private static float meteorX, meteorY, meteorAngle, meteorLen;
    private static final java.util.Random METEOR_RANDOM = new java.util.Random();

    public static void meteors(GuiGraphics g, int width, int height, float time) {
        if (!SpaceArt.ready()) {
            return;
        }
        float age = time - meteorStart;
        if (age > 4.5f) {
            meteorStart = time + METEOR_RANDOM.nextFloat() * 2.5f;
            meteorX = width * (0.35f + METEOR_RANDOM.nextFloat() * 0.65f);
            meteorY = height * (METEOR_RANDOM.nextFloat() * 0.35f);
            meteorAngle = 2.45f + METEOR_RANDOM.nextFloat() * 0.35f;
            meteorLen = width * (0.25f + METEOR_RANDOM.nextFloat() * 0.25f);
            return;
        }
        if (age < 0f || age > 1.3f) {
            return;
        }
        float t = age / 1.3f;
        float fade = Mth.sin(t * Mth.PI);
        float dx = Mth.cos(meteorAngle);
        float dy = Mth.sin(meteorAngle);
        float hx = meteorX + dx * meteorLen * t;
        float hy = meteorY + dy * meteorLen * t;
        for (int i = 0; i < 28; i++) {
            float k = i / 28f;
            float x = hx - dx * k * 70f;
            float y = hy - dy * k * 70f;
            int a = (int) (180 * fade * (1f - k));
            glow(g, x, y, 3.5f * (1f - k) + 1f, 3.5f * (1f - k) + 1f, (a << 24) | 0xB8C4FF);
        }
        glow(g, hx, hy, 9, 9, ((int) (220 * fade) << 24) | 0xFFFFFF);
        g.fill(Math.round(hx), Math.round(hy), Math.round(hx) + 2, Math.round(hy) + 2, ((int) (255 * fade) << 24) | 0xFFFFFF);
    }

    /** Onda expansiva alrededor del planeta (al hacer clic sobre él). */
    public static void shockwave(GuiGraphics g, float cx, float cy, float r, float age) {
        if (age < 0f || age > 1.2f) {
            return;
        }
        float t = age / 1.2f;
        float e = 1f - (1f - t) * (1f - t);
        int alpha = (int) (200 * (1f - t));
        Fx.arc(g, cx, cy, r * (1.05f + e * 1.6f), 0, Mth.TWO_PI, 2f, (alpha << 24) | 0xB7A8FF, false);
        Fx.arc(g, cx, cy, r * (1.0f + e * 1.1f), 0, Mth.TWO_PI, 1f, ((alpha / 2) << 24) | 0x7FE8DA, false);
        glow(g, cx, cy, r * (1.6f + e), r * (1.6f + e), ((int) (90 * (1f - t)) << 24) | 0x8B7CFF);
    }

    public static void moon(GuiGraphics g, float cx, float cy, float r) {
        if (!SpaceArt.ready()) {
            return;
        }
        glow(g, cx, cy, r * 2.4f, r * 2.4f, 0x30FFF1C8);
        tex(g, SpaceArt.moon(), cx - r, cy - r, r * 2, r * 2, SpaceArt.MOON, SpaceArt.MOON, 0xFFFFFFFF);
    }

    /**
     * Planeta de radio r centrado en (cx,cy) con anillo inclinado. Si rocket es true, un cohete orbita
     * alrededor y pasa por delante y por detrás del planeta.
     */
    public static void planet(GuiGraphics g, float cx, float cy, float r, float tilt, float time, boolean rocket) {
        if (!SpaceArt.ready()) {
            return;
        }
        float ringR = r * 2.05f;
        float squash = 0.2f;
        float orbitA = r * 2.5f;
        float orbitB = r * 0.62f;
        float theta = time * 0.32f;
        boolean rocketBehind = Mth.sin(theta) < 0;

        glow(g, cx, cy, r * 1.75f, r * 1.75f, 0x608B7CFF);
        if (rocket && rocketBehind) {
            rocket(g, cx, cy, orbitA, orbitB, tilt, theta);
        }
        ringHalf(g, cx, cy, ringR, tilt, squash, true);
        tex(g, SpaceArt.planet(), cx - r, cy - r, r * 2, r * 2, SpaceArt.PLANET, SpaceArt.PLANET, 0xFFFFFFFF);
        ringHalf(g, cx, cy, ringR, tilt, squash, false);
        if (rocket && !rocketBehind) {
            rocket(g, cx, cy, orbitA, orbitB, tilt, theta);
        }
    }

    // ------------------------------------------------------------------ piezas sueltas (menú animado)

    public static final float RING_SQUASH = 0.2f;

    /** Brillo y mitad trasera del anillo (lo que va detrás del planeta y del cohete cuando está detrás). */
    public static void planetBack(GuiGraphics g, float cx, float cy, float r, float tilt) {
        if (!SpaceArt.ready()) {
            return;
        }
        glow(g, cx, cy, r * 1.75f, r * 1.75f, 0x608B7CFF);
        ringHalf(g, cx, cy, r * 2.05f, tilt, RING_SQUASH, true);
    }

    /** Superficie del planeta girando sobre su eje (periodo en segundos) y mitad delantera del anillo. */
    public static void planetFront(GuiGraphics g, float cx, float cy, float r, float tilt, float time, float period) {
        if (!SpaceArt.ready()) {
            return;
        }
        Identifier[] frames = SpaceArt.spinFrames();
        if (frames == null) {
            tex(g, SpaceArt.planet(), cx - r, cy - r, r * 2, r * 2, SpaceArt.PLANET, SpaceArt.PLANET, 0xFFFFFFFF);
        } else {
            float phase = (time / period % 1f + 1f) % 1f * frames.length;
            int i = (int) phase;
            float f = phase - i;
            int s = SpaceArt.SPIN_SIZE;
            // Fundido entre dos fotogramas seguidos: el giro se ve continuo.
            tex(g, frames[i % frames.length], cx - r, cy - r, r * 2, r * 2, s, s, 0xFFFFFFFF);
            if (f > 0.02f) {
                tex(g, frames[(i + 1) % frames.length], cx - r, cy - r, r * 2, r * 2, s, s, ((int) (f * 255) << 24) | 0xFFFFFF);
            }
        }
        ringHalf(g, cx, cy, r * 2.05f, tilt, RING_SQUASH, false);
    }

    /** Punto de la órbita del cohete alrededor de un planeta de radio r. */
    public static float[] rocketOrbit(float cx, float cy, float r, float tilt, float theta) {
        return orbit(cx, cy, r * 2.5f, r * 0.62f, tilt, theta);
    }

    /** Cohete en (x,y) apuntando en la dirección angle (radianes, 0 = hacia la derecha), con llama. */
    public static void rocketAt(GuiGraphics g, float x, float y, float angle, float depth, float thrust) {
        if (!SpaceArt.ready()) {
            return;
        }
        float w = 12f * depth;
        float h = 24f * depth;
        float dx = Mth.cos(angle);
        float dy = Mth.sin(angle);
        float flicker = 0.85f + 0.15f * Mth.sin(dev.orbita.client.ui.anim.Animation.seconds() * 40f);
        float fl = h * (0.55f + 0.35f * thrust);
        glow(g, x - dx * fl, y - dy * fl, w * (0.7f + 0.5f * thrust) * flicker, w * (0.7f + 0.5f * thrust) * flicker, 0xC0FFB347);
        if (thrust > 0.3f) {
            glow(g, x - dx * fl * 1.4f, y - dy * fl * 1.4f, w * 0.6f * flicker, w * 0.6f * flicker, 0x90FF5C3A);
        }
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().rotate(angle + Mth.HALF_PI);
        tex(g, SpaceArt.rocket(), -w / 2f, -h / 2f, w, h, SpaceArt.ROCKET_W, SpaceArt.ROCKET_H, 0xFFFFFFFF);
        g.pose().popMatrix();
    }

    /** Estela de humo brillante a partir de una lista de posiciones (de la más nueva a la más vieja). */
    public static void trail(GuiGraphics g, float[] xs, float[] ys, int count, float depth) {
        if (!SpaceArt.ready()) {
            return;
        }
        for (int k = count - 1; k >= 1; k--) {
            float f = k / (float) count;
            float size = (5.5f - f * 4f) * depth;
            int alpha = (int) (120 * (1f - f));
            glow(g, xs[k], ys[k], size, size, (alpha << 24) | 0xC8D2FF);
        }
    }

    private static void ringHalf(GuiGraphics g, float cx, float cy, float ringR, float tilt, float squash, boolean back) {
        int r = Math.round(ringR);
        int half = SpaceArt.RING / 2;
        g.pose().pushMatrix();
        g.pose().translate(cx, cy);
        g.pose().rotate(tilt);
        g.pose().scale(1f, squash);
        if (back) {
            g.blit(RenderPipelines.GUI_TEXTURED, SpaceArt.ring(), -r, -r, 0f, 0f, 2 * r, r,
                    SpaceArt.RING, half, SpaceArt.RING, SpaceArt.RING, fadeColor());
        } else {
            g.blit(RenderPipelines.GUI_TEXTURED, SpaceArt.ring(), -r, 0, 0f, half, 2 * r, r,
                    SpaceArt.RING, half, SpaceArt.RING, SpaceArt.RING, fadeColor());
        }
        g.pose().popMatrix();
    }

    private static int fadeColor() {
        return ((int) (255 * SpaceArt.fade()) << 24) | 0xFFFFFF;
    }

    private static float[] orbit(float cx, float cy, float a, float b, float tilt, float theta) {
        float ox = a * Mth.cos(theta);
        float oy = b * Mth.sin(theta);
        float c = Mth.cos(tilt);
        float s = Mth.sin(tilt);
        return new float[]{cx + ox * c - oy * s, cy + ox * s + oy * c};
    }

    private static void rocket(GuiGraphics g, float cx, float cy, float a, float b, float tilt, float theta) {
        float depth = 0.85f + 0.25f * Mth.sin(theta);
        // Estela
        for (int k = 14; k >= 1; k--) {
            float[] p = orbit(cx, cy, a, b, tilt, theta - k * 0.03f);
            float size = (5.5f - k * 0.3f) * depth;
            int alpha = (int) (110 * (1f - k / 15f));
            glow(g, p[0], p[1], size, size, (alpha << 24) | 0xC8D2FF);
        }
        float[] p = orbit(cx, cy, a, b, tilt, theta);
        float[] q = orbit(cx, cy, a, b, tilt, theta + 0.01f);
        float angle = (float) Math.atan2(q[1] - p[1], q[0] - p[0]) + Mth.HALF_PI;
        float w = 9f * depth;
        float h = 18f * depth;
        // Llama
        float fx = p[0] - (q[0] - p[0]) / Math.max(0.001f, (float) Math.hypot(q[0] - p[0], q[1] - p[1])) * h * 0.55f;
        float fy = p[1] - (q[1] - p[1]) / Math.max(0.001f, (float) Math.hypot(q[0] - p[0], q[1] - p[1])) * h * 0.55f;
        glow(g, fx, fy, w * 0.7f, w * 0.7f, 0xC0FFB347);
        g.pose().pushMatrix();
        g.pose().translate(p[0], p[1]);
        g.pose().rotate(angle);
        tex(g, SpaceArt.rocket(), -w / 2f, -h / 2f, w, h, SpaceArt.ROCKET_W, SpaceArt.ROCKET_H, 0xFFFFFFFF);
        g.pose().popMatrix();
    }
}
