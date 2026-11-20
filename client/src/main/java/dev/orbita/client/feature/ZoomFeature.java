package dev.orbita.client.feature;

import dev.orbita.client.config.OrbitaConfig;
import dev.orbita.client.ui.anim.Animation;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

/** Zoom integrado: mantener la tecla reduce el FOV con una transición suave. */
public final class ZoomFeature {
    private static final Animation PROGRESS = new Animation(0f, 14f);
    private static boolean zooming;
    private static double level = 4.0;
    private static boolean appliedSmoothCamera;
    private static boolean savedSmoothCamera;

    private ZoomFeature() {
    }

    public static void tick(Minecraft mc, boolean keyDown) {
        boolean want = keyDown && mc.screen == null && mc.player != null;
        if (want == zooming) {
            return;
        }
        zooming = want;
        OrbitaConfig cfg = OrbitaConfig.get();
        if (want) {
            level = cfg.zoomLevel;
            if (cfg.zoomSmoothCamera) {
                savedSmoothCamera = mc.options.smoothCamera;
                mc.options.smoothCamera = true;
                appliedSmoothCamera = true;
            }
        } else if (appliedSmoothCamera) {
            mc.options.smoothCamera = savedSmoothCamera;
            appliedSmoothCamera = false;
        }
        PROGRESS.setTarget(want ? 1f : 0f);
        if (!cfg.zoomAnimated) {
            PROGRESS.snap(want ? 1f : 0f);
        }
    }

    public static boolean isZooming() {
        return zooming;
    }

    public static void onScroll(double amount) {
        if (amount == 0) {
            return;
        }
        level = Mth.clamp(amount > 0 ? level * 1.25 : level / 1.25, 1.5, 50.0);
    }

    /** Multiplicador aplicado al FOV (1 = sin zoom). */
    public static float fovMultiplier() {
        float p = PROGRESS.update();
        if (p <= 0f) {
            return 1f;
        }
        return (float) Mth.lerp(p, 1.0, 1.0 / level);
    }
}
