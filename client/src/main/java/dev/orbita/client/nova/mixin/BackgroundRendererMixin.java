package dev.orbita.client.nova.mixin;

import dev.orbita.client.nova.module.SkyModule;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 1.21.6+: el color de la niebla lo calcula FogRenderer. Con el cielo personalizado toma su color. */
@Mixin(FogRenderer.class)
public class BackgroundRendererMixin {
    @Inject(method = "computeFogColor", at = @At("RETURN"))
    private void nova$skyFog(Camera camera, float tickDelta, ClientLevel world, int viewDistance, float skyDarkness,
                             CallbackInfoReturnable<Vector4f> cir) {
        int rgb = SkyModule.fogColor(camera, world);
        if (rgb < 0) return;
        Vector4f v = cir.getReturnValue();
        v.set(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f, v.w);
    }
}
