package dev.orbita.client.nova.mixin;

import dev.orbita.client.nova.module.PvpExtras;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Oculta los nombres normales de los jugadores (Nombres de jugadores -> ocultar). Aparte porque cambia en cada version. */
@Mixin(EntityRenderer.class)
public abstract class LabelMixin {
    @Inject(method = "shouldShowName", at = @At("HEAD"), cancellable = true)
    private void nova$hide(Entity entity, double dist, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof Player && PvpExtras.Nametags.hideVanilla()) cir.setReturnValue(false);
    }
}
