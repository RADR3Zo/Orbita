package dev.orbita.client.nova.module.cosmetic;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.orbita.client.nova.core.Setting;
import dev.orbita.client.nova.gfx.Gfx;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Monedas/orbes que giran alrededor del jugador (siempre miran a la camara). */
public final class Coins extends Cosmetic {
    private static final int[] PALETTE = {0xFFB020, 0x5C8DFF, 0xC9D2E0, 0x35E0C0, 0xB06BFF};

    public final Setting.Num count = add(new Setting.Num("Count", 5, 1, 10, 1));
    public final Setting.Num radius = add(new Setting.Num("Radius", 1.1, 0.6, 2.0, 0.1));
    public final Setting.Num speed = add(new Setting.Num("Speed", 1.0, 0.2, 3.0, 0.1));
    public final Setting.Num size = add(new Setting.Num("Size", 0.16, 0.08, 0.35, 0.01));
    public final Setting.Bool mono = add(new Setting.Bool("Single color", false));
    public final Setting.Col color = add(new Setting.Col("Color", 0.55f, 0.7f)).showIf(() -> mono.value);

    public Coins() { super("Orbiting Coins", "Monedas que giran a tu alrededor"); }

    @Override boolean rotateWithBody() { return false; }

    @Override
    void draw(PoseStack ms, Player p, float time, float delta, dev.orbita.client.nova.gfx.WCtx ctx) {
        int n = count.i();
        float r = radius.f(), sz = size.f();
        for (int i = 0; i < n; i++) {
            double a = time * 0.05 * speed.value + i * Math.PI * 2 / n;
            float x = (float) Math.cos(a) * r, z = (float) Math.sin(a) * r;
            float y = 1.0f + (float) Math.sin(time * 0.07 + i * 1.7) * 0.25f;
            int rgb = mono.value ? color.rgb() : PALETTE[i % PALETTE.length];

            ms.pushPose();
            ms.translate(x, y, z);
            ms.mulPose(ctx.camera().rotation()); // billboard
            disc(ms.last().pose(), sz, rgb);
            ms.popPose();
        }
    }

    private static void disc(Matrix4f m, float r, int rgb) {
        int seg = 18;
        int fill = Gfx.argb(rgb, 150), edge = Gfx.argb(rgb, 255), center = Gfx.argb(0xFFFFFF, 110);
        for (int i = 0; i < seg; i++) {
            double a0 = i * Math.PI * 2 / seg, a1 = (i + 1) * Math.PI * 2 / seg;
            float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
            Vector3f o = new Vector3f();
            // relleno (triangulo del centro al borde como quad degenerado)
            Gfx.quad(m, o, center, new Vector3f(c0 * r * 0.8f, s0 * r * 0.8f, 0), fill, new Vector3f(c1 * r * 0.8f, s1 * r * 0.8f, 0), fill, o, center);
            // anillo exterior
            Gfx.quad(m, new Vector3f(c0 * r * 0.8f, s0 * r * 0.8f, 0), edge, new Vector3f(c0 * r, s0 * r, 0), edge,
                    new Vector3f(c1 * r, s1 * r, 0), edge, new Vector3f(c1 * r * 0.8f, s1 * r * 0.8f, 0), edge);
        }
    }
}
