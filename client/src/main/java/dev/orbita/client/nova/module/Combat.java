package dev.orbita.client.nova.module;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Sigue lo que pasa en la pelea para los efectos visuales: a quien golpeaste, cuando recibe el golpe, cuando muere,
 * los totems que explotan y los TPS del servidor. No cambia nada del juego.
 */
public final class Combat {
    private Combat() { }

    public interface Listener {
        default void onHit(LivingEntity target) { }
        default void onKill(LivingEntity target) { }
        default void onTotem(LivingEntity who) { }
        default void onAttack(Entity target) { }
    }

    private static final List<Listener> LISTENERS = new ArrayList<>();
    public static LivingEntity target;
    public static long targetTime;
    private static int lastHurt;
    private static boolean killed;

    public static void listen(Listener l) { LISTENERS.add(l); }

    /** Llamado al pegarle a algo (evento de Fabric). */
    public static void attacked(Entity e) {
        for (Listener l : LISTENERS) l.onAttack(e);
        if (e instanceof LivingEntity le) {
            if (le != target) lastHurt = 0;
            target = le; targetTime = System.currentTimeMillis(); killed = false;
        }
    }

    /** Objetivo actual (se olvida a los 6 segundos o si se aleja mucho). */
    public static LivingEntity target() {
        var mc = Minecraft.getInstance();
        if (target == null || mc.player == null) return null;
        if (System.currentTimeMillis() - targetTime > 6000 || target.distanceToSqr(mc.player) > 30 * 30 || (target.isRemoved() && !target.isDeadOrDying())
                || (target.isDeadOrDying() && System.currentTimeMillis() - targetTime > 1200)) target = null;
        return target;
    }

    public static void tick() {
        LivingEntity t = target();
        if (t == null) return;
        if (t.hurtTime > lastHurt && t.hurtTime >= 8) { // empezo a parpadear rojo: el golpe entro
            targetTime = System.currentTimeMillis();
            for (Listener l : LISTENERS) l.onHit(t);
        }
        lastHurt = t.hurtTime;
        if (!killed && (t.isDeadOrDying() || t.getHealth() <= 0)) {
            killed = true;
            for (Listener l : LISTENERS) l.onKill(t);
        }
    }

    public static void totem(LivingEntity who) { for (Listener l : LISTENERS) l.onTotem(who); }

    // ---- TPS: el servidor manda la hora cada 20 ticks; si tarda mas de 1 segundo, va lento
    private static long lastTimePacket;
    private static double tps = 20;

    public static void timePacket() {
        long now = System.nanoTime();
        if (lastTimePacket != 0) {
            double secs = (now - lastTimePacket) / 1e9;
            if (secs > 0.05) tps = tps * 0.7 + Math.min(20, 20 / secs) * 0.3;
        }
        lastTimePacket = now;
    }

    public static double tps() { return tps; }
}
