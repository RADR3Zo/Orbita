package dev.orbita.client.mixin;

import dev.orbita.client.config.OrbitaConfig;
import dev.orbita.client.feature.CpsTracker;
import dev.orbita.client.feature.ZoomFeature;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {

    @Inject(method = "onButton", at = @At("HEAD"))
    private void orbita$countClick(long window, MouseButtonInfo info, int action, CallbackInfo ci) {
        if (action == GLFW.GLFW_PRESS && Minecraft.getInstance().screen == null) {
            CpsTracker.click(info.button());
        }
    }

    /** Mientras se hace zoom, la rueda ajusta el aumento en lugar de cambiar de ranura. */
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void orbita$zoomScroll(long window, double xOffset, double yOffset, CallbackInfo ci) {
        if (ZoomFeature.isZooming() && OrbitaConfig.get().zoomScroll) {
            ZoomFeature.onScroll(yOffset);
            ci.cancel();
        }
    }
}
