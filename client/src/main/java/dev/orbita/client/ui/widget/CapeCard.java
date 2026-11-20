package dev.orbita.client.ui.widget;

import dev.orbita.client.cosmetics.CapeRegistry;
import dev.orbita.client.cosmetics.CapeStyle;
import dev.orbita.client.ui.anim.Animation;
import dev.orbita.client.ui.render.UiDraw;
import dev.orbita.client.ui.theme.OrbitaTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Tarjeta seleccionable del guardarropa con vista previa de la capa. */
public class CapeCard extends AbstractWidget {
    private final CapeStyle style;
    private final Animation hover = new Animation(0f, 14f);
    private final Animation selected = new Animation(0f, 12f);

    public CapeCard(int x, int y, int w, int h, CapeStyle style) {
        super(x, y, w, h, Component.literal(style.displayName));
        this.style = style;
        selected.snap(CapeRegistry.selected() == style ? 1f : 0f);
    }

    @Override
    public void onClick(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        CapeRegistry.select(style);
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        hover.setTarget(isHoveredOrFocused() ? 1f : 0f);
        selected.setTarget(CapeRegistry.selected() == style ? 1f : 0f);
        float h = hover.update();
        float s = selected.update();

        int x = getX();
        int y = getY() - Math.round(h * 2f);
        int w = getWidth();
        int ht = getHeight();
        Font font = Minecraft.getInstance().font;

        UiDraw.roundedRect(g, x - 1, y - 1, w + 2, ht + 2, 4,
                OrbitaTheme.lerpColor(OrbitaTheme.BORDER, OrbitaTheme.ACCENT, Math.max(s, h * 0.6f)));
        UiDraw.roundedRect(g, x, y, w, ht, 3, OrbitaTheme.lerpColor(OrbitaTheme.CARD, 0xF0242838, h));

        int ph = Math.max(16, ht - 18);
        int pw = ph * 10 / 16;
        int px = x + (w - pw) / 2;
        int py = y + 5;
        Identifier tex = CapeRegistry.texture(style);
        if (tex != null) {
            // Cara frontal de la capa: u=1, v=1, 10x16 px en una textura de 64x32.
            g.blit(RenderPipelines.GUI_TEXTURED, tex, px, py, 1f, 1f, pw, ph, 10, 16, 64, 32);
        } else {
            UiDraw.roundedRect(g, px, py, pw, ph, 2, 0x40FFFFFF);
            g.drawString(font, "—", px + (pw - font.width("—")) / 2, py + ph / 2 - 4, OrbitaTheme.TEXT_MUTED, false);
        }
        String name = style.displayName;
        int color = OrbitaTheme.lerpColor(OrbitaTheme.TEXT_MUTED, OrbitaTheme.TEXT, Math.max(s, h));
        float ts = Math.min(0.75f, (w - 4) / (float) Math.max(1, font.width(name)));
        g.pose().pushMatrix();
        g.pose().translate(x + w / 2f - font.width(name) * ts / 2f, y + ht - 10);
        g.pose().scale(ts, ts);
        g.drawString(font, name, 0, 0, color, false);
        g.pose().popMatrix();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
