package dev.orbita.client.nova.mixin;

import com.mojang.blaze3d.platform.FramerateLimitTracker;
import dev.orbita.client.nova.module.UnlockFps;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FramerateLimitTracker.class)
public class FpsLimiterMixin {
    /** En menus Minecraft limita a 60 FPS: con "Desbloquear FPS" se usa el maximo elegido (salvo minimizado o ausente). */
    @Inject(method = "getFramerateLimit", at = @At("RETURN"), cancellable = true)
    private void nova$unlock(CallbackInfoReturnable<Integer> cir) {
        int v = cir.getReturnValueI();
        if (v == 60 && UnlockFps.on()) cir.setReturnValue(Minecraft.getInstance().options.framerateLimit().get());
    }
}
