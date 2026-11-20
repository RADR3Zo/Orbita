package dev.orbita.client.ui.render;

import dev.orbita.client.ui.theme.OrbitaTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

import java.util.Random;

/** Campo de estrellas precalculado: parpadeo, deriva lenta, paralaje con el ratón y estrellas fugaces. */
public final class Starfield {
    public static final Starfield SHARED = new Starfield(0x0B17AL);

    private static final int STAR_COUNT = 240;
    private final float[] x = new float[STAR_COUNT];
    private final float[] y = new float[STAR_COUNT];
    private final float[] depth = new float[STAR_COUNT];
    private final float[] phase = new float[STAR_COUNT];
    private final Random random;

    private float shootX, shootY, shootStart = -100f;

    public Starfield(long seed) {
        random = new Random(seed);
        for (int i = 0; i < STAR_COUNT; i++) {
            x[i] = random.nextFloat();
            y[i] = random.nextFloat();
            depth[i] = 0.2f + random.nextFloat() * 0.8f;
            phase[i] = random.nextFloat() * Mth.TWO_PI;
        }
    }

    public void render(GuiGraphics g, int width, int height, int mouseX, int mouseY) {
        float time = dev.orbita.client.ui.anim.Animation.seconds();

        g.fillGradient(0, 0, width, height, OrbitaTheme.SPACE_BLACK, OrbitaTheme.CHARCOAL);

        // Nebulosas: bandas translúcidas que "respiran" lentamente.
        int pulse = (int) (22 + 10 * Mth.sin(time * 0.4f));
        g.fillGradient(0, height / 3, width, height, 0x00000000, (pulse << 24) | 0x3A2A7A);
        g.fillGradient(0, 0, width, height / 2, ((pulse / 2) << 24) | 0x1A3A6A, 0x00000000);

        float px = (mouseX / (float) Math.max(1, width) - 0.5f) * 14f;
        float py = (mouseY / (float) Math.max(1, height) - 0.5f) * 14f;

        for (int i = 0; i < STAR_COUNT; i++) {
            float d = depth[i];
            float sx = ((x[i] + time * 0.002f * d) % 1f) * width + px * d;
            float sy = y[i] * height + py * d;

            float twinkle = 0.55f + 0.45f * Mth.sin(time * (1.5f + d * 2f) + phase[i]);
            int alpha = (int) (255 * twinkle * (0.3f + 0.7f * d));
            int ix = (int) sx;
            int iy = (int) sy;
            int size = d > 0.85f ? 2 : 1;
            g.fill(ix, iy, ix + size, iy + size, (alpha << 24) | 0xFFFFFF);
            if (d > 0.94f) {
                int glow = ((alpha / 4) << 24) | 0xB8B0FF;
                g.fill(ix - 1, iy, ix + size + 1, iy + size, glow);
                g.fill(ix, iy - 1, ix + size, iy + size + 1, glow);
            }
        }

        renderShootingStar(g, width, height, time);
    }

    private void renderShootingStar(GuiGraphics g, int width, int height, float time) {
        float age = time - shootStart;
        if (age > 7f + random.nextFloat() * 4f) {
            shootStart = time;
            shootX = random.nextFloat() * width * 0.7f + width * 0.2f;
            shootY = random.nextFloat() * height * 0.4f;
            age = 0f;
        }
        if (age > 1.1f) {
            return;
        }
        float t = age / 1.1f;
        float headX = shootX - t * width * 0.35f;
        float headY = shootY + t * height * 0.2f;
        float fade = Mth.sin(t * Mth.PI);
        for (int i = 0; i < 18; i++) {
            float k = i / 18f;
            int a = (int) (200 * fade * (1f - k));
            int tx = (int) (headX + k * 30f);
            int ty = (int) (headY - k * 17f);
            g.fill(tx, ty, tx + 1, ty + 1, (a << 24) | 0xE6E2FF);
        }
    }
}
