package dev.orbita.client.mixin;

import dev.orbita.client.feature.ZoomFeature;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    /** Aplica el zoom solo al FOV del mundo (useFovSetting), no al de la mano. */
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void orbita$applyZoom(Camera camera, float partialTick, boolean useFovSetting, CallbackInfoReturnable<Float> cir) {
        if (!useFovSetting) {
            return;
        }
        float multiplier = ZoomFeature.fovMultiplier();
        if (multiplier != 1f) {
            cir.setReturnValue(cir.getReturnValueF() * multiplier);
        }
    }
}
