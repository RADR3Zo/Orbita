package dev.orbita.client.mixin;

import dev.orbita.client.OrbitaClient;
import dev.orbita.client.ui.screen.OrbitaMainMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    /** Cada vez que el juego abre el TitleScreen vanilla (arranque, desconexión...) abrimos el de Órbita. */
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void orbita$replaceTitleScreen(Screen screen, CallbackInfo ci) {
        if (screen instanceof TitleScreen) {
            ci.cancel();
            ((Minecraft) (Object) this).setScreen(new OrbitaMainMenu());
        }
    }

    @Inject(method = "createTitle", at = @At("HEAD"), cancellable = true)
    private void orbita$windowTitle(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue("Órbita · Minecraft " + OrbitaClient.minecraftVersion());
    }
}
