package dev.orbita.client.nova.module.cosmetic;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.orbita.client.nova.core.Setting;
import dev.orbita.client.nova.gfx.Mesh;
import org.joml.Vector3f;

/**
 * Alas pegadas a la espalda, con tres estilos: Angel (plumas), Dragon (huesos y membrana) y Mariposa.
 * Se pliegan al agacharte, respiran quietas en el suelo y aletean fuerte en el aire.
 */
public final class Wings extends ModelCosmetic {
    public final Setting.Choice style = add(new Setting.Choice("Style", 0, "Angel", "Dragon", "Butterfly"));
    public final Setting.Col color = add(new Setting.Col("Color", 0.60f, 0.60f));
    public final Setting.Num size = add(new Setting.Num("Size", 1.0, 0.5, 1.6, 0.05));
    public final Setting.Bool glow = add(new Setting.Bool("Glow", true));
    public final Setting.Bool flap = add(new Setting.Bool("Flap", true));

    private float sweep = 40, lift = 10; // grados, suavizados entre cuadros

    public Wings() { super("Wings", "Alas brillantes"); }
    @Override public String info() { return dev.orbita.client.nova.core.Lang.t(style.get()); }

    @Override
    public void render(Ctx c) {
        PoseStack ms = c.ms();
        // pose objetivo segun lo que haga el jugador
        float t = c.time(), tSweep, tLift;
        if (c.state().isDiscrete) { tSweep = 78; tLift = -4; }
        else if (c.airborne() && flap.value) { tSweep = 22 + 26 * (float) Math.sin(t * 0.45); tLift = 14 + 18 * (float) Math.sin(t * 0.45 + 0.6); }
        else { tSweep = 38 + (flap.value ? 4 * (float) Math.sin(t * 0.08) : 0); tLift = 8 + (flap.value ? 3 * (float) Math.sin(t * 0.08 + 1) : 0); }
        sweep += (tSweep - sweep) * 0.2f;
        lift += (tLift - lift) * 0.2f;

        c.model().body.translateAndRotate(ms);
        for (int side = -1; side <= 1; side += 2) {
            ms.pushPose();
            ms.translate(side * 0.05f, 0.22f, 0.15f);
            ms.mulPose(Axis.YP.rotationDegrees(-side * sweep));
            ms.mulPose(Axis.ZP.rotationDegrees(-side * lift));
            switch (style.index) {
                case 1 -> dragon(c, side);
                case 2 -> butterfly(c, side);
                default -> angel(c, side);
            }
            ms.popPose();
        }
    }

    /** Punto del ala: u hacia afuera, v hacia arriba, w hacia atras (en bloques). */
    private static Vector3f p(int side, float u, float v, float w) { return new Vector3f(side * u, -v, w); }

    // ------------------------------------------------------------------ angel
    private void angel(Ctx c, int side) {
        var e = c.ms().last();
        float s = size.f();
        int rgb = color.rgb(), tipRgb = Mesh.mix(rgb, 0xFFFFFF, 0.55f), dark = Mesh.shade(rgb, 0.7f);
        float eu = 0.42f * s, ev = 0.30f * s, wu = 1.0f * s, wv = 0.42f * s; // codo y punta del hueso
        int n = 14;
        float[][] root = new float[n][2], tip = new float[n][2];
        double[] ang = new double[n];
        float[] len = new float[n];
        for (int i = 0; i < n; i++) {
            float k = i / (float) (n - 1);
            if (k < 0.45f) { float q = k / 0.45f; root[i][0] = eu * q; root[i][1] = ev * q; }
            else { float q = (k - 0.45f) / 0.55f; root[i][0] = eu + (wu - eu) * q; root[i][1] = ev + (wv - ev) * q; }
            ang[i] = Math.toRadians(-98 + 80 * Math.pow(k, 0.85));
            len[i] = s * (0.42f + 0.58f * (float) Math.sin(Math.PI * (0.22 + 0.62 * k)));
            tip[i][0] = root[i][0] + (float) Math.cos(ang[i]) * len[i];
            tip[i][1] = root[i][1] + (float) Math.sin(ang[i]) * len[i];
        }
        VertexConsumer vc = c.lit();
        // plumas largas: de atras hacia adelante, cada una un poco separada para que no parpadeen
        for (int i = 0; i < n; i++) feather(vc, e, side, root[i], ang[i], len[i], 0.095f * s, 0.028f * s, 0.003f * i, Mesh.argb(dark, 255), Mesh.argb(tipRgb, 235), c.light());
        // plumas cortas encima, mas claras
        for (int i = 0; i < n; i++) feather(vc, e, side, root[i], ang[i] + 0.15, len[i] * 0.5f, 0.085f * s, 0.035f * s, -0.01f + 0.003f * i, Mesh.argb(rgb, 255), Mesh.argb(Mesh.mix(rgb, 0xFFFFFF, 0.35f), 255), c.light());
        // hueso
        float bw = 0.035f * s;
        Mesh.quad(vc, e, p(side, 0, -bw, -0.02f), p(side, 0, bw, -0.02f), p(side, eu, ev + bw, -0.02f), p(side, eu, ev - bw, -0.02f), Mesh.argb(tipRgb, 255), c.light(), true);
        Mesh.quad(vc, e, p(side, eu, ev - bw, -0.02f), p(side, eu, ev + bw, -0.02f), p(side, wu, wv + bw * 0.4f, -0.02f), p(side, wu, wv - bw * 0.4f, -0.02f), Mesh.argb(tipRgb, 255), c.light(), true);
        if (glow.value) { // un velo de luz por encima de las plumas
            VertexConsumer g = c.glow();
            for (int i = 0; i < n; i++) feather(g, e, side, root[i], ang[i], len[i] * 1.04f, 0.09f * s, 0.035f * s, -0.002f, Mesh.argb(rgb, 70), Mesh.argb(tipRgb, 25), c.fullLight());
        }
    }

    private static void feather(VertexConsumer vc, com.mojang.blaze3d.vertex.PoseStack.Pose e, int side, float[] r, double ang,
                                float len, float wRoot, float wTip, float w, int cRoot, int cTip, int light) {
        float du = (float) Math.cos(ang), dv = (float) Math.sin(ang), pu = -dv, pv = du;
        float tu = r[0] + du * len, tv = r[1] + dv * len;
        Vector3f a = p(side, r[0] - pu * wRoot, r[1] - pv * wRoot, w), b = p(side, r[0] + pu * wRoot, r[1] + pv * wRoot, w);
        Vector3f cc = p(side, tu + pu * wTip, tv + pv * wTip, w), d = p(side, tu - pu * wTip, tv - pv * wTip, w);
        Mesh.quad(vc, e, a, b, cc, d, cRoot, cRoot, cTip, cTip, null, light, true);
        // punta redondeada
        Mesh.tri(vc, e, d, cc, p(side, tu + du * wTip * 2.2f, tv + dv * wTip * 2.2f, w), cTip, cTip, cTip, light, true);
    }

    // ------------------------------------------------------------------ dragon
    private void dragon(Ctx c, int side) {
        var e = c.ms().last();
        float s = size.f();
        int rgb = color.rgb(), bone = Mesh.shade(rgb, 0.55f), skin = Mesh.shade(rgb, 0.9f), edge = Mesh.shade(rgb, 0.4f);
        float[] sh = {0, 0}, el = {0.38f * s, 0.36f * s}, wr = {0.72f * s, 0.55f * s};
        double[] fa = {Math.toRadians(-8), Math.toRadians(-38), Math.toRadians(-68), Math.toRadians(-98)};
        float[] fl = {0.7f * s, 0.78f * s, 0.7f * s, 0.55f * s};
        float[][] tips = new float[4][];
        for (int i = 0; i < 4; i++) tips[i] = new float[]{wr[0] + (float) Math.cos(fa[i]) * fl[i], wr[1] + (float) Math.sin(fa[i]) * fl[i]};
        float[] body = {0.02f, -0.5f * s};
        VertexConsumer vc = c.lit();
        // membrana: entre dedos vecinos, con el borde curvado hacia dentro
        float[][] chain = {tips[0], tips[1], tips[2], tips[3], body};
        for (int i = 0; i < chain.length - 1; i++) {
            float[] a = chain[i], b = chain[i + 1];
            float[] mid = {(a[0] + b[0]) / 2 * 0.8f + wr[0] * 0.2f, (a[1] + b[1]) / 2 * 0.8f + wr[1] * 0.2f};
            Mesh.quad(vc, e, p(side, wr[0], wr[1], 0), p(side, a[0], a[1], 0), p(side, mid[0], mid[1], 0), p(side, b[0], b[1], 0),
                    Mesh.argb(skin, 225), Mesh.argb(edge, 225), Mesh.argb(edge, 225), Mesh.argb(edge, 225), null, c.light(), true);
        }
        Mesh.quad(vc, e, p(side, sh[0], sh[1], 0), p(side, el[0], el[1], 0), p(side, wr[0], wr[1], 0), p(side, body[0], body[1], 0),
                Mesh.argb(skin, 225), Mesh.argb(skin, 225), Mesh.argb(skin, 225), Mesh.argb(edge, 225), null, c.light(), true);
        // huesos con algo de grosor
        bone(vc, e, side, sh, el, 0.045f * s, bone, c.light());
        bone(vc, e, side, el, wr, 0.04f * s, bone, c.light());
        for (int i = 0; i < 4; i++) bone(vc, e, side, wr, tips[i], 0.025f * s, bone, c.light());
        // garra en la muñeca
        Mesh.tri(vc, e, p(side, wr[0] - 0.03f, wr[1], -0.01f), p(side, wr[0] + 0.03f, wr[1], -0.01f), p(side, wr[0] + 0.02f, wr[1] + 0.12f * s, -0.01f),
                Mesh.argb(0xE8E0D0, 255), Mesh.argb(0xE8E0D0, 255), Mesh.argb(0xFFFFFF, 255), c.light(), true);
        if (glow.value) {
            VertexConsumer g = c.glow();
            for (int i = 0; i < 4; i++) bone(g, e, side, wr, tips[i], 0.05f * s, Mesh.mix(rgb, 0xFFFFFF, 0.3f), c.fullLight(), 60);
        }
    }

    private static void bone(VertexConsumer vc, com.mojang.blaze3d.vertex.PoseStack.Pose e, int side, float[] a, float[] b, float w, int rgb, int light) {
        bone(vc, e, side, a, b, w, rgb, light, 255);
    }

    private static void bone(VertexConsumer vc, com.mojang.blaze3d.vertex.PoseStack.Pose e, int side, float[] a, float[] b, float w, int rgb, int light, int alpha) {
        float du = b[0] - a[0], dv = b[1] - a[1], l = (float) Math.sqrt(du * du + dv * dv);
        if (l < 1e-5f) return;
        float pu = -dv / l * w, pv = du / l * w;
        int c0 = Mesh.argb(rgb, alpha), c1 = Mesh.argb(Mesh.shade(rgb, 1.25f), alpha);
        Mesh.quad(vc, e, p(side, a[0] - pu, a[1] - pv, -0.012f), p(side, a[0] + pu, a[1] + pv, -0.012f),
                p(side, b[0] + pu * 0.4f, b[1] + pv * 0.4f, -0.012f), p(side, b[0] - pu * 0.4f, b[1] - pv * 0.4f, -0.012f), c0, c0, c1, c1, null, light, true);
    }

    // ------------------------------------------------------------------ mariposa
    private void butterfly(Ctx c, int side) {
        var e = c.ms().last();
        float s = size.f();
        int rgb = color.rgb(), rim = Mesh.shade(rgb, 0.35f), inner = Mesh.mix(rgb, 0xFFFFFF, 0.25f);
        VertexConsumer vc = c.lit();
        lobe(vc, e, side, 10, 100, 0.85f * s, 0.6f, inner, rgb, rim, c.light());   // ala de arriba
        lobe(vc, e, side, -95, -15, 0.55f * s, 0.75f, inner, rgb, rim, c.light()); // ala de abajo
        // manchas que brillan
        VertexConsumer em = c.emissive();
        spot(em, e, side, 0.45f * s, 0.42f * s, 0.07f * s, Mesh.mix(rgb, 0xFFFFFF, 0.7f), c.fullLight());
        spot(em, e, side, 0.28f * s, 0.62f * s, 0.045f * s, 0xFFFFFF, c.fullLight());
        spot(em, e, side, 0.3f * s, -0.32f * s, 0.05f * s, Mesh.mix(rgb, 0xFFFFFF, 0.6f), c.fullLight());
        if (glow.value) lobe(c.glow(), e, side, 10, 100, 0.9f * s, 0.6f, rgb, rgb, rgb, c.fullLight(), 45);
    }

    private static void lobe(VertexConsumer vc, com.mojang.blaze3d.vertex.PoseStack.Pose e, int side, float a0, float a1, float r,
                             float round, int cIn, int cMid, int cRim, int light) {
        lobe(vc, e, side, a0, a1, r, round, cIn, cMid, cRim, light, 230);
    }

    private static void lobe(VertexConsumer vc, com.mojang.blaze3d.vertex.PoseStack.Pose e, int side, float a0, float a1, float r,
                             float round, int cIn, int cMid, int cRim, int light, int alpha) {
        int segs = 14;
        for (int i = 0; i < segs; i++) {
            float k0 = i / (float) segs, k1 = (i + 1) / (float) segs;
            double t0 = Math.toRadians(a0 + (a1 - a0) * k0), t1 = Math.toRadians(a0 + (a1 - a0) * k1);
            float r0 = r * (float) Math.pow(Math.sin(Math.PI * k0), round) + 0.02f, r1 = r * (float) Math.pow(Math.sin(Math.PI * k1), round) + 0.02f;
            Vector3f o = p(side, 0, 0, 0);
            Vector3f m0 = p(side, (float) Math.cos(t0) * r0 * 0.7f, (float) Math.sin(t0) * r0 * 0.7f, 0), m1 = p(side, (float) Math.cos(t1) * r1 * 0.7f, (float) Math.sin(t1) * r1 * 0.7f, 0);
            Vector3f q0 = p(side, (float) Math.cos(t0) * r0, (float) Math.sin(t0) * r0, 0), q1 = p(side, (float) Math.cos(t1) * r1, (float) Math.sin(t1) * r1, 0);
            Mesh.tri(vc, e, o, m0, m1, Mesh.argb(cIn, alpha), Mesh.argb(cMid, alpha), Mesh.argb(cMid, alpha), light, true);
            Mesh.quad(vc, e, m0, q0, q1, m1, Mesh.argb(cMid, alpha), Mesh.argb(cRim, alpha), Mesh.argb(cRim, alpha), Mesh.argb(cMid, alpha), null, light, true);
        }
    }

    private static void spot(VertexConsumer vc, com.mojang.blaze3d.vertex.PoseStack.Pose e, int side, float u, float v, float r, int rgb, int light) {
        int segs = 10;
        for (int i = 0; i < segs; i++) {
            double t0 = i * Math.PI * 2 / segs, t1 = (i + 1) * Math.PI * 2 / segs;
            Mesh.tri(vc, e, p(side, u, v, -0.006f), p(side, u + (float) Math.cos(t0) * r, v + (float) Math.sin(t0) * r, -0.006f),
                    p(side, u + (float) Math.cos(t1) * r, v + (float) Math.sin(t1) * r, -0.006f), Mesh.argb(rgb, 255), Mesh.argb(rgb, 200), Mesh.argb(rgb, 200), light, true);
        }
    }
}
