package dev.orbita.client.nova.mixin;

import dev.orbita.client.nova.module.cosmetic.Cape;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public class AbstractClientPlayerEntityMixin {
    /** Pone la capa de Nova (y la misma para la elytra) al jugador local. */
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void nova$cape(CallbackInfoReturnable<PlayerSkin> cir) {
        Identifier cape = Cape.textureFor((AbstractClientPlayer) (Object) this);
        if (cape == null) return;
        PlayerSkin s = cir.getReturnValue();
        var asset = new net.minecraft.core.ClientAsset.ResourceTexture(cape, cape);
        cir.setReturnValue(new PlayerSkin(s.body(), asset, asset, s.model(), s.secure()));
    }
}
