package dev.orbita.client.nova.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import dev.orbita.client.nova.gfx.WorldScale;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Resolucion del mundo (FPS Boost): ver gfx/WorldScale. */
public final class WorldScaleMixins {
    private WorldScaleMixins() { }

    @Mixin(GameRenderer.class)
    public abstract static class World {
        @Inject(method = "renderLevel", at = @At("HEAD"))
        private void nova$begin(DeltaTracker t, CallbackInfo ci) { WorldScale.begin(); }

        @Inject(method = "renderLevel", at = @At("RETURN"))
        private void nova$end(DeltaTracker t, CallbackInfo ci) { WorldScale.end(); }
    }

    @Mixin(Minecraft.class)
    public abstract static class Target {
        @Inject(method = "getMainRenderTarget", at = @At("HEAD"), cancellable = true)
        private void nova$target(CallbackInfoReturnable<RenderTarget> cir) {
            RenderTarget f = WorldScale.current();
            if (f != null) cir.setReturnValue(f);
        }
    }
}
