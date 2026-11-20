package dev.orbita.client.nova.module.cosmetic;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.orbita.client.nova.core.Setting;
import dev.orbita.client.nova.gfx.Gfx;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix4f;

/** Estrella de 8 puntas con circulo que gira en el suelo bajo tus pies. */
public final class GroundSigil extends Cosmetic {
    public final Setting.Col color = add(new Setting.Col("Color", 0.70f, 0.7f));
    public final Setting.Num size = add(new Setting.Num("Size", 1.0, 0.5, 2.5, 0.1));
    public final Setting.Num speed = add(new Setting.Num("Speed", 1.0, 0.0, 4.0, 0.1));

    public GroundSigil() { super("Ground Sigil", "Rotating symbol on the ground"); }

    @Override boolean rotateWithBody() { return false; }

    @Override
    void draw(PoseStack ms, Player p, float time, float delta, dev.orbita.client.nova.gfx.WCtx ctx) {
        Matrix4f m = ms.last().pose();
        int rgb = color.rgb();
        float r = size.f(), y = 0.03f;
        int line = Gfx.argb(rgb, 235), soft = Gfx.argb(rgb, 60);
        float rot = time * 0.02f * speed.f();
        Gfx.ringXZ(m, y, r * 1.18f, 0.015f, 48, line);
        Gfx.ringXZ(m, y, r * 1.18f, 0.05f, 48, soft);
        for (int sq = 0; sq < 2; sq++) {
            for (int k = 0; k < 4; k++) {
                double a0 = rot + sq * Math.PI / 4 + k * Math.PI / 2, a1 = a0 + Math.PI / 2;
                float ax = (float) Math.cos(a0) * r, az = (float) Math.sin(a0) * r, bx = (float) Math.cos(a1) * r, bz = (float) Math.sin(a1) * r;
                Gfx.segXZ(m, ax, az, bx, bz, y, 0.018f, line);
                Gfx.segXZ(m, ax, az, bx, bz, y, 0.055f, soft);
            }
        }
    }
}
