package dev.orbita.client.nova.gfx;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import com.mojang.blaze3d.vertex.BufferBuilder;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Random;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * Particulas propias de Nova (solo visuales, no son las de Minecraft): formas que brillan y miran a la camara.
 * Las usan los efectos de golpe, salto, muerte, totem y ambiente. Tope de 1500 a la vez.
 */
public final class Fx {
    public enum Shape {
        GLOW(Tex.GLOW), STAR(Tex.STAR), HEART(Tex.HEART), SPARK(Tex.SPARK), RING(Tex.RING);
        final Identifier tex;
        Shape(Identifier t) { tex = t; }
    }

    public static final String[] SHAPES = {"Glow", "Stars", "Hearts", "Sparks", "Rings"};

    public static final class P {
        public double x, y, z, vx, vy, vz, oldX, oldY, oldZ;
        public float size, growth, gravity = 0.0f, drag = 0.96f, spin, angle;
        public int rgb, age, life;
        public Shape shape;
        /** Anillo tumbado en el suelo (salto) en vez de mirar a la camara. */
        public boolean flat;
    }

    private static final List<P> ALL = new ArrayList<>();
    public static final Random RNG = new Random();

    private Fx() { }

    public static P spawn(Shape shape, Vec3 pos, double vx, double vy, double vz, float size, int rgb, int life) {
        if (ALL.size() >= 1500) ALL.remove(0);
        P p = new P();
        p.x = p.oldX = pos.x; p.y = p.oldY = pos.y; p.z = p.oldZ = pos.z;
        p.vx = vx; p.vy = vy; p.vz = vz; p.size = size; p.rgb = rgb; p.life = life; p.shape = shape;
        p.spin = (RNG.nextFloat() - 0.5f) * 0.3f; p.angle = RNG.nextFloat() * 6.28f;
        ALL.add(p);
        return p;
    }

    /** Explosion de particulas en todas direcciones. */
    public static void burst(Shape shape, Vec3 pos, int count, double speed, float size, int rgb, int life) {
        for (int i = 0; i < count; i++) {
            double a = RNG.nextDouble() * Math.PI * 2, b = Math.acos(RNG.nextDouble() * 2 - 1);
            double s = speed * (0.4 + RNG.nextDouble() * 0.8);
            P p = spawn(shape, pos, Math.sin(b) * Math.cos(a) * s, Math.cos(b) * s * 0.8 + speed * 0.3, Math.sin(b) * Math.sin(a) * s,
                    size * (0.7f + RNG.nextFloat() * 0.6f), rgb, life + RNG.nextInt(10));
            p.gravity = 0.012f;
        }
    }

    public static int count() { return ALL.size(); }

    public static void tick() {
        if (dev.orbita.client.nova.module.ClientSettings.eco()) { ALL.clear(); return; } // modo ahorro: sin particulas de Nova
        for (int i = ALL.size() - 1; i >= 0; i--) {
            P p = ALL.get(i);
            p.oldX = p.x; p.oldY = p.y; p.oldZ = p.z;
            p.x += p.vx; p.y += p.vy; p.z += p.vz;
            p.vx *= p.drag; p.vy = p.vy * p.drag - p.gravity; p.vz *= p.drag;
            p.angle += p.spin;
            p.size += p.growth;
            if (++p.age >= p.life) ALL.remove(i);
        }
    }

    public static void clear() { ALL.clear(); }

    public static void render(dev.orbita.client.nova.gfx.WCtx ctx) {
        if (ALL.isEmpty()) return;
        Tex.ensure();
        Vec3 cam = ctx.camera().position();
        Quaternionf rot = ctx.camera().rotation();
        float pt = ctx.tickDelta();
        Matrix4f m = ctx.matrixStack() != null ? ctx.matrixStack().last().pose() : new Matrix4f();
        // una tanda por forma (cada forma es una textura)
        EnumMap<Shape, List<P>> by = new EnumMap<>(Shape.class);
        for (P p : ALL) by.computeIfAbsent(p.shape, k -> new ArrayList<>()).add(p);
        for (var en : by.entrySet()) {
            BufferBuilder b = dev.orbita.client.nova.compat.V.begin(dev.orbita.client.nova.compat.V.Mode.TEX_ADD, en.getKey().tex); // brillo aditivo
            for (P p : en.getValue()) {
                float x = (float) (p.oldX + (p.x - p.oldX) * pt - cam.x), y = (float) (p.oldY + (p.y - p.oldY) * pt - cam.y), z = (float) (p.oldZ + (p.z - p.oldZ) * pt - cam.z);
                float k = (p.age + pt) / p.life, alpha = k < 0.15f ? k / 0.15f : 1 - (k - 0.15f) / 0.85f;
                int c = (Math.max(0, Math.min(255, (int) (alpha * 255))) << 24) | (p.rgb & 0xFFFFFF);
                float s = p.size / 2;
                Vector3f r, u;
                if (p.flat) { r = new Vector3f(s, 0, 0); u = new Vector3f(0, 0, s); }
                else {
                    float ca = (float) Math.cos(p.angle), sa = (float) Math.sin(p.angle);
                    r = rot.transform(new Vector3f(ca * s, sa * s, 0));
                    u = rot.transform(new Vector3f(-sa * s, ca * s, 0));
                }
                b.addVertex(m, x - r.x - u.x, y - r.y - u.y, z - r.z - u.z).setUv(0, 1).setColor(c);
                b.addVertex(m, x + r.x - u.x, y + r.y - u.y, z + r.z - u.z).setUv(1, 1).setColor(c);
                b.addVertex(m, x + r.x + u.x, y + r.y + u.y, z + r.z + u.z).setUv(1, 0).setColor(c);
                b.addVertex(m, x - r.x + u.x, y - r.y + u.y, z - r.z + u.z).setUv(0, 0).setColor(c);
            }
            dev.orbita.client.nova.compat.V.end(dev.orbita.client.nova.compat.V.Mode.TEX_ADD, en.getKey().tex, b);
        }
    }
}
