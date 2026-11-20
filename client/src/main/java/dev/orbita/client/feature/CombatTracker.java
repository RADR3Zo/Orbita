package dev.orbita.client.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** Combo (golpes seguidos sin recibir daño) y alcance del último golpe, como en los clientes PvP. */
public final class CombatTracker {
    private static final long COMBO_TIMEOUT_MS = 2500;
    private static final long REACH_SHOW_MS = 3000;

    private static int combo;
    private static long lastHit;
    private static double lastReach = -1;
    private static long lastReachTime;
    private static int lastHurtTime;

    private CombatTracker() {
    }

    public static void onAttack(Player player, Entity target) {
        Minecraft mc = Minecraft.getInstance();
        HitResult hit = mc.hitResult;
        if (hit instanceof EntityHitResult ehr && ehr.getEntity() == target) {
            lastReach = player.getEyePosition().distanceTo(ehr.getLocation());
            lastReachTime = System.currentTimeMillis();
        }
        if (target instanceof LivingEntity living && living.hurtTime == 0) {
            combo++;
            lastHit = System.currentTimeMillis();
        }
    }

    public static void tick(Minecraft mc) {
        if (mc.player == null) {
            combo = 0;
            return;
        }
        int hurt = mc.player.hurtTime;
        if (hurt > lastHurtTime) {
            combo = 0;
        }
        lastHurtTime = hurt;
        if (combo > 0 && System.currentTimeMillis() - lastHit > COMBO_TIMEOUT_MS) {
            combo = 0;
        }
    }

    public static int combo() {
        return combo;
    }

    /** Alcance del último golpe en bloques, o -1 si hace rato que no golpeas. */
    public static double reach() {
        return System.currentTimeMillis() - lastReachTime > REACH_SHOW_MS ? -1 : lastReach;
    }
}
