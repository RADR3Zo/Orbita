package dev.orbita.client.nova.gfx;

import com.mojang.blaze3d.vertex.BufferBuilder;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Ayudante para dibujar geometria de color con brillo aditivo (cosmeticos). */
public final class Gfx {
    private static BufferBuilder b;
    private static int count;

    private Gfx() { }

    public static void begin() {
        b = dev.orbita.client.nova.compat.V.begin(dev.orbita.client.nova.compat.V.Mode.COLOR_ADD, null);
        count = 0;
    }

    public static void quad(Matrix4f m, Vector3f a, int ca, Vector3f c, int cc, Vector3f d, int cd, Vector3f e, int ce) {
        b.addVertex(m, a.x, a.y, a.z).setColor(ca);
        b.addVertex(m, c.x, c.y, c.z).setColor(cc);
        b.addVertex(m, d.x, d.y, d.z).setColor(cd);
        b.addVertex(m, e.x, e.y, e.z).setColor(ce);
        count += 4;
    }

    public static void end() {
        dev.orbita.client.nova.compat.V.end(dev.orbita.client.nova.compat.V.Mode.COLOR_ADD, null, b);
        b = null;
    }

    /** Caja rellena (6 caras) en coordenadas ya relativas a la camara. */
    public static void box(Matrix4f m, float x0, float y0, float z0, float x1, float y1, float z1, int c) {
        Vector3f a = new Vector3f(x0, y0, z0), b = new Vector3f(x1, y0, z0), cc = new Vector3f(x1, y0, z1), d = new Vector3f(x0, y0, z1);
        Vector3f e = new Vector3f(x0, y1, z0), f = new Vector3f(x1, y1, z0), g = new Vector3f(x1, y1, z1), h = new Vector3f(x0, y1, z1);
        quad(m, a, c, b, c, cc, c, d, c); quad(m, e, c, f, c, g, c, h, c);
        quad(m, a, c, b, c, f, c, e, c); quad(m, d, c, cc, c, g, c, h, c);
        quad(m, a, c, d, c, h, c, e, c); quad(m, b, c, cc, c, g, c, f, c);
    }

    /** Mezcla dos colores RGB (t = 0 -> a, t = 1 -> b). */
    public static int mixRgb(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }

    /** ARGB a partir de rgb y alpha 0..255. */
    public static int argb(int rgb, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0xFFFFFF);
    }

    /** Anillo plano (en el plano XZ) a altura y. */
    public static void ringXZ(Matrix4f m, float y, float r, float w, int seg, int color) {
        for (int i = 0; i < seg; i++) {
            double a0 = i * Math.PI * 2 / seg, a1 = (i + 1) * Math.PI * 2 / seg;
            quad(m,
                    new Vector3f((float) Math.cos(a0) * (r - w), y, (float) Math.sin(a0) * (r - w)), color,
                    new Vector3f((float) Math.cos(a0) * (r + w), y, (float) Math.sin(a0) * (r + w)), color,
                    new Vector3f((float) Math.cos(a1) * (r + w), y, (float) Math.sin(a1) * (r + w)), color,
                    new Vector3f((float) Math.cos(a1) * (r - w), y, (float) Math.sin(a1) * (r - w)), color);
        }
    }

    /** Segmento plano (en el plano XZ) con grosor w. */
    public static void segXZ(Matrix4f m, float ax, float az, float bx, float bz, float y, float w, int color) {
        float dx = bx - ax, dz = bz - az, len = (float) Math.sqrt(dx * dx + dz * dz);
        if (len < 1e-5f) return;
        float nx = -dz / len * w, nz = dx / len * w;
        quad(m, new Vector3f(ax - nx, y, az - nz), color, new Vector3f(ax + nx, y, az + nz), color,
                new Vector3f(bx + nx, y, bz + nz), color, new Vector3f(bx - nx, y, bz - nz), color);
    }
}
