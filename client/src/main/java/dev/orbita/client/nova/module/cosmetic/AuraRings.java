package dev.orbita.client.nova.module.cosmetic;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.orbita.client.nova.core.Setting;
import dev.orbita.client.nova.gfx.Gfx;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix4f;

/** Anillos de luz que suben alrededor del cuerpo (aura). */
public final class AuraRings extends Cosmetic {
    public final Setting.Col color = add(new Setting.Col("Color", 0.55f, 0.7f));
    public final Setting.Num count = add(new Setting.Num("Count", 4, 2, 8, 1));
    public final Setting.Num speed = add(new Setting.Num("Speed", 1.0, 0.3, 3.0, 0.1));
    public final Setting.Num radius = add(new Setting.Num("Radius", 0.55, 0.3, 1.2, 0.05));

    public AuraRings() { super("Aura Rings", "Rising rings of light"); }

    @Override boolean rotateWithBody() { return false; }

    @Override
    void draw(PoseStack ms, Player p, float time, float delta, dev.orbita.client.nova.gfx.WCtx ctx) {
        Matrix4f m = ms.last().pose();
        int rgb = color.rgb(), n = count.i();
        float h = p.getBbHeight() + 0.2f;
        for (int i = 0; i < n; i++) {
            float f = ((time * 0.012f * speed.f()) + i / (float) n) % 1f;
            float fade = (float) Math.sin(f * Math.PI);          // aparece y se desvanece
            float r = radius.f() * (0.85f + 0.3f * fade);
            Gfx.ringXZ(m, f * h, r, 0.022f, 36, Gfx.argb(rgb, (int) (210 * fade)));
            Gfx.ringXZ(m, f * h, r, 0.06f, 36, Gfx.argb(rgb, (int) (55 * fade)));
        }
    }
}
