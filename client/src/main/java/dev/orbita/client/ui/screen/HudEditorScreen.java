package dev.orbita.client.ui.screen;

import dev.orbita.client.hud.HudManager;
import dev.orbita.client.hud.HudModule;
import dev.orbita.client.ui.theme.OrbitaTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * Editor de HUD: arrastra los módulos, usa la rueda para cambiar su escala y clic derecho para sus opciones.
 * El HUD se dibuja debajo (HudManager).
 */
public class HudEditorScreen extends Screen {
    private static final int SNAP = 4;
    private final Screen parent;
    @Nullable
    private HudModule dragging;
    private int offsetX;
    private int offsetY;

    public HudEditorScreen(Screen parent) {
        super(Component.literal("Editor de HUD"));
        this.parent = parent;
    }

    @Nullable
    private HudModule at(double mx, double my) {
        List<HudModule> modules = HudManager.get().modules();
        for (int i = modules.size() - 1; i >= 0; i--) {
            HudModule m = modules.get(i);
            if (m.isEnabled() && m.hasHud() && !m.freeform() && m.contains(mx, my)) {
                return m;
            }
        }
        return null;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0x40000000);
        for (int x = 0; x < width; x += 20) {
            g.fill(x, 0, x + 1, height, 0x0CFFFFFF);
        }
        for (int y = 0; y < height; y += 20) {
            g.fill(0, y, width, y + 1, 0x0CFFFFFF);
        }
        g.fill(width / 2, 0, width / 2 + 1, height, 0x308B7CFF);
        g.fill(0, height / 2, width, height / 2 + 1, 0x308B7CFF);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        HudModule hovered = dragging != null ? dragging : at(mouseX, mouseY);
        for (HudModule m : HudManager.get().modules()) {
            if (!m.isEnabled() || !m.hasHud() || m.freeform()) {
                continue;
            }
            boolean active = m == hovered;
            g.renderOutline(m.lastX() - 1, m.lastY() - 1, m.scaledWidth() + 2, m.scaledHeight() + 2,
                    active ? OrbitaTheme.ACCENT : 0x60FFFFFF);
            if (active) {
                String label = m.name() + "  " + String.format(Locale.ROOT, "%.2fx", m.scale());
                int ly = m.lastY() > 12 ? m.lastY() - 11 : m.lastY() + m.scaledHeight() + 3;
                g.drawString(font, label, m.lastX(), ly, OrbitaTheme.ACCENT, true);
            }
        }
        String hint = "Arrastra para mover · Rueda: escala · Clic derecho: opciones · ESC: guardar";
        g.drawString(font, hint, (width - font.width(hint)) / 2, height - 40, OrbitaTheme.TEXT, true);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        HudModule m = at(event.x(), event.y());
        if (m != null && event.button() == 1) {
            HudManager.get().save();
            minecraft.setScreen(new ControlPanelScreen(this, m));
            return true;
        }
        if (m != null && event.button() == 0) {
            dragging = m;
            offsetX = (int) event.x() - m.lastX();
            offsetY = (int) event.y() - m.lastY();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (dragging == null) {
            return super.mouseDragged(event, dragX, dragY);
        }
        int w = dragging.scaledWidth();
        int h = dragging.scaledHeight();
        int nx = Mth.clamp((int) event.x() - offsetX, 0, Math.max(0, width - w));
        int ny = Mth.clamp((int) event.y() - offsetY, 0, Math.max(0, height - h));
        if (nx < SNAP) nx = 0;
        if (ny < SNAP) ny = 0;
        if (width - w - nx < SNAP) nx = width - w;
        if (height - h - ny < SNAP) ny = height - h;
        int center = (width - w) / 2;
        if (Math.abs(nx - center) < SNAP) nx = center;
        dragging.setPosition(nx, ny);
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging != null) {
            dragging = null;
            HudManager.get().save();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        HudModule m = at(mouseX, mouseY);
        if (m != null) {
            m.setScale(m.scale() + (scrollY > 0 ? 0.05f : -0.05f));
            HudManager.get().save();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        HudManager.get().save();
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
