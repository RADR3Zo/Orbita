package dev.orbita.client.nova.gfx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Vector3f;

/**
 * Formas para cosmeticos dibujados como parte del jugador (con luz del mundo y sombreado por normales).
 * Coordenadas del modelo: 1 = un bloque, Y hacia ABAJO, -Z = cara del jugador, +Z = espalda.
 */
public final class Mesh {
    private Mesh() { }

    private static void v(VertexConsumer vc, PoseStack.Pose e, Vector3f p, int argb, float u, float w, int light, Vector3f n) {
        vc.addVertex(e, p.x, p.y, p.z).setColor(argb).setUv(u, w).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(e, n.x, n.y, n.z);
    }

    private static Vector3f normal(Vector3f a, Vector3f b, Vector3f c) {
        Vector3f n = new Vector3f(b).sub(a).cross(new Vector3f(c).sub(a));
        float l = n.length();
        return l < 1e-7f ? new Vector3f(0, -1, 0) : n.div(l);
    }

    /** Cuadrilatero con un color por vertice y coordenadas de textura; twoSided = tambien se ve por detras. */
    public static void quad(VertexConsumer vc, PoseStack.Pose e, Vector3f a, Vector3f b, Vector3f c, Vector3f d,
                            int ca, int cb, int cc, int cd, float[] uv, int light, boolean twoSided) {
        Vector3f n = normal(a, b, c);
        float[] t = uv != null ? uv : new float[]{0, 0, 0, 1, 1, 1, 1, 0};
        v(vc, e, a, ca, t[0], t[1], light, n); v(vc, e, b, cb, t[2], t[3], light, n);
        v(vc, e, c, cc, t[4], t[5], light, n); v(vc, e, d, cd, t[6], t[7], light, n);
        if (twoSided) {
            Vector3f m = new Vector3f(n).negate();
            v(vc, e, d, cd, t[6], t[7], light, m); v(vc, e, c, cc, t[4], t[5], light, m);
            v(vc, e, b, cb, t[2], t[3], light, m); v(vc, e, a, ca, t[0], t[1], light, m);
        }
    }

    public static void quad(VertexConsumer vc, PoseStack.Pose e, Vector3f a, Vector3f b, Vector3f c, Vector3f d, int color, int light, boolean twoSided) {
        quad(vc, e, a, b, c, d, color, color, color, color, null, light, twoSided);
    }

    /** Triangulo (cuadrilatero con dos vertices iguales). */
    public static void tri(VertexConsumer vc, PoseStack.Pose e, Vector3f a, Vector3f b, Vector3f c, int ca, int cb, int cc, int light, boolean twoSided) {
        quad(vc, e, a, b, c, c, ca, cb, cc, cc, null, light, twoSided);
    }

    /** Caja solida (normales hacia fuera). */
    public static void box(VertexConsumer vc, PoseStack.Pose e, float x0, float y0, float z0, float x1, float y1, float z1, int color, int light) {
        Vector3f p000 = new Vector3f(x0, y0, z0), p100 = new Vector3f(x1, y0, z0), p010 = new Vector3f(x0, y1, z0), p110 = new Vector3f(x1, y1, z0);
        Vector3f p001 = new Vector3f(x0, y0, z1), p101 = new Vector3f(x1, y0, z1), p011 = new Vector3f(x0, y1, z1), p111 = new Vector3f(x1, y1, z1);
        quad(vc, e, p000, p100, p110, p010, color, light, true);
        quad(vc, e, p101, p001, p011, p111, color, light, true);
        quad(vc, e, p001, p000, p010, p011, color, light, true);
        quad(vc, e, p100, p101, p111, p110, color, light, true);
        quad(vc, e, p001, p101, p100, p000, color, light, true);
        quad(vc, e, p010, p110, p111, p011, color, light, true);
    }

    /** Tubo (o cono si r0 != r1) vertical entre y0 e y1, con tapas opcionales. */
    public static void cylinder(VertexConsumer vc, PoseStack.Pose e, float y0, float y1, float r0, float r1, int segs,
                                int c0, int c1, int light, boolean capBottom, boolean capTop) {
        for (int i = 0; i < segs; i++) {
            double a0 = i * Math.PI * 2 / segs, a1 = (i + 1) * Math.PI * 2 / segs;
            float s0 = (float) Math.sin(a0), k0 = (float) Math.cos(a0), s1 = (float) Math.sin(a1), k1 = (float) Math.cos(a1);
            Vector3f b0 = new Vector3f(k0 * r0, y0, s0 * r0), b1 = new Vector3f(k1 * r0, y0, s1 * r0);
            Vector3f t0 = new Vector3f(k0 * r1, y1, s0 * r1), t1 = new Vector3f(k1 * r1, y1, s1 * r1);
            quad(vc, e, b0, t0, t1, b1, c0, c1, c1, c0, null, light, true);
            if (capBottom && r0 > 0) tri(vc, e, new Vector3f(0, y0, 0), b0, b1, c0, c0, c0, light, true);
            if (capTop && r1 > 0) tri(vc, e, new Vector3f(0, y1, 0), t1, t0, c1, c1, c1, light, true);
        }
    }

    /** Anillo plano horizontal a altura y. */
    public static void ring(VertexConsumer vc, PoseStack.Pose e, float y, float rIn, float rOut, int segs, int cIn, int cOut, int light) {
        for (int i = 0; i < segs; i++) {
            double a0 = i * Math.PI * 2 / segs, a1 = (i + 1) * Math.PI * 2 / segs;
            float s0 = (float) Math.sin(a0), k0 = (float) Math.cos(a0), s1 = (float) Math.sin(a1), k1 = (float) Math.cos(a1);
            quad(vc, e, new Vector3f(k0 * rIn, y, s0 * rIn), new Vector3f(k0 * rOut, y, s0 * rOut),
                    new Vector3f(k1 * rOut, y, s1 * rOut), new Vector3f(k1 * rIn, y, s1 * rIn), cIn, cOut, cOut, cIn, null, light, true);
        }
    }

    public static int argb(int rgb, int a) { return (Math.max(0, Math.min(255, a)) << 24) | (rgb & 0xFFFFFF); }

    /** Oscurece (f < 1) o aclara (f > 1) un color RGB. */
    public static int shade(int rgb, float f) {
        int r = Math.min(255, (int) (((rgb >> 16) & 255) * f)), g = Math.min(255, (int) (((rgb >> 8) & 255) * f)), b = Math.min(255, (int) ((rgb & 255) * f));
        return (r << 16) | (g << 8) | b;
    }

    public static int mix(int a, int b, float t) { return Gfx.mixRgb(a, b, Math.max(0, Math.min(1, t))); }
}
