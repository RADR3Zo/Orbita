package dev.orbita.client.nova.module;

import dev.orbita.client.nova.core.Setting;
import net.minecraft.world.InteractionHand;

/**
 * Gestos que usan acciones reales (golpe de mano, agacharse, girar, saltar), asi que los demas
 * jugadores tambien los ven en un servidor. Se abren con la tecla B (rueda de emotes).
 */
public final class Emotes extends Module {
    public enum Emote {
        WAVE("Wave", 36), DANCE("Dance", 100), SPIN("Spin", 20), NOD("Nod", 40), PARTY("Party Jump", 44);
        public final String label; final int length;
        Emote(String l, int len) { label = l; length = len; }
    }

    public final Setting.Bool stopOnMove = add(new Setting.Bool("Stop when moving", true));

    private static Emote active;
    private static int tick;

    public Emotes() { super("Emote Wheel", "Gestures: wave, dance, spin...", Category.COSMETICS); enabled = true; }

    public static void play(Emote e) { active = e; tick = 0; }
    public static boolean playing() { return active != null; }

    private static void stop() {
        active = null;
        var mc = net.minecraft.client.Minecraft.getInstance();
        mc.options.keyShift.setDown(false);
        mc.options.keyJump.setDown(false);
    }

    @Override public void onTick() {
        var mc = mc();
        var p = mc.player;
        if (p == null || active == null || mc.screen != null) return;
        var o = mc.options;
        if (stopOnMove.value && (o.keyUp.isDown() || o.keyDown.isDown() || o.keyLeft.isDown() || o.keyRight.isDown())) { stop(); return; }
        switch (active) {
            case WAVE -> { if (tick % 6 == 0) p.swing(InteractionHand.MAIN_HAND); }
            case DANCE -> {
                o.keyShift.setDown((tick / 4) % 2 == 0);
                p.setYRot(p.getYRot() + (float) Math.sin(tick * 0.35) * 9f);
                if (tick % 10 == 0) p.swing(tick % 20 == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
            }
            case SPIN -> p.setYRot(p.getYRot() + 18f);
            case NOD -> p.setXRot((float) (15 + Math.sin(tick * 0.55) * 28));
            case PARTY -> { o.keyJump.setDown(tick % 11 < 2); if (tick % 11 == 0) p.swing(InteractionHand.MAIN_HAND); }
        }
        if (++tick >= active.length) stop();
    }

    @Override public void onDisable() { if (active != null) stop(); }
}
