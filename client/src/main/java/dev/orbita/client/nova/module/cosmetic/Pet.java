package dev.orbita.client.nova.module.cosmetic;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.orbita.client.nova.core.Setting;
import dev.orbita.client.nova.gfx.Mesh;
import org.joml.Vector3f;

/** Mascota flotante al lado del hombro: gema, dona, cubo o estrella que gira y brilla. */
public final class Pet extends ModelCosmetic {
    public final Setting.Choice style = add(new Setting.Choice("Style", 0, "Gem", "Donut", "Cube", "Star"));
    public final Setting.Col color = add(new Setting.Col("Color", 0.5f, 0.65f));
    public final Setting.Num size = add(new Setting.Num("Size", 1.0, 0.5, 2.0, 0.1));

    public Pet() { super("Pet", "Floating gem, donut or star next to you"); }

    @Override public String info() { return dev.orbita.client.nova.core.Lang.t(style.get()); }

    @Override
    public void render(Ctx c) {
        var ms = c.ms();
        float t = c.time();
        c.model().body.translateAndRotate(ms);
        ms.translate(0.55f, -0.25f + (float) Math.sin(t * 0.09) * 0.08f, 0.15f);
        ms.mulPose(Axis.YP.rotationDegrees(t * 3));
        ms.mulPose(Axis.XP.rotationDegrees((float) Math.sin(t * 0.05) * 15));
        float s = 0.13f * size.f();
        ms.scale(s, s, s);
        var e = ms.last();
        int rgb = color.rgb(), light = Mesh.mix(rgb, 0xFFFFFF, 0.45f), dark = Mesh.shade(rgb, 0.6f);
        VertexConsumer vc = c.lit();
        switch (style.index) {
            case 1 -> { // dona: anillo grueso hecho de tubos
                int segs = 20, tube = 8;
                for (int i = 0; i < segs; i++) for (int j = 0; j < tube; j++) {
                    Vector3f a = torus(i, j, segs, tube), b = torus(i + 1, j, segs, tube), cc = torus(i + 1, j + 1, segs, tube), d = torus(i, j + 1, segs, tube);
                    int col = j < tube / 2 ? Mesh.argb(light, 255) : Mesh.argb(Mesh.mix(rgb, 0xD08040, 0.6f), 255);
                    Mesh.quad(vc, e, a, b, cc, d, col, c.light(), false);
                }
            }
            case 2 -> Mesh.box(vc, e, -0.7f, -0.7f, -0.7f, 0.7f, 0.7f, 0.7f, Mesh.argb(rgb, 255), c.light());
            case 3 -> { // estrella plana con grosor
                for (int i = 0; i < 10; i++) {
                    double a0 = i * Math.PI / 5, a1 = (i + 1) * Math.PI / 5;
                    float r0 = i % 2 == 0 ? 1.1f : 0.45f, r1 = i % 2 == 0 ? 0.45f : 1.1f;
                    Vector3f p0 = new Vector3f((float) Math.sin(a0) * r0, -(float) Math.cos(a0) * r0, 0), p1 = new Vector3f((float) Math.sin(a1) * r1, -(float) Math.cos(a1) * r1, 0);
                    Mesh.tri(vc, e, new Vector3f(0, 0, 0.25f), p0, p1, Mesh.argb(light, 255), Mesh.argb(rgb, 255), Mesh.argb(rgb, 255), c.light(), true);
                    Mesh.tri(vc, e, new Vector3f(0, 0, -0.25f), p1, p0, Mesh.argb(light, 255), Mesh.argb(rgb, 255), Mesh.argb(rgb, 255), c.light(), true);
                }
            }
            default -> { // gema: octaedro alargado
                Vector3f top = new Vector3f(0, -1.3f, 0), bot = new Vector3f(0, 1.3f, 0);
                Vector3f[] mid = {new Vector3f(0.75f, 0, 0), new Vector3f(0, 0, 0.75f), new Vector3f(-0.75f, 0, 0), new Vector3f(0, 0, -0.75f)};
                for (int i = 0; i < 4; i++) {
                    Mesh.tri(vc, e, top, mid[i], mid[(i + 1) % 4], Mesh.argb(light, 235), Mesh.argb(rgb, 235), Mesh.argb(rgb, 235), c.light(), true);
                    Mesh.tri(vc, e, bot, mid[(i + 1) % 4], mid[i], Mesh.argb(dark, 235), Mesh.argb(rgb, 235), Mesh.argb(rgb, 235), c.light(), true);
                }
            }
        }
        // halo de luz alrededor
        VertexConsumer g = c.glow();
        Mesh.ring(g, e, 0, 0.0f, 1.8f, 20, Mesh.argb(rgb, 70), Mesh.argb(rgb, 0), c.fullLight());
    }

    private static Vector3f torus(int i, int j, int segs, int tube) {
        double u = i * Math.PI * 2 / segs, v = j * Math.PI * 2 / tube;
        float R = 0.8f, r = 0.38f;
        return new Vector3f((float) ((R + r * Math.cos(v)) * Math.cos(u)), (float) (r * Math.sin(v)), (float) ((R + r * Math.cos(v)) * Math.sin(u)));
    }
}
