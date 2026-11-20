package dev.orbita.client.nova.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import dev.orbita.client.nova.module.LiteMode;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpriteContents.class)
public class SpriteContentsMixin {
    @Shadow @Final private NativeImage originalImage;

    /** Texturas planas: cada bloque toma su color medio (respeta las partes transparentes). Menos ruido y mas claro. */
    @Inject(method = "<init>(Lnet/minecraft/resources/Identifier;Lnet/minecraft/client/resources/metadata/animation/FrameSize;Lcom/mojang/blaze3d/platform/NativeImage;Ljava/util/Optional;Ljava/util/List;Ljava/util/Optional;)V", at = @At("TAIL"))
    private void nova$flat(Identifier id, FrameSize dims, NativeImage img, java.util.Optional<?> anim, java.util.List<?> extra, java.util.Optional<?> tex, CallbackInfo ci) {
        String p = id.getPath();
        if (!p.startsWith("block/") || p.contains("destroy_stage") || p.contains("_ore") || !LiteMode.flatTextures()) return;
        long r = 0, g = 0, b = 0, n = 0;
        int w = originalImage.getWidth(), h = originalImage.getHeight();
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int c = dev.orbita.client.nova.compat.V.getArgb(originalImage, x, y);
            if ((c >>> 24) < 16) continue;
            r += (c >> 16) & 255; g += (c >> 8) & 255; b += c & 255; n++;
        }
        if (n == 0) return;
        int avg = (int) (r / n) << 16 | (int) (g / n) << 8 | (int) (b / n);
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int a = dev.orbita.client.nova.compat.V.getArgb(originalImage, x, y) >>> 24;
            if (a > 0) dev.orbita.client.nova.compat.V.setArgb(originalImage, x, y, a << 24 | avg);
        }
    }
}
