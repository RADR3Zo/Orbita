package dev.orbita.client.mixin;

import dev.orbita.client.feature.FreelookFeature;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {

    /** Con freelook activo, el ratón mueve la cámara y no al jugador. */
    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void orbita$freelookTurn(double yRot, double xRot, CallbackInfo ci) {
        if (FreelookFeature.isActive() && (Object) this == Minecraft.getInstance().player) {
            FreelookFeature.turn(yRot, xRot);
            ci.cancel();
        }
    }
}
