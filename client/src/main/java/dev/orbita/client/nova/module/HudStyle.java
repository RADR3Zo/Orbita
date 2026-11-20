package dev.orbita.client.nova.module;

import dev.orbita.client.nova.NovaClient;
import dev.orbita.client.nova.core.Setting;
import dev.orbita.client.nova.core.Theme;
import dev.orbita.client.nova.gui.Ui;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Barra de objetos y mira con el estilo de Nova (reemplazan a las de Minecraft cuando estan encendidas). */
public final class HudStyle {
    private HudStyle() { }

    static <T extends Module> T on(Class<T> c) {
        if (NovaClient.modules == null) return null;
        T m = NovaClient.modules.get(c);
        return m != null && m.enabled ? m : null;
    }

    public static final class Hotbar extends Module {
        public final Setting.Bool glass = add(new Setting.Bool("Glass background", true));
        private float sel = -1;

        public Hotbar() { super("Hotbar", "Rounded glass hotbar with a smooth selector", Category.HUD); enabled = true; }

        public static Hotbar get() { return on(Hotbar.class); }

        /** Dibuja la barra; devuelve true si reemplazo a la de Minecraft. */
        public boolean draw(GuiGraphics ctx, DeltaTracker tc) {
            var mc = mc();
            Player p = mc.player;
            if (p == null || mc.gameMode == null || mc.gameMode.getPlayerMode() == net.minecraft.world.level.GameType.SPECTATOR) return false;
            int sw = ctx.guiWidth(), sh = ctx.guiHeight();
            int x0 = sw / 2 - 91, y0 = sh - 22;
            int target = p.getInventory().getSelectedSlot();
            if (sel < 0) sel = target;
            sel += (target - sel) * 0.35f; // el selector se desliza
            if (glass.value) {
                Ui.glow(ctx, x0, y0, 182, 21, 8, Theme.rgb(), 30, 4);
                Ui.roundRect(ctx, x0, y0, 182, 21, 8, 0xB0101018);
                Ui.outline(ctx, x0, y0, 182, 21, 8, 0x30FFFFFF);
            }
            int sx = x0 + 1 + Math.round(sel * 20);
            Ui.roundRect(ctx, sx, y0 + 1, 20, 19, 6, Theme.accent(90));
            Ui.outline(ctx, sx, y0 + 1, 20, 19, 6, Theme.accent(230));
            float pt = tc.getGameTimeDeltaPartialTick(false);
            for (int i = 0; i < 9; i++) slot(ctx, mc, p, p.getInventory().getNonEquipmentItems().get(i), x0 + 3 + i * 20, y0 + 3, pt);
            ItemStack off = p.getOffhandItem();
            if (!off.isEmpty()) {
                int ox = x0 - 26;
                Ui.roundRect(ctx, ox, y0, 22, 21, 7, 0xB0101018);
                slot(ctx, mc, p, off, ox + 3, y0 + 3, pt);
            }
            return true;
        }

        private static void slot(GuiGraphics ctx, net.minecraft.client.Minecraft mc, Player p, ItemStack s, int x, int y, float pt) {
            if (s.isEmpty()) return;
            ctx.renderItem(p, s, x, y, 1);
            dev.orbita.client.nova.compat.V.stackOverlay(ctx, mc.font, s, x, y);
        }
    }

    public static final class Crosshair extends Module {
        public final Setting.Choice style = add(new Setting.Choice("Style", 0, "Cross", "Dot", "Circle", "Cross + dot"));
        public final Setting.Num size = add(new Setting.Num("Size", 5, 2, 12, 1));
        public final Setting.Num gap = add(new Setting.Num("Gap", 2, 0, 8, 1));
        public final Setting.Bool dynamic = add(new Setting.Bool("Dynamic", true));
        public final Setting.Bool cooldown = add(new Setting.Bool("Attack indicator", true));
        public final Setting.Col color = add(new Setting.Col("Color", 0f, 1f));
        private float spread;

        public Crosshair() { super("Crosshair", "Custom crosshair: cross, dot or circle", Category.HUD); }

        public static Crosshair get() { return on(Crosshair.class); }

        public boolean draw(GuiGraphics ctx) {
            var mc = mc();
            if (mc.player == null || !mc.options.getCameraType().isFirstPerson() || mc.getDebugOverlay().showDebugScreen()) return false;
            int cx = ctx.guiWidth() / 2, cy = ctx.guiHeight() / 2;
            float move = (float) mc.player.getDeltaMovement().horizontalDistance();
            float target = dynamic.value ? Math.min(4, move * 12 + (mc.player.isSprinting() ? 1.5f : 0)) : 0;
            spread += (target - spread) * 0.25f;
            int c = 0xFF000000 | (color.light >= 0.99f && color.hue == 0f ? 0xFFFFFF : color.rgb());
            int g = gap.i() + Math.round(spread), s = size.i(), sh = 0x90000000;
            boolean cross = style.index == 0 || style.index == 3;
            if (cross) {
                bar(ctx, cx - g - s, cy, s, true, c, sh); bar(ctx, cx + g + 1, cy, s, true, c, sh);
                bar(ctx, cx, cy - g - s, s, false, c, sh); bar(ctx, cx, cy + g + 1, s, false, c, sh);
            }
            if (style.index == 1 || style.index == 3) { Ui.circle(ctx, cx, cy, 2, sh); ctx.fill(cx, cy, cx + 1, cy + 1, c); Ui.circle(ctx, cx, cy, 1, c); }
            if (style.index == 2) { Ui.outline(ctx, cx - s - g, cy - s - g, (s + g) * 2 + 1, (s + g) * 2 + 1, s + g, c); }
            if (cooldown.value) {
                float k = mc.player.getAttackStrengthScale(0);
                if (k < 1) {
                    int w = 16;
                    Ui.roundRect(ctx, cx - w / 2, cy + 10, w, 3, 1, 0x60000000);
                    Ui.roundRect(ctx, cx - w / 2, cy + 10, Math.max(2, Math.round(w * k)), 3, 1, c);
                }
            }
            return true;
        }

        private static void bar(GuiGraphics ctx, int x, int y, int len, boolean horizontal, int c, int shadow) {
            if (horizontal) { ctx.fill(x - 1, y - 1, x + len + 1, y + 2, shadow); ctx.fill(x, y, x + len, y + 1, c); }
            else { ctx.fill(x - 1, y - 1, x + 2, y + len + 1, shadow); ctx.fill(x, y, x + 1, y + len, c); }
        }
    }
}
