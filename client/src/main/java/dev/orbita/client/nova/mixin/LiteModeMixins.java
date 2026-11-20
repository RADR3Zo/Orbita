package dev.orbita.client.nova.mixin;

import dev.orbita.client.nova.module.LiteMode;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Ganchos del Modo ligero (cada uno en su clase de Minecraft). */
public final class LiteModeMixins {
    private LiteModeMixins() { }

    @Mixin(TextureAtlas.class)
    public static class Atlas {
        /** Agua, lava, fuego, portales... quietos: no se vuelve a subir la textura en cada tick. */
        @Inject(method = "cycleAnimationFrames", at = @At("HEAD"), cancellable = true)
        private void nova$stop(CallbackInfo ci) { if (LiteMode.stopAnimated()) ci.cancel(); }
    }

    @Mixin(ItemStack.class)
    public static class Glint {
        @Inject(method = "hasFoil", at = @At("RETURN"), cancellable = true)
        private void nova$noGlint(CallbackInfoReturnable<Boolean> cir) { if (cir.getReturnValueZ() && LiteMode.noGlint()) cir.setReturnValue(false); }
    }

    @Mixin(Gui.class)
    public static class Vignette {
        @Inject(method = "renderVignette", at = @At("HEAD"), cancellable = true)
        private void nova$noVignette(GuiGraphics ctx, Entity entity, CallbackInfo ci) { if (LiteMode.noVignette()) ci.cancel(); }
    }

    @Mixin(LevelRenderer.class)
    public static class Sky {
        @Inject(method = "addSkyPass", at = @At("HEAD"), cancellable = true)
        private void nova$noSky(CallbackInfo ci) { if (LiteMode.noSky()) ci.cancel(); } // sin argumentos: sirve en todas las versiones
    }
}
