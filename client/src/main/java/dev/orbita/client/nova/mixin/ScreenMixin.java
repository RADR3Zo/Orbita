package dev.orbita.client.nova.mixin;

import dev.orbita.client.nova.module.VideoBackground;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class ScreenMixin {
    /** Pantallas fuera del mundo (opciones, multijugador...): video sobre el desenfoque normal. */
    @Inject(method = "renderBackground", at = @At("TAIL"))
    private void nova$video(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        VideoBackground.drawForScreen((Screen) (Object) this, context);
    }

    /** Dentro del mundo el inventario y los cofres usan este fondo (no el de arriba). */
    @Inject(method = "renderTransparentBackground", at = @At("TAIL"))
    private void nova$videoInGame(GuiGraphics context, CallbackInfo ci) {
        VideoBackground.drawForScreen((Screen) (Object) this, context);
    }
}
