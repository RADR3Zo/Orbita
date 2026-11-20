package dev.orbita.client.feature;

import dev.orbita.client.config.OrbitaConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Fullbright: ves todo con luz máxima (cuevas, noche, Nether) aunque no haya antorchas.
 *
 * <p>Funciona dándote, solo en tu pantalla, un efecto de visión nocturna infinito y sin partículas ni icono.
 * El servidor no se entera y no cambia nada del juego para los demás. Al apagarlo se quita ese efecto,
 * pero no una visión nocturna de verdad (de una poción, por ejemplo).
 */
public final class FullbrightFeature {
    private static boolean applied;

    private FullbrightFeature() {
    }

    public static boolean enabled() {
        return OrbitaConfig.get().fullbright;
    }

    public static void setEnabled(boolean value) {
        OrbitaConfig.get().fullbright = value;
        OrbitaConfig.save();
    }

    public static void toggle() {
        setEnabled(!enabled());
    }

    /** Se llama en cada tick del cliente. */
    public static void tick(Minecraft mc) {
        var player = mc.player;
        if (player == null) {
            applied = false;
            return;
        }
        if (enabled()) {
            MobEffectInstance cur = player.getEffect(MobEffects.NIGHT_VISION);
            // Si moriste, cambiaste de dimensión o se acabó una poción, se vuelve a poner.
            if (cur == null || !cur.isInfiniteDuration()) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, MobEffectInstance.INFINITE_DURATION, 0,
                        false, false, false));
            }
            applied = true;
        } else if (applied) {
            MobEffectInstance cur = player.getEffect(MobEffects.NIGHT_VISION);
            if (cur != null && isOurs(cur)) {
                player.removeEffect(MobEffects.NIGHT_VISION);
            }
            applied = false;
        }
    }

    /** ¿Este efecto es el que pone Fullbright? (infinito y sin icono ni partículas) */
    public static boolean isOurs(MobEffectInstance e) {
        return e.getEffect() == MobEffects.NIGHT_VISION && e.isInfiniteDuration() && !e.isVisible() && !e.showIcon();
    }
}
