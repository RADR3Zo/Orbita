package dev.orbita.client.ui.widget;

import dev.orbita.client.ui.anim.Animation;
import dev.orbita.client.ui.render.UiDraw;
import dev.orbita.client.ui.theme.OrbitaTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Interruptor moderno con perilla animada. */
public class ToggleSwitch extends AbstractWidget {
    private final BooleanSupplier getter;
    private final Consumer<Boolean> setter;
    private final Animation knob;
    private final Animation hover = new Animation(0f, 14f);

    public ToggleSwitch(int x, int y, BooleanSupplier getter, Consumer<Boolean> setter) {
        super(x, y, 26, 14, Component.empty());
        this.getter = getter;
        this.setter = setter;
        this.knob = new Animation(getter.getAsBoolean() ? 1f : 0f, 16f);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        setter.accept(!getter.getAsBoolean());
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        knob.setTarget(getter.getAsBoolean() ? 1f : 0f);
        float t = knob.update();
        hover.setTarget(isHoveredOrFocused() ? 1f : 0f);
        float h = hover.update();

        int x = getX();
        int y = getY();
        int w = getWidth();
        int ht = getHeight();
        if (h > 0.01f) {
            UiDraw.roundedRect(g, x - 2, y - 2, w + 4, ht + 4, 9, OrbitaTheme.withAlpha(OrbitaTheme.ACCENT_GLOW, h));
        }
        UiDraw.roundedRect(g, x, y, w, ht, 7, OrbitaTheme.lerpColor(0xFF2A2D3A, OrbitaTheme.ACCENT, t));
        int r = (ht - 4) / 2;
        int kx = x + 2 + r + Math.round((w - ht) * t);
        UiDraw.circle(g, kx, y + ht / 2, r, 0xFFFFFFFF);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
