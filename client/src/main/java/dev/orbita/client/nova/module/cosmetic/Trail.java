package dev.orbita.client.nova.module.cosmetic;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.orbita.client.nova.core.Setting;
import dev.orbita.client.nova.gfx.Gfx;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Estela brillante que deja el jugador al moverse. */
public final class Trail extends Cosmetic {
    public final Setting.Num length = add(new Setting.Num("Length", 30, 8, 80, 1));
    public final Setting.Num height = add(new Setting.Num("Height", 0.5, 0.05, 1.8, 0.05));
    public final Setting.Num lift = add(new Setting.Num("Lift", 0.2, 0.0, 1.5, 0.05));
    public final Setting.Bool rainbow = add(new Setting.Bool("Rainbow", false));
    public final Setting.Col color = add(new Setting.Col("Color", 0.12f, 0.8f)).showIf(() -> !rainbow.value);

    private final ArrayDeque<Vec3> points = new ArrayDeque<>();

    public Trail() { super("Trail", "Estela de luz al moverte"); }

    @Override boolean showInFirstPerson() { return true; }

    @Override public void onDisable() { points.clear(); }

    @Override public void onTick() {
        Player p = mc().player;
        if (p == null) return;
        Vec3 pos = p.position();
        if (points.isEmpty() || points.peekLast().distanceToSqr(pos) > 0.004) points.addLast(pos);
        else if (!points.isEmpty()) points.pollFirst(); // quieto: la estela se va apagando
        while (points.size() > length.i()) points.pollFirst();
    }

    private Vec3 cam = Vec3.ZERO;

    @Override
    public void onWorldAfter(dev.orbita.client.nova.gfx.WCtx ctx) {
        if (points.size() < 2 || mc().level == null || mc().player == null) return;
        cam = ctx.camera().position();
        PoseStack ms = ctx.matrixStack() != null ? ctx.matrixStack() : new PoseStack();
        Gfx.begin();
        draw(ms, mc().player, mc().level.getGameTime(), 0, ctx);
        Gfx.end();
    }

    /**
     * Cinta de luz que siempre mira a la camara (si fuera una pared vertical, de lado se veria como una raya).
     * Las posiciones se restan a la camara en double, asi no tiembla lejos del centro del mundo.
     */
    @Override
    void draw(PoseStack ms, Player p, float time, float delta, dev.orbita.client.nova.gfx.WCtx ctx) {
        Matrix4f m = ms.last().pose();
        List<Vec3> pts = new ArrayList<>(points);
        pts.add(p.getPosition(ctx.tickDelta())); // la cinta llega hasta el jugador sin saltos
        int n = pts.size();
        float mid = lift.f() + height.f() / 2, half = height.f() / 2;
        Vector3f[] c = new Vector3f[n], side = new Vector3f[n];
        for (int i = 0; i < n; i++) {
            Vec3 q = pts.get(i);
            c[i] = new Vector3f((float) (q.x - cam.x), (float) (q.y + mid - cam.y), (float) (q.z - cam.z));
        }
        for (int i = 0; i < n; i++) {
            Vector3f dir = new Vector3f(c[Math.min(n - 1, i + 1)]).sub(c[Math.max(0, i - 1)]);
            Vector3f s = dir.cross(new Vector3f(c[i]), new Vector3f()); // c[i] = vector camara -> punto
            float len = s.length();
            side[i] = len < 1e-6f ? new Vector3f(0, half, 0) : s.mul(half / len);
        }
        for (int i = 0; i < n - 1; i++) {
            float fa = i / (float) (n - 1), fb = (i + 1) / (float) (n - 1);
            int rgbA = rainbow.value ? Color.HSBtoRGB((time * 0.01f + i * 0.02f) % 1f, 0.8f, 1f) : color.rgb();
            int rgbB = rainbow.value ? Color.HSBtoRGB((time * 0.01f + (i + 1) * 0.02f) % 1f, 0.8f, 1f) : color.rgb();
            int ca = Gfx.argb(rgbA, (int) (fa * fa * 230)), cb = Gfx.argb(rgbB, (int) (fb * fb * 230));
            Vector3f sa = new Vector3f(side[i]).mul(0.25f + 0.75f * fa), sb = new Vector3f(side[i + 1]).mul(0.25f + 0.75f * fb);
            Gfx.quad(m, new Vector3f(c[i]).sub(sa), ca, new Vector3f(c[i]).add(sa), ca,
                    new Vector3f(c[i + 1]).add(sb), cb, new Vector3f(c[i + 1]).sub(sb), cb);
        }
    }
}
