package dev.orbita.client.nova.module.cosmetic;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.orbita.client.nova.core.Setting;
import dev.orbita.client.nova.gfx.Mesh;
import org.joml.Vector3f;

/** Corona de oro con puntas, bolitas y gemas que brillan; va puesta en la cabeza. */
public final class Crown extends ModelCosmetic {
    public final Setting.Col color = add(new Setting.Col("Color", 0.13f, 0.8f));
    public final Setting.Col gem = add(new Setting.Col("Gems", 0.95f, 0.55f));
    public final Setting.Num height = add(new Setting.Num("Height", 0.14, 0.08, 0.3, 0.01));

    public Crown() { super("Crown", "Shiny crown"); }

    @Override
    public void render(Ctx c) {
        var ms = c.ms();
        c.model().head.translateAndRotate(ms);
        var e = ms.last();
        int gold = color.rgb(), dark = Mesh.shade(gold, 0.7f), light = Mesh.mix(gold, 0xFFFFFF, 0.35f);
        float r = 0.285f, y0 = -0.47f, y1 = -0.56f, h = height.f();
        int n = 8;
        VertexConsumer vc = c.lit();
        Mesh.cylinder(vc, e, y0, y1, r, r, n, Mesh.argb(dark, 255), Mesh.argb(gold, 255), c.light(), false, false);
        Mesh.cylinder(vc, e, y0, y1, r - 0.02f, r - 0.02f, n, Mesh.argb(dark, 255), Mesh.argb(dark, 255), c.light(), false, false);
        Mesh.ring(vc, e, y1, r - 0.02f, r, n, Mesh.argb(light, 255), Mesh.argb(light, 255), c.light());
        for (int i = 0; i < n; i++) { // puntas en cada esquina del octogono
            double a = (i + 0.5) * Math.PI * 2 / n, half = Math.PI / n * 0.85;
            float rr = r * (float) Math.cos(Math.PI / n);
            Vector3f b0 = new Vector3f((float) Math.cos(a - half) * r, y1, (float) Math.sin(a - half) * r);
            Vector3f b1 = new Vector3f((float) Math.cos(a + half) * r, y1, (float) Math.sin(a + half) * r);
            Vector3f tip = new Vector3f((float) Math.cos(a) * rr, y1 - h, (float) Math.sin(a) * rr);
            Mesh.tri(vc, e, b0, b1, tip, Mesh.argb(gold, 255), Mesh.argb(gold, 255), Mesh.argb(light, 255), c.light(), true);
            float s = 0.018f;
            Mesh.box(vc, e, tip.x - s, tip.y - s * 2, tip.z - s, tip.x + s, tip.y, tip.z + s, Mesh.argb(light, 255), c.light());
        }
        // gemas: una en cada cara, a plena luz
        VertexConsumer em = c.emissive();
        int g = gem.rgb();
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n;
            float rr = r * (float) Math.cos(Math.PI / n) + 0.004f, gy = (y0 + y1) / 2, s = 0.022f;
            float cx = (float) Math.cos(a) * rr, cz = (float) Math.sin(a) * rr, tx = -(float) Math.sin(a) * s, tz = (float) Math.cos(a) * s;
            Mesh.quad(em, e, new Vector3f(cx - tx, gy, cz - tz), new Vector3f(cx, gy - s * 1.4f, cz), new Vector3f(cx + tx, gy, cz + tz), new Vector3f(cx, gy + s * 1.4f, cz),
                    Mesh.argb(i % 2 == 0 ? g : Mesh.mix(g, 0x40C0FF, 0.6f), 255), c.fullLight(), true);
        }
    }
}
