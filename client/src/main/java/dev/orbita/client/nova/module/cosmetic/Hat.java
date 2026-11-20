package dev.orbita.client.nova.module.cosmetic;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.orbita.client.nova.core.Setting;
import dev.orbita.client.nova.gfx.Mesh;
import org.joml.Vector3f;

/** Sombreros: chistera, mago y fiesta. Siguen la cabeza y reciben la luz del mundo. */
public final class Hat extends ModelCosmetic {
    public final Setting.Choice style = add(new Setting.Choice("Style", 0, "Top hat", "Wizard", "Party", "China hat"));
    public final Setting.Col color = add(new Setting.Col("Color", 0.75f, 0.45f));

    public Hat() { super("Hat", "Top hat, wizard or party hat"); }
    @Override public String info() { return dev.orbita.client.nova.core.Lang.t(style.get()); }

    @Override
    public void render(Ctx c) {
        var ms = c.ms();
        c.model().head.translateAndRotate(ms);
        ms.translate(0, -0.5f, 0);
        int rgb = color.rgb();
        switch (style.index) {
            case 1 -> wizard(c, rgb);
            case 2 -> party(c, rgb);
            case 3 -> china(c, rgb);
            default -> topHat(c, rgb);
        }
    }

    private void topHat(Ctx c, int rgb) {
        var ms = c.ms();
        ms.mulPose(Axis.ZP.rotationDegrees(-6));
        var e = ms.last();
        int body = Mesh.shade(rgb, 0.22f), band = rgb;
        VertexConsumer vc = c.lit();
        Mesh.cylinder(vc, e, 0.01f, -0.015f, 0.38f, 0.38f, 28, Mesh.argb(body, 255), Mesh.argb(body, 255), c.light(), true, true);
        Mesh.cylinder(vc, e, -0.015f, -0.37f, 0.22f, 0.235f, 28, Mesh.argb(body, 255), Mesh.argb(Mesh.shade(body, 1.4f), 255), c.light(), false, true);
        Mesh.cylinder(vc, e, -0.015f, -0.085f, 0.226f, 0.228f, 28, Mesh.argb(band, 255), Mesh.argb(band, 255), c.light(), false, false);
    }

    private void wizard(Ctx c, int rgb) {
        var e = c.ms().last();
        int dark = Mesh.shade(rgb, 0.6f);
        VertexConsumer vc = c.lit();
        Mesh.cylinder(vc, e, 0.01f, -0.01f, 0.44f, 0.44f, 28, Mesh.argb(dark, 255), Mesh.argb(dark, 255), c.light(), true, true);
        // cono doblado: varios tramos que se van inclinando hacia atras
        int steps = 7;
        float y = -0.01f, r = 0.3f, bx = 0, bz = 0;
        for (int i = 0; i < steps; i++) {
            float k = (i + 1) / (float) steps, nr = 0.3f * (1 - k), ny = y - 0.11f, nbx = 0.0f, nbz = 0.03f * k * k * 6;
            for (int s = 0; s < 20; s++) {
                double a0 = s * Math.PI * 2 / 20, a1 = (s + 1) * Math.PI * 2 / 20;
                Vector3f p0 = new Vector3f(bx + (float) Math.cos(a0) * r, y, bz + (float) Math.sin(a0) * r), p1 = new Vector3f(bx + (float) Math.cos(a1) * r, y, bz + (float) Math.sin(a1) * r);
                Vector3f q0 = new Vector3f(nbx + (float) Math.cos(a0) * nr, ny, nbz + (float) Math.sin(a0) * nr), q1 = new Vector3f(nbx + (float) Math.cos(a1) * nr, ny, nbz + (float) Math.sin(a1) * nr);
                Mesh.quad(vc, e, p0, q0, q1, p1, Mesh.argb(Mesh.shade(rgb, 0.85f + 0.3f * (i / (float) steps)), 255), c.light(), true);
            }
            y = ny; r = nr; bx = nbx; bz = nbz;
        }
        // estrellitas que brillan
        VertexConsumer em = c.emissive();
        float[][] st = {{0.2f, -0.12f, -0.17f}, {-0.16f, -0.25f, -0.1f}, {0.05f, -0.42f, -0.1f}};
        for (float[] s : st) {
            float z = s[2] - 0.012f, q = 0.03f;
            Mesh.quad(em, e, new Vector3f(s[0] - q, s[1], z), new Vector3f(s[0], s[1] - q * 1.6f, z), new Vector3f(s[0] + q, s[1], z), new Vector3f(s[0], s[1] + q * 1.6f, z), Mesh.argb(0xFFF2A0, 255), c.fullLight(), true);
        }
    }

    /** Sombrero chino: cono ancho y translucido con degradado y borde brillante. */
    private void china(Ctx c, int rgb) {
        var e = c.ms().last();
        int rgb2 = Mesh.mix(rgb, 0xFFFFFF, 0.5f);
        Mesh.cylinder(c.lit(), e, 0.02f, -0.22f, 0.62f, 0.0f, 32, Mesh.argb(rgb, 150), Mesh.argb(rgb2, 220), c.light(), false, false);
        var g = c.glow();
        Mesh.ring(g, e, 0.02f, 0.58f, 0.64f, 32, Mesh.argb(rgb, 200), Mesh.argb(rgb, 40), c.fullLight());
    }

    private void party(Ctx c, int rgb) {
        var ms = c.ms();
        ms.translate(0.06f, 0, 0);
        ms.mulPose(Axis.ZP.rotationDegrees(-12));
        var e = ms.last();
        VertexConsumer vc = c.lit();
        int bands = 5;
        int other = Mesh.mix(rgb, 0xFFFFFF, 0.75f);
        for (int i = 0; i < bands; i++) {
            float k0 = i / (float) bands, k1 = (i + 1) / (float) bands;
            int col = i % 2 == 0 ? rgb : other;
            Mesh.cylinder(vc, e, -0.42f * k0, -0.42f * k1, 0.17f * (1 - k0), 0.17f * (1 - k1), 20, Mesh.argb(col, 255), Mesh.argb(col, 255), c.light(), i == 0, false);
        }
        float s = 0.035f;
        Mesh.box(c.emissive(), e, -s, -0.42f - s * 2, -s, s, -0.42f, s, Mesh.argb(0xFFE070, 255), c.fullLight());
    }
}
