package dev.orbita.client.feature;

import net.minecraft.client.Minecraft;

/** Velocidad horizontal del jugador en bloques por segundo (suavizada). */
public final class MotionTracker {
    private static double lastX = Double.NaN;
    private static double lastZ;
    private static double speed;

    private MotionTracker() {
    }

    public static void tick(Minecraft mc) {
        if (mc.player == null) {
            lastX = Double.NaN;
            speed = 0;
            return;
        }
        double x = mc.player.getX();
        double z = mc.player.getZ();
        if (!Double.isNaN(lastX)) {
            double perSecond = Math.hypot(x - lastX, z - lastZ) * 20.0;
            speed = speed * 0.6 + perSecond * 0.4;
        }
        lastX = x;
        lastZ = z;
    }

    public static double speed() {
        return speed;
    }
}
