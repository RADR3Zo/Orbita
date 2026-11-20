package dev.orbita.client.nova.mixin;

import dev.orbita.client.nova.NovaClient;
import dev.orbita.client.nova.module.Visual;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleEngine.class)
public class ParticleManagerMixin {
    @Shadow private void clearParticles() { }

    private static Visual.NoParticles nova$module() {
        if (NovaClient.modules == null) return null;
        Visual.NoParticles m = NovaClient.modules.get(Visual.NoParticles.class);
        return m != null && m.enabled ? m : null;
    }

    /** Particulas que se crean por tipo (humo, lluvia, criticos, pociones...). */
    @Inject(method = "createParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)Lnet/minecraft/client/particle/Particle;",
            at = @At("HEAD"), cancellable = true)
    private void nova$noParticles(ParticleOptions effect, double x, double y, double z, double vx, double vy, double vz,
                                  CallbackInfoReturnable<Particle> cir) {
        Visual.NoParticles m = nova$module();
        if (m != null && m.blocks(effect)) cir.setReturnValue(null);
    }

    /** Particulas que llegan ya creadas: las de romper bloques van por aqui. */
    @Inject(method = "add(Lnet/minecraft/client/particle/Particle;)V", at = @At("HEAD"), cancellable = true)
    private void nova$noParticleObjects(Particle particle, CallbackInfo ci) {
        Visual.NoParticles m = nova$module();
        if (m != null && (m.all.value || (m.blocks.value && particle instanceof TerrainParticle))) ci.cancel();
    }

    /** Con "Quitar todo": borra las que ya habia (por ejemplo, de otro mod) en cada tick. */
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void nova$clearAll(CallbackInfo ci) {
        Visual.NoParticles m = nova$module();
        if (m != null && m.all.value) { clearParticles(); ci.cancel(); }
    }

    /** ...y no dibuja ninguna, venga de donde venga (1.21.9+: las particulas se mandan a dibujar en addToBatch). */
    @Inject(method = "extract", at = @At("HEAD"), cancellable = true)
    private void nova$renderNone(CallbackInfo ci) {
        Visual.NoParticles m = nova$module();
        if (m != null && m.all.value) ci.cancel();
    }
}
