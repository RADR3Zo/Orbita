package dev.orbita.client.nova.module;

import dev.orbita.client.nova.core.Setting;
import java.util.Set;
import net.minecraft.client.CloudStatus;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;

public final class Visual {
    private Visual() { }

    /** Quita particulas. Lo usa ParticleManagerMixin. */
    public static final class NoParticles extends Module {
        public final Setting.Bool all = add(new Setting.Bool("Remove everything", true));
        public final Setting.Bool weather = add(new Setting.Bool("Rain / splash", true)).showIf(() -> !all.value);
        public final Setting.Bool fire = add(new Setting.Bool("Fire / smoke", true)).showIf(() -> !all.value);
        public final Setting.Bool hits = add(new Setting.Bool("Crits / hits", false)).showIf(() -> !all.value);
        public final Setting.Bool potions = add(new Setting.Bool("Potion effects", true)).showIf(() -> !all.value);
        public final Setting.Bool blocks = add(new Setting.Bool("Block breaking", false)).showIf(() -> !all.value);

        private static final Set<ParticleType<?>> WEATHER = Set.of(ParticleTypes.RAIN, ParticleTypes.SPLASH);
        private static final Set<ParticleType<?>> FIRE = Set.of(ParticleTypes.FLAME, ParticleTypes.SMOKE, ParticleTypes.LARGE_SMOKE,
                ParticleTypes.LAVA, ParticleTypes.SOUL_FIRE_FLAME, ParticleTypes.CAMPFIRE_COSY_SMOKE, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,
                ParticleTypes.ASH);
        private static final Set<ParticleType<?>> HITS = Set.of(ParticleTypes.CRIT, ParticleTypes.ENCHANTED_HIT,
                ParticleTypes.DAMAGE_INDICATOR, ParticleTypes.SWEEP_ATTACK);
        private static final Set<ParticleType<?>> POTIONS = Set.of(ParticleTypes.ENTITY_EFFECT, ParticleTypes.EFFECT,
                ParticleTypes.INSTANT_EFFECT, ParticleTypes.WITCH);

        public NoParticles() { super("No Particles", "Quita todas las particulas (o las que elijas)", Category.VISUAL); }

        public boolean blocks(ParticleOptions p) {
            if (all.value) return true;
            ParticleType<?> t = p.getType();
            return (weather.value && WEATHER.contains(t)) || (fire.value && FIRE.contains(t))
                    || (hits.value && HITS.contains(t)) || (potions.value && POTIONS.contains(t))
                    || (blocks.value && p instanceof BlockParticleOption);
        }
    }

    public static final class NoClouds extends Module {
        private CloudStatus saved;

        public NoClouds() { super("No Clouds", "Quita las nubes", Category.VISUAL); }

        @Override public void onEnable() {
            var o = mc().options;
            saved = o.cloudStatus().get();
            o.cloudStatus().set(CloudStatus.OFF);
        }

        @Override public void onTick() {
            var o = mc().options;
            if (o.cloudStatus().get() != CloudStatus.OFF) o.cloudStatus().set(CloudStatus.OFF);
        }

        @Override public void onDisable() {
            if (saved != null) mc().options.cloudStatus().set(saved);
        }
    }

    public static final class NoWeather extends Module {
        public NoWeather() { super("No Weather", "Quita lluvia y tormenta (visual)", Category.VISUAL); }

        @Override public void onTick() {
            var w = mc().level;
            if (w != null) { w.setRainLevel(0f); w.setThunderLevel(0f); }
        }
    }

    /** Los efectos de estos tres los aplican mixins que consultan "enabled". */
    public static final class NoFire extends Module {
        public NoFire() { super("No Fire Overlay", "Quita el fuego que tapa la pantalla", Category.VISUAL); }
    }

    public static final class NoHurtCam extends Module {
        public NoHurtCam() { super("No Hurt Cam", "Sin sacudida al recibir daño", Category.VISUAL); }
    }

    /** Los corazones negros del efecto Wither se muestran como corazones normales (rojos). */
    public static final class NoWitherHearts extends Module {
        public NoWitherHearts() { super("No Wither Hearts", "Los corazones del Wither se ven normales (rojos)", Category.VISUAL); }
    }

    public static final class NoBobbing extends Module {
        private boolean saved;

        public NoBobbing() { super("No Bobbing", "Quita el balanceo al caminar", Category.VISUAL); }

        @Override public void onEnable() {
            saved = mc().options.bobView().get();
            mc().options.bobView().set(false);
        }

        @Override public void onDisable() { mc().options.bobView().set(saved); }
    }
}
