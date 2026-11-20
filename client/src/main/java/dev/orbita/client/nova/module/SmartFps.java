package dev.orbita.client.nova.module;

import dev.orbita.client.nova.core.Setting;

/**
 * FPS inteligente: mantiene los FPS cerca de un objetivo moviendo la distancia de renderizado poco a poco.
 * Si los FPS bajan del objetivo, quita chunks; si sobran, los devuelve (hasta la distancia que tenias).
 * Al entrar a un mundo espera unos segundos (mientras carga siempre hay tirones) para no reaccionar de mas.
 */
public final class SmartFps extends Module {
    public final Setting.Num target = add(new Setting.Num("Target FPS", 60, 30, 240, 5));
    public final Setting.Num minView = add(new Setting.Num("Minimum render distance", 4, 2, 12, 1));
    public final Setting.Bool lockFps = add(new Setting.Bool("Lock FPS to target (more stable)", false));
    /** Distancia que tenia el jugador al prender el modulo (oculto). */
    private final Setting.Num saved = add(new Setting.Num("SavedView", 0, 0, 64, 1)).showIf(() -> false);
    private final Setting.Num savedMax = add(new Setting.Num("SavedMaxFps", 0, 0, 260, 1)).showIf(() -> false);

    private final int[] samples = new int[6];
    private boolean away;
    private int n, ticks, settle;
    private Object lastWorld;

    public SmartFps() { super("Smart FPS", "Keeps FPS steady by adjusting render distance", Category.UTILITY); }

    @Override public String info() { return target.i() + " FPS"; }

    @Override public void onEnable() {
        var o = mc().options;
        if (saved.value == 0) saved.set(o.renderDistance().get());
        if (lockFps.value && savedMax.value == 0) savedMax.set(o.framerateLimit().get());
        n = 0; settle = 200;
    }

    @Override public void onDisable() {
        var o = mc().options;
        if (saved.value > 0) o.renderDistance().set((int) saved.value);
        if (savedMax.value > 0) o.framerateLimit().set((int) savedMax.value);
        saved.set(0); savedMax.set(0);
        o.save();
    }

    @Override public void onTick() {
        var mc = mc();
        if (mc.level != lastWorld) { lastWorld = mc.level; settle = 200; n = 0; } // entrando o saliendo: esperar 10 s
        if (mc.level == null || mc.isPaused()) return;
        // fuera de la ventana (Alt+Tab) Minecraft baja los FPS a proposito: no se mide nada, y al volver se espera 5 s
        // (antes se bajaba la distancia por esos FPS falsos y al volver se subia de a poco, recargando chunks: tirones)
        if (!mc.isWindowActive()) { away = true; return; }
        if (away) { away = false; settle = 100; n = 0; }
        var o = mc.options;
        if (lockFps.value) {
            if (savedMax.value == 0) savedMax.set(o.framerateLimit().get());
            if (o.framerateLimit().get() != target.i()) o.framerateLimit().set(target.i());
        }
        if (settle > 0) { settle--; return; }
        if (++ticks % 20 != 0) return; // una muestra por segundo
        samples[n++ % samples.length] = mc.getFps();
        if (n < samples.length || n % 4 != 0) return; // decide cada 4 s con los ultimos 6 s
        int sum = 0;
        for (int s : samples) sum += s;
        int avg = sum / samples.length, view = o.renderDistance().get();
        int max = saved.value > 0 ? (int) saved.value : view;
        if (avg < target.value * 0.9 && view > minView.i()) o.renderDistance().set(view - 1);
        else if (avg > target.value * 1.3 && view < max) o.renderDistance().set(view + 1);
    }
}
