package dev.orbita.client.nova.mixin;

import dev.orbita.client.nova.module.PvpExtras;
import net.minecraft.util.StringDecomposer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Ganchos de los extras de PvP: nombre falso en todo el texto y ocultar los nombres normales. */
public final class PvpMixins {
    private PvpMixins() { }

    /** Todo el texto del juego (chat, tab, nombres, marcador...) pasa por aqui antes de dibujarse. */
    @Mixin(StringDecomposer.class)
    public abstract static class Text {
        @ModifyVariable(method = "iterateFormatted(Ljava/lang/String;ILnet/minecraft/network/chat/Style;Lnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z", at = @At("HEAD"), argsOnly = true, ordinal = 0)
        private static String nova$formatted(String text) { return PvpExtras.NameChanger.replace(text); }

        @ModifyVariable(method = "iterate", at = @At("HEAD"), argsOnly = true, ordinal = 0)
        private static String nova$forwards(String text) { return PvpExtras.NameChanger.replace(text); }

        @ModifyVariable(method = "iterateBackwards", at = @At("HEAD"), argsOnly = true, ordinal = 0)
        private static String nova$backwards(String text) { return PvpExtras.NameChanger.replace(text); }
    }
}
