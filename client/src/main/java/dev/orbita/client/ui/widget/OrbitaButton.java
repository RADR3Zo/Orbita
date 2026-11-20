package dev.orbita.client.ui.widget;

import dev.orbita.client.ui.anim.Animation;
import dev.orbita.client.ui.render.UiDraw;
import dev.orbita.client.ui.theme.OrbitaTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

import java.util.function.Consumer;

/** Botón de Órbita con varios estilos y animaciones de entrada y hover. */
public class OrbitaButton extends AbstractButton {
    public enum Style {
        /** Caja con borde y brillo. */
        PRIMARY,
        /** Solo texto, con fondo suave al pasar el ratón. */
        GHOST,
        /** Pestaña con barra lateral de selección. */
        TAB,
        /** Entrada del menú principal: texto grande alineado a la izquierda con píldora y punto al pasar el ratón. */
        MENU
    }

    private final Consumer<OrbitaButton> action;
    private final Animation hover = new Animation(0f, 14f);
    private final Animation select = new Animation(0f, 12f);
    private Style style = Style.PRIMARY;
    private boolean selected;
    private boolean muted;
    private float textScale = 1f;
    private long appearAt = Util.getMillis();

    public OrbitaButton(int x, int y, int w, int h, Component label, Consumer<OrbitaButton> action) {
        super(x, y, w, h, label);
        this.action = action;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        action.accept(this);
    }

    public OrbitaButton style(Style style) {
        this.style = style;
        return this;
    }

    public OrbitaButton selected(boolean selected) {
        this.selected = selected;
        select.snap(selected ? 1f : 0f);
        return this;
    }

    /** Texto atenuado (p. ej. "Salir del juego"). */
    public OrbitaButton muted(boolean muted) {
        this.muted = muted;
        return this;
    }

    public OrbitaButton textScale(float scale) {
        this.textScale = scale;
        return this;
    }

    /** Retrasa la animación de entrada (ms). */
    public OrbitaButton delay(int ms) {
        appearAt = Util.getMillis() + ms;
        return this;
    }

    public OrbitaButton noIntro() {
        appearAt = 0;
        return this;
    }

    @Override
    protected void renderContents(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        float intro = Animation.easeOutCubic((Util.getMillis() - appearAt) / 380f);
        if (intro <= 0f) {
            return;
        }
        hover.setTarget(isHoveredOrFocused() && active ? 1f : 0f);
        float h = hover.update();
        select.setTarget(selected ? 1f : 0f);
        float s = select.update();

        int x = getX();
        int y = getY();
        int w = getWidth();
        int ht = getHeight();
        Font font = Minecraft.getInstance().font;

        if (style == Style.MENU) {
            renderMenu(g, font, x - Math.round((1f - intro) * 14f), y, w, ht, h, intro);
            return;
        }
        y += Math.round((1f - intro) * 8f);

        switch (style) {
            case PRIMARY -> {
                if (h > 0.01f) {
                    UiDraw.roundedRect(g, x - 2, y - 2, w + 4, ht + 4, 5, OrbitaTheme.withAlpha(OrbitaTheme.ACCENT_GLOW, h * intro));
                }
                UiDraw.roundedRect(g, x - 1, y - 1, w + 2, ht + 2, 5,
                        OrbitaTheme.withAlpha(OrbitaTheme.lerpColor(OrbitaTheme.BORDER, OrbitaTheme.ACCENT, h), intro));
                UiDraw.roundedRect(g, x, y, w, ht, 4,
                        OrbitaTheme.withAlpha(OrbitaTheme.lerpColor(OrbitaTheme.PANEL_SOLID, 0xF0242838, h), intro));
            }
            case GHOST -> {
                if (h > 0.01f) {
                    UiDraw.roundedRect(g, x, y, w, ht, 4, OrbitaTheme.withAlpha(0x22FFFFFF, h * intro));
                }
            }
            case TAB -> {
                float k = Math.max(s, h * 0.5f);
                if (k > 0.01f) {
                    UiDraw.roundedRect(g, x, y, w, ht, 4, OrbitaTheme.withAlpha(0x308B7CFF, k * intro));
                }
                int barH = Math.round((ht - 8) * s);
                if (barH > 0) {
                    g.fill(x, y + (ht - barH) / 2, x + 2, y + (ht + barH) / 2, OrbitaTheme.withAlpha(OrbitaTheme.ACCENT, intro));
                }
            }
            default -> {
            }
        }

        int textColor = active
                ? OrbitaTheme.lerpColor(OrbitaTheme.TEXT_MUTED, OrbitaTheme.TEXT, Math.max(Math.max(h, s), style == Style.PRIMARY ? 0.65f : 0f))
                : OrbitaTheme.TEXT_DISABLED;
        textColor = OrbitaTheme.withAlpha(textColor, intro);
        int tw = font.width(getMessage());
        int ty = y + (ht - 8) / 2;
        int tx = style == Style.TAB ? x + 9 : x + (w - tw) / 2;
        g.drawString(font, getMessage(), tx, ty, textColor, false);
    }

    private void renderMenu(GuiGraphics g, Font font, int x, int y, int w, int ht, float h, float intro) {
        int tw = Math.round(font.width(getMessage()) * textScale);
        int pillW = tw + 22 + Math.round(h * 4f);
        if (h > 0.01f) {
            // Halo de luz detrás de la entrada y flecha que se desliza.
            dev.orbita.client.ui.render.SpaceScene.glow(g, x + pillW * 0.35f, y + ht / 2f, pillW * 0.75f, ht * 1.3f,
                    OrbitaTheme.withAlpha(0x388B7CFF, h * intro));
            g.fillGradient(x + 1, y + ht - 1, x + Math.round(pillW * h), y + ht, OrbitaTheme.withAlpha(OrbitaTheme.ACCENT, h * intro),
                    OrbitaTheme.withAlpha(OrbitaTheme.ACCENT_2, h * intro));
            g.drawString(font, "›", x + pillW + 2 + Math.round(h * 3f), y + (ht - 8) / 2, OrbitaTheme.withAlpha(OrbitaTheme.ACCENT, h * intro), false);
            UiDraw.roundedRect(g, x, y, pillW, ht, ht / 2, OrbitaTheme.withAlpha(0x2C1E2240, h * intro));
            UiDraw.roundedRect(g, x, y, pillW, ht, ht / 2, OrbitaTheme.withAlpha(0x14FFFFFF, h * intro));
            UiDraw.circle(g, x + 8, y + ht / 2, 2, OrbitaTheme.withAlpha(OrbitaTheme.ACCENT, h * intro));
        }
        int base = muted ? 0xFF8A8FA3 : 0xFFD9DBE6;
        int color = OrbitaTheme.withAlpha(OrbitaTheme.lerpColor(base, 0xFFFFFFFF, h), intro);
        g.pose().pushMatrix();
        g.pose().translate(x + 12 + h * 4f, y + (ht - 9 * textScale) / 2f);
        g.pose().scale(textScale, textScale);
        g.drawString(font, getMessage(), 0, 0, color, false);
        g.pose().popMatrix();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
