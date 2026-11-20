package dev.orbita.client.nova.gui;

import dev.orbita.client.nova.core.Lang;
import dev.orbita.client.nova.core.Theme;
import dev.orbita.client.nova.module.Emotes;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Rueda de emotes: tarjetas de cristal; click para reproducir el gesto. */
public class EmoteScreen extends NovaScreen {
    private static final int CW = 78, CH = 54, GAP = 8;

    public EmoteScreen() { super(Component.literal("Emotes")); }

    @Override public boolean isPauseScreen() { return false; }

    private int x0(int n) { return (width - (n * CW + (n - 1) * GAP)) / 2; }
    private int y0() { return height / 2 - CH / 2; }

    @Override
    public void render(GuiGraphics ctx, int mx, int my, float delta) {
        Font tr = font;
        ctx.fill(0, 0, width, height, 0x88050509);
        Ui.blob(ctx, width / 2, height / 2, 170, Theme.rgb(), 100);
        Emotes.Emote[] all = Emotes.Emote.values();
        int x0 = x0(all.length);
        Ui.ctext(ctx, tr, Lang.t("Emotes"), width / 2, y0() - 24, 0xFFFFFFFF, true);
        for (int i = 0; i < all.length; i++) {
            int x = x0 + i * (CW + GAP), y = y0();
            boolean hov = mx >= x && mx < x + CW && my >= y && my < y + CH;
            if (hov) Ui.glow(ctx, x, y, CW, CH, 12, Theme.rgb(), 80, 5);
            Ui.roundRect(ctx, x, y, CW, CH, 12, hov ? Theme.accent(70) : 0x1CFFFFFF);
            Ui.outline(ctx, x, y, CW, CH, 12, hov ? Theme.accent(180) : 0x22FFFFFF);
            Ui.ctext(ctx, tr, Integer.toString(i + 1), x + CW / 2, y + 12, hov ? Theme.light() : 0xFF6D6B80, true);
            Ui.ctext(ctx, tr, Lang.t(all[i].label), x + CW / 2, y + 32, 0xFFF3F2FA, hov);
        }
    }

    @Override
    protected boolean onClick(double mx, double my, int button) {
        Emotes.Emote[] all = Emotes.Emote.values();
        int x0 = x0(all.length);
        for (int i = 0; i < all.length; i++) {
            int x = x0 + i * (CW + GAP);
            if (mx >= x && mx < x + CW && my >= y0() && my < y0() + CH) { Emotes.play(all[i]); onClose(); return true; }
        }
        return superClick(mx, my, button);
    }

    @Override
    protected boolean onKey(int key, int scan, int mods) {
        Emotes.Emote[] all = Emotes.Emote.values();
        if (key >= 49 && key < 49 + all.length) { Emotes.play(all[key - 49]); onClose(); return true; } // teclas 1..5
        return superKey(key, scan, mods);
    }
}
