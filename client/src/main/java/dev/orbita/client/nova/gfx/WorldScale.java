package dev.orbita.client.nova.gfx;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.orbita.client.nova.NovaClient;
import dev.orbita.client.nova.module.FpsBoost;
import net.minecraft.client.Minecraft;

/**
 * Resolucion del mundo (1.21.5+): el mundo se dibuja en una imagen mas chica y luego se estira a la pantalla.
 * La grafica pinta muchos menos pixeles (al 70 % son la mitad), y el HUD, los menus y el chat siguen nitidos.
 */
public final class WorldScale {
    private WorldScale() { }

    private static final boolean NOSCALE = Boolean.getBoolean("nova.noscale"); // solo para pruebas de FPS
    private static RenderTarget fb;
    private static boolean active;

    public static float scale() {
        if (NovaClient.modules == null || NOSCALE) return 1f;
        FpsBoost b = NovaClient.modules.get(FpsBoost.class);
        if (b == null || !b.enabled) return 1f;
        float s = b.resolution.f();
        // modo Extra con la opcion beta: fuerza 80 % (en vivo, sin pisar la barra manual)
        if (dev.orbita.client.nova.module.ClientSettings.extra() && dev.orbita.client.nova.module.ClientSettings.betaBoost()) s = Math.min(s, 0.80f);
        return s;
    }

    /** Lo usa el mixin de MinecraftClient.getFramebuffer(): mientras se dibuja el mundo, la imagen chica. */
    public static RenderTarget current() { return active ? fb : null; }

    public static void begin() {
        float s = scale();
        if (s >= 0.999f) return;
        var win = Minecraft.getInstance().getWindow();
        int w = Math.max(16, Math.round(win.getWidth() * s)), h = Math.max(16, Math.round(win.getHeight() * s));
        if (fb == null) fb = new TextureTarget("nova_world_scale", w, h, true);
        else if (fb.width != w || fb.height != h) fb.resize(w, h);
        // 1.21.11: el filtrado lo pone el dibujo, no la imagen
        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(fb.getColorTexture(), 0, fb.getDepthTexture(), 1.0);
        active = true;
    }

    public static void end() {
        if (!active) return;
        active = false;
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        fb.blitAndBlendToTexture(main.getColorTextureView()); // estira la imagen del mundo a la pantalla
    }
}
