package dev.orbita.client.ui.widget;

import dev.orbita.client.ui.anim.Animation;
import dev.orbita.client.ui.render.UiDraw;
import dev.orbita.client.ui.theme.OrbitaFonts;
import dev.orbita.client.ui.theme.OrbitaTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.function.Supplier;

/** Tarjeta de cuenta (cara del jugador, nombre y tipo) en la esquina inferior izquierda del menú. */
public class AccountCard extends AbstractWidget {
    private static Supplier<PlayerSkin> skin;
    private final Runnable onClick;
    private final Animation hover = new Animation(0f, 14f);

    public AccountCard(int x, int y, int w, int h, Runnable onClick) {
        super(x, y, w, h, Component.literal("Perfil"));
        this.onClick = onClick;
    }

    public static Supplier<PlayerSkin> skin() {
        if (skin == null) {
            Minecraft mc = Minecraft.getInstance();
            skin = mc.getSkinManager().createLookup(mc.getGameProfile(), false);
        }
        return skin;
    }

    /** Tras cambiar de cuenta: la próxima vez se busca la skin de la cuenta nueva. */
    public static void resetSkin() {
        skin = null;
    }

    public static boolean isMicrosoft() {
        // El launcher pasa el token "0" en modo offline.
        String token = Minecraft.getInstance().getUser().getAccessToken();
        return token != null && !token.isEmpty() && !"0".equals(token);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        onClick.run();
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        hover.setTarget(isHoveredOrFocused() ? 1f : 0f);
        float h = hover.update();
        int x = getX();
        int y = getY();
        int w = getWidth();
        int ht = getHeight();
        UiDraw.roundedRect(g, x - 1, y - 1, w + 2, ht + 2, ht / 2 + 1,
                OrbitaTheme.lerpColor(0x30FFFFFF, OrbitaTheme.ACCENT, h * 0.8f));
        UiDraw.roundedRect(g, x, y, w, ht, ht / 2, OrbitaTheme.lerpColor(0xD0121520, 0xE01B1F2E, h));
        int face = ht - 10;
        PlayerFaceRenderer.draw(g, skin().get(), x + 6, y + 5, face);
        Minecraft mc = Minecraft.getInstance();
        g.drawString(mc.font, OrbitaFonts.bold(mc.getUser().getName()), x + face + 12, y + 5, OrbitaTheme.TEXT, false);
        g.drawString(mc.font, OrbitaFonts.menu(isMicrosoft() ? "Premium (Microsoft)" : "Sin premium"),
                x + face + 12, y + 16, isMicrosoft() ? 0xFF7CFFB2 : OrbitaTheme.TEXT_MUTED, false);
        // flechita: al pulsar se abre el apartado de cuentas
        int ax = x + w - 14, ay = y + ht / 2;
        int c = OrbitaTheme.lerpColor(OrbitaTheme.TEXT_MUTED, OrbitaTheme.ACCENT, h);
        g.fill(ax - 3, ay - 1, ax - 2, ay, c);
        g.fill(ax - 2, ay, ax - 1, ay + 1, c);
        g.fill(ax - 1, ay + 1, ax, ay + 2, c);
        g.fill(ax, ay, ax + 1, ay + 1, c);
        g.fill(ax + 1, ay - 1, ax + 2, ay, c);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
