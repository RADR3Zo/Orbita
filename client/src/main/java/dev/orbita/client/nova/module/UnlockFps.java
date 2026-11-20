package dev.orbita.client.nova.module;

import dev.orbita.client.nova.NovaClient;

/**
 * Quita el tope de 60 FPS: apaga V-Sync (que ata los FPS a los Hz del monitor), pone FPS sin limite y deja
 * que los menus tambien vayan sin limite (Minecraft los baja a 60).
 */
public final class UnlockFps extends Module {
    private int ticks;

    public UnlockFps() { super("Unlock FPS", "No 60 FPS cap: V-Sync off and unlimited FPS", Category.UTILITY); enabled = true; }

    public static boolean on() {
        if (NovaClient.modules == null) return false;
        UnlockFps m = NovaClient.modules.get(UnlockFps.class);
        return m != null && m.enabled;
    }

    private void apply() {
        var mc = mc();
        var o = mc.options;
        boolean changed = false;
        if (o.enableVsync().get()) { o.enableVsync().set(false); mc.getWindow().updateVsync(false); changed = true; }
        if (o.framerateLimit().get() < 260) { o.framerateLimit().set(260); changed = true; } // 260 = sin limite
        if (changed) o.save();
    }

    @Override public void onEnable() { apply(); }
    @Override public void onTick() { if (++ticks % 40 == 0) apply(); }
}
