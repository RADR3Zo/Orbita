package dev.orbita.client.hud.modules;

import dev.orbita.client.feature.CpsTracker;
import dev.orbita.client.hud.HudModule;
import dev.orbita.client.ui.anim.Animation;
import dev.orbita.client.ui.theme.OrbitaTheme;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Keystrokes estilo Lunar: cajas translúcidas que se iluminan al pulsar. */
public final class KeystrokesModule extends HudModule {
    private static final int KEY = 22;
    private static final int GAP = 2;
    private static final int IDLE_BG = 0x6F000000;
    private static final int PRESSED_BG = 0xD0FFFFFF;

    private final Animation[] anims = new Animation[7];

    public KeystrokesModule() {
        super("keystrokes", "Keystrokes", "WASD, clics con CPS y espacio", "WASD", Category.HUD, true, 4, 112);
        icon(net.minecraft.world.item.Items.CRAFTER);
        for (int i = 0; i < anims.length; i++) {
            anims[i] = new Animation(0f, 22f);
        }
    }

    @Override
    public boolean hasTextStyle() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, Minecraft mc) {
        width = KEY * 3 + GAP * 2;
        int row2 = KEY + GAP;
        int row3 = row2 + KEY + GAP;
        int row4 = row3 + 20 + GAP;
        height = row4 + 10;

        key(g, mc, KEY + GAP, 0, KEY, KEY, mc.options.keyUp, 0);
        key(g, mc, 0, row2, KEY, KEY, mc.options.keyLeft, 1);
        key(g, mc, KEY + GAP, row2, KEY, KEY, mc.options.keyDown, 2);
        key(g, mc, 2 * (KEY + GAP), row2, KEY, KEY, mc.options.keyRight, 3);

        int half = (width - GAP) / 2;
        mouse(g, mc, 0, row3, half, "LMB", CpsTracker.left(), mc.options.keyAttack.isDown(), 4);
        mouse(g, mc, half + GAP, row3, width - half - GAP, "RMB", CpsTracker.right(), mc.options.keyUse.isDown(), 5);

        float t = press(6, mc.options.keyJump.isDown());
        g.fill(0, row4, width, row4 + 10, bg(t));
        int barW = width / 3;
        g.fill((width - barW) / 2, row4 + 4, (width + barW) / 2, row4 + 6, fg(t));
    }

    private int bg(float t) {
        return OrbitaTheme.lerpColor(background() ? IDLE_BG : 0x00000000, PRESSED_BG, t);
    }

    private int fg(float t) {
        return OrbitaTheme.lerpColor(textColor(), 0xFF000000, t);
    }

    private void key(GuiGraphics g, Minecraft mc, int x, int y, int w, int h, KeyMapping mapping, int index) {
        float t = press(index, mapping.isDown());
        g.fill(x, y, x + w, y + h, bg(t));
        String label = label(mc, mapping, index, w);
        g.drawString(mc.font, label, x + (w - mc.font.width(label)) / 2 + 1, y + (h - 8) / 2, fg(t), shadow() && t < 0.5f);
    }

    private void mouse(GuiGraphics g, Minecraft mc, int x, int y, int w, String label, int cps, boolean down, int index) {
        float t = press(index, down);
        g.fill(x, y, x + w, y + 20, bg(t));
        g.drawString(mc.font, label, x + (w - mc.font.width(label)) / 2 + 1, y + 2, fg(t), shadow() && t < 0.5f);
        String c = cps + " CPS";
        g.pose().pushMatrix();
        g.pose().translate(x + w / 2f, y + 12);
        g.pose().scale(0.7f, 0.7f);
        g.drawString(mc.font, c, -mc.font.width(c) / 2, 0, OrbitaTheme.withAlpha(fg(t), 0.8f), false);
        g.pose().popMatrix();
    }

    private final String[] labels = new String[4];
    private long labelsAt;

    /** Nombre de la tecla asignada, recalculado como mucho una vez por segundo (por si se reasigna). */
    private String label(Minecraft mc, KeyMapping mapping, int index, int w) {
        long now = System.currentTimeMillis();
        if (now - labelsAt > 1000) {
            labelsAt = now;
            java.util.Arrays.fill(labels, null);
        }
        if (labels[index] == null) {
            String l = mapping.getTranslatedKeyMessage().getString();
            labels[index] = mc.font.width(l) > w - 4 ? l.substring(0, 1) : l;
        }
        return labels[index];
    }

    private float press(int index, boolean down) {
        anims[index].setTarget(down ? 1f : 0f);
        return anims[index].update();
    }
}
