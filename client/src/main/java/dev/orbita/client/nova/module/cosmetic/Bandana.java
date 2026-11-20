package dev.orbita.client.nova.module.cosmetic;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.orbita.client.nova.core.Setting;
import dev.orbita.client.nova.gfx.Mesh;
import dev.orbita.client.nova.gfx.Tex;
import org.joml.Vector3f;

/** Pañuelo atado a la frente, con lunares; las puntas del nudo ondean al moverte. */
public final class Bandana extends ModelCosmetic {
    public final Setting.Col color = add(new Setting.Col("Color", 0.0f, 0.55f));

    public Bandana() { super("Bandana", "Headband with a knot that waves"); }

    @Override
    public void render(Ctx c) {
        var ms = c.ms();
        c.model().head.translateAndRotate(ms);
        var e = ms.last();
        int rgb = color.rgb(), col = Mesh.argb(rgb, 255);
        float h = 0.268f, y0 = -0.44f, y1 = -0.31f;
        VertexConsumer vc = c.textured(Tex.BANDANA);
        Vector3f[] corners = {new Vector3f(-h, 0, -h), new Vector3f(h, 0, -h), new Vector3f(h, 0, h), new Vector3f(-h, 0, h)};
        for (int i = 0; i < 4; i++) {
            Vector3f a = corners[i], b = corners[(i + 1) % 4];
            float u0 = i * 0.25f, u1 = u0 + 0.25f;
            Mesh.quad(vc, e, new Vector3f(a.x, y1, a.z), new Vector3f(a.x, y0, a.z), new Vector3f(b.x, y0, b.z), new Vector3f(b.x, y1, b.z),
                    col, col, col, col, new float[]{u0, 1, u0, 0, u1, 0, u1, 1}, c.light(), true);
        }
        // nudo detras y dos puntas que cuelgan
        float kz = h + 0.03f, ky = (y0 + y1) / 2;
        Mesh.box(vc, e, -0.05f, ky - 0.05f, h, 0.05f, ky + 0.05f, kz + 0.02f, col, c.light());
        float sway = (float) Math.sin(c.time() * 0.25) * 0.04f + Math.min(0.25f, c.speed() * 1.5f);
        for (int side = -1; side <= 1; side += 2) {
            Vector3f top = new Vector3f(side * 0.02f, ky, kz + 0.01f);
            Vector3f end = new Vector3f(side * (0.1f + sway * 0.3f), ky + 0.28f - sway * 0.6f, kz + 0.06f + sway);
            Vector3f w = new Vector3f(0.045f, 0, 0);
            Mesh.quad(vc, e, new Vector3f(top).sub(w), new Vector3f(top).add(w), new Vector3f(end).add(w), new Vector3f(end).sub(w),
                    col, col, Mesh.argb(Mesh.shade(rgb, 0.8f), 255), Mesh.argb(Mesh.shade(rgb, 0.8f), 255), new float[]{0, 0, 0.1f, 0, 0.1f, 1, 0, 1}, c.light(), true);
        }
    }
}
