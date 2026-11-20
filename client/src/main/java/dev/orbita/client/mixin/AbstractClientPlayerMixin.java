package dev.orbita.client.mixin;

import dev.orbita.client.cosmetics.CapeRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {

    /** Sustituye la capa (y los élitros) del jugador local por la elegida en el guardarropa. */
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void orbita$applyCape(CallbackInfoReturnable<PlayerSkin> cir) {
        AbstractClientPlayer self = (AbstractClientPlayer) (Object) this;
        if (self.getUUID().equals(Minecraft.getInstance().getUser().getProfileId())) {
            cir.setReturnValue(CapeRegistry.withCape(cir.getReturnValue()));
        }
    }
}
