package dev.orbita.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Módulo de una línea al estilo Lunar: caja translúcida de 18 px con icono opcional y el texto centrado.
 * El texto se recalcula como mucho cada {@link #refreshMs} ms para no crear cadenas en cada fotograma.
 */
public abstract class TextHudModule extends HudModule {
    private static final int BOX_BG = 0x6F000000;
    private static final int MIN_BOX = 56;
    private static final float ICON_SCALE = 0.625f;

    private final long refreshMs;
    private String cached = "";
    private long lastRefresh;

    protected TextHudModule(String id, String name, String description, String glyph, Category category,
                            boolean enabledByDefault, int x, int y) {
        this(id, name, description, glyph, category, enabledByDefault, x, y, 100);
    }

    protected TextHudModule(String id, String name, String description, String glyph, Category category,
                            boolean enabledByDefault, int x, int y, long refreshMs) {
        super(id, name, description, glyph, category, enabledByDefault, x, y);
        this.refreshMs = refreshMs;
    }

    /** Texto a mostrar, sin corchetes (p. ej. "144 FPS"). */
    protected abstract String text(Minecraft mc);

    protected String currentText(Minecraft mc) {
        long now = System.currentTimeMillis();
        if (now - lastRefresh >= refreshMs) {
            lastRefresh = now;
            String t = text(mc);
            cached = brackets() ? "[" + t + "]" : t;
        }
        return cached;
    }

    @Override
    public void setBrackets(boolean brackets) {
        super.setBrackets(brackets);
        lastRefresh = 0;
    }

    @Override
    public void render(GuiGraphics g, Minecraft mc) {
        String t = currentText(mc);
        int tw = mc.font.width(t);
        int iconW = showIcon() ? 13 : 0;
        if (background()) {
            width = Math.max(tw + 10 + iconW, MIN_BOX);
            height = 18;
            g.fill(0, 0, width, height, BOX_BG);
            int content = tw + iconW;
            int x0 = (width - content) / 2;
            if (iconW > 0) {
                drawIcon(g, x0, 4);
            }
            g.drawString(mc.font, t, x0 + iconW, 5, textColor(), shadow());
        } else {
            width = tw + 1 + iconW;
            height = 10;
            if (iconW > 0) {
                drawIcon(g, 0, 0);
            }
            g.drawString(mc.font, t, iconW, 1, textColor(), shadow());
        }
    }

    private void drawIcon(GuiGraphics g, int x, int y) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(ICON_SCALE, ICON_SCALE);
        g.renderItem(iconStack(), 0, 0);
        g.pose().popMatrix();
    }
}
