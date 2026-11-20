package dev.orbita.client.nova.module;

import dev.orbita.client.nova.NovaClient;
import dev.orbita.client.nova.core.Lang;
import dev.orbita.client.nova.core.Setting;
import dev.orbita.client.nova.core.Theme;
import dev.orbita.client.nova.gui.Ui;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Avisos que entran deslizando abajo a la derecha: modulos encendidos/apagados, totems, etc. */
public final class Notifications extends Module {
    public final Setting.Bool modules = add(new Setting.Bool("Module toggles", true));
    public final Setting.Num seconds = add(new Setting.Num("Duration", 2.5, 1, 6, 0.5));

    private record Note(String text, int rgb, long born) { }
    private static final List<Note> NOTES = new ArrayList<>();

    public Notifications() { super("Notifications", "Toasts when modules turn on or off", Category.HUD); enabled = true; }

    static Notifications get() { return NovaClient.modules == null ? null : NovaClient.modules.get(Notifications.class); }

    public static void push(String text, int rgb) {
        Notifications n = get();
        if (n == null || !n.enabled) return;
        if (NOTES.size() > 5) NOTES.remove(0);
        NOTES.add(new Note(text, rgb, System.currentTimeMillis()));
    }

    /** Lo llama Module.setEnabled. */
    /** true mientras el modo ahorro cambia modulos (no llena la pantalla de avisos). */
    static boolean mute;

    static void toggled(Module m) {
        Notifications n = get();
        if (mute || n == null || !n.enabled || !n.modules.value || m == n || m.alwaysOn) return;
        push(Lang.t(m.name) + "  " + Lang.t(m.enabled ? "On" : "Off"), m.enabled ? 0x34D399 : 0xF87171);
    }

    @Override public void onHud(GuiGraphics ctx) {
        if (mc().options.hideGui) return;
        Font tr = mc().font;
        long now = System.currentTimeMillis(), life = (long) (seconds.value * 1000);
        NOTES.removeIf(n -> now - n.born > life + 400);
        int sw = ctx.guiWidth(), y = ctx.guiHeight() - 30;
        for (int i = NOTES.size() - 1; i >= 0; i--) {
            Note n = NOTES.get(i);
            long age = now - n.born;
            float in = Ui.ease(age / 250f), out = age > life ? Ui.ease((age - life) / 400f) : 0;
            int w = Ui.width(tr, n.text, true) + 26, h = 20;
            int x = sw - 6 - Math.round((w + 6) * (in - out));
            Ui.roundRect(ctx, x, y, w, h, 7, 0xD0101018);
            Ui.circle(ctx, x + 10, y + 10, 3, 0xFF000000 | n.rgb);
            Ui.text(ctx, tr, n.text, x + 18, y + 6, 0xFFFFFFFF, true, false);
            float k = Math.max(0, 1 - age / (float) life); // barrita de tiempo
            ctx.fill(x + 6, y + h - 2, x + 6 + Math.round((w - 12) * k), y + h - 1, Theme.accent(200));
            y -= Math.round((h + 4) * (1 - out));
        }
    }
}
