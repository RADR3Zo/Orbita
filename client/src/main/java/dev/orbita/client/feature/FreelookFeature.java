package dev.orbita.client.feature;

import dev.orbita.client.config.OrbitaConfig;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

/**
 * Freelook (perspectiva libre): mientras está activo, el ratón gira solo la cámara alrededor del jugador
 * en tercera persona; el personaje sigue mirando y moviéndose hacia donde miraba.
 */
public final class FreelookFeature {
    private static boolean active;
    private static float yaw;
    private static float pitch;
    private static CameraType savedCamera;
    private static boolean toggled;
    private static boolean devForced;

    private FreelookFeature() {
    }

    /**
     * @param keyDown  la tecla de freelook está pulsada
     * @param keyClick la tecla se pulsó este tick (para el modo alternar)
     */
    public static void tick(Minecraft mc, boolean keyDown, boolean keyClick) {
        if (devForced) {
            return;
        }
        OrbitaConfig cfg = OrbitaConfig.get();
        if (cfg.freelookToggle) {
            if (keyClick) {
                toggled = !toggled;
            }
        } else {
            toggled = keyDown;
        }
        boolean want = toggled && cfg.freelookEnabled && mc.player != null && mc.screen == null;
        if (want == active) {
            return;
        }
        if (want) {
            yaw = mc.player.getYRot();
            pitch = mc.player.getXRot();
            savedCamera = mc.options.getCameraType();
            if (savedCamera == CameraType.FIRST_PERSON) {
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            }
            active = true;
        } else {
            active = false;
            toggled = false;
            if (savedCamera != null) {
                mc.options.setCameraType(savedCamera);
            }
        }
    }

    /** Solo para las capturas de desarrollo: activa el freelook girando la cámara dYaw grados. */
    public static void devForce(Minecraft mc, boolean on, float dYaw) {
        devForced = on;
        if (on && !active && mc.player != null) {
            yaw = mc.player.getYRot() + dYaw;
            pitch = 15f;
            savedCamera = mc.options.getCameraType();
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            active = true;
        } else if (!on && active) {
            active = false;
            mc.options.setCameraType(savedCamera);
        }
    }

    public static boolean isActive() {
        return active;
    }

    /** Recibe el movimiento del ratón (mismas unidades que Entity#turn). */
    public static void turn(double dYaw, double dPitch) {
        float invert = OrbitaConfig.get().freelookInvert ? -1f : 1f;
        yaw += (float) dYaw * 0.15f;
        pitch = Mth.clamp(pitch + (float) dPitch * 0.15f * invert, -90f, 90f);
    }

    public static float yaw() {
        return yaw;
    }

    public static float pitch() {
        return pitch;
    }
}
