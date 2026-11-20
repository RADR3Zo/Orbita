package dev.orbita.client.nova.module.cosmetic;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.orbita.client.nova.core.Setting;
import dev.orbita.client.nova.gfx.Mesh;
import org.joml.Vector3f;

/** Aro de luz sobre la cabeza: sigue la cabeza, flota, gira y suelta destellos. */
public final class Halo extends ModelCosmetic {
    public final Setting.Col color = add(new Setting.Col("Color", 0.13f, 0.8f));
    public final Setting.Num radius = add(new Setting.Num("Radius", 0.24, 0.16, 0.4, 0.01));
    public final Setting.Num height = add(new Setting.Num("Height", 0.2, 0.08, 0.5, 0.01));
    public final Setting.Bool sparkles = add(new Setting.Bool("Sparkles", true));

    public Halo() { super("Halo", "Aro sobre la cabeza"); }

    @Override
    public void render(Ctx c) {
        var ms = c.ms();
        float t = c.time();
        c.model().head.translateAndRotate(ms);
        ms.translate(0, -0.5f - height.f() + (float) Math.sin(t * 0.08) * 0.015f, 0);
        ms.mulPose(Axis.XP.rotationDegrees(8));
        ms.mulPose(Axis.YP.rotationDegrees(t * 1.5f));
        var e = ms.last();
        int rgb = color.rgb(), core = Mesh.mix(rgb, 0xFFFFFF, 0.55f);
        float r = radius.f();
        // nucleo brillante con algo de grosor (dos anillos y la pared interior/exterior)
        VertexConsumer em = c.emissive();
        Mesh.ring(em, e, 0, r - 0.025f, r + 0.025f, 40, Mesh.argb(core, 255), Mesh.argb(rgb, 255), c.fullLight());
        Mesh.cylinder(em, e, -0.012f, 0.012f, r + 0.025f, r + 0.025f, 40, Mesh.argb(rgb, 255), Mesh.argb(rgb, 255), c.fullLight(), false, false);
        Mesh.cylinder(em, e, -0.012f, 0.012f, r - 0.025f, r - 0.025f, 40, Mesh.argb(core, 255), Mesh.argb(core, 255), c.fullLight(), false, false);
        // resplandor suave alrededor
        VertexConsumer g = c.glow();
        Mesh.ring(g, e, 0, r - 0.09f, r, 40, Mesh.argb(rgb, 0), Mesh.argb(rgb, 110), c.fullLight());
        Mesh.ring(g, e, 0, r, r + 0.09f, 40, Mesh.argb(rgb, 110), Mesh.argb(rgb, 0), c.fullLight());
        if (sparkles.value) {
            for (int i = 0; i < 6; i++) {
                double a = i * Math.PI / 3 + t * 0.03;
                float x = (float) Math.cos(a) * r, z = (float) Math.sin(a) * r;
                float s = 0.02f + 0.015f * (float) Math.max(0, Math.sin(t * 0.2 + i * 1.7));
                Mesh.quad(g, e, new Vector3f(x - s, 0, z), new Vector3f(x, -s * 1.6f, z), new Vector3f(x + s, 0, z), new Vector3f(x, s * 1.6f, z), Mesh.argb(0xFFFFFF, 220), c.fullLight(), true);
            }
        }
    }
}
