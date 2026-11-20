package dev.orbita.client.mixin;

import dev.orbita.client.feature.FreelookFeature;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Camera.class)
public abstract class CameraMixin {

    /** Con freelook activo, la cámara usa su propia rotación en lugar de la del jugador. */
    @ModifyArg(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V"), index = 0)
    private float orbita$freelookYaw(float yaw) {
        return FreelookFeature.isActive() ? FreelookFeature.yaw() : yaw;
    }

    @ModifyArg(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V"), index = 1)
    private float orbita$freelookPitch(float pitch) {
        return FreelookFeature.isActive() ? FreelookFeature.pitch() : pitch;
    }
}
