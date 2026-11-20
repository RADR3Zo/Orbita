package dev.orbita.client.ui.screen;

import dev.orbita.client.cosmetics.CapeRegistry;
import dev.orbita.client.cosmetics.CapeStyle;
import dev.orbita.client.ui.anim.Animation;
import dev.orbita.client.ui.render.SpaceScene;
import dev.orbita.client.ui.render.Starfield;
import dev.orbita.client.ui.render.UiDraw;
import dev.orbita.client.ui.theme.OrbitaFonts;
import dev.orbita.client.ui.theme.OrbitaTheme;
import dev.orbita.client.ui.widget.AccountCard;
import dev.orbita.client.ui.widget.CapeCard;
import dev.orbita.client.ui.widget.OrbitaButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.components.PlayerSkinWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Perfil y guardarropa: el jugador de pie sobre un planeta, la cuenta y las capas. */
public class ProfileScreen extends Screen {
    private static final int PANEL_W = 196;
    private static final int ACCOUNT_H = 40;
    private final Screen parent;
    private final Animation intro = new Animation(0f, 5f);
    private int panelX, accountY, capesY, capesH;
    private int playerCx, planetTop;
    private float planetR;

    public ProfileScreen(Screen parent) {
        super(Component.literal("Perfil"));
        this.parent = parent;
        intro.setTarget(1f);
    }

    @Override
    protected void init() {
        panelX = Math.max(14, (int) (width * 0.06f));
        accountY = 58;
        capesY = accountY + ACCOUNT_H + 8;

        addRenderableWidget(new OrbitaButton(panelX - 6, 8, 60, 14, OrbitaFonts.menu("‹  Volver"), b -> onClose())
                .style(OrbitaButton.Style.GHOST).noIntro());

        CapeStyle[] styles = CapeStyle.values();
        int gap = 5;
        int cols = 4;
        int cardW = (PANEL_W - 16 - gap * (cols - 1)) / cols;
        int rows = (styles.length + cols - 1) / cols;
        int available = height - capesY - 30 - 8;
        int cardH = Math.max(34, Math.min(64, (available - gap * (rows - 1)) / rows));
        for (int i = 0; i < styles.length; i++) {
            int x = panelX + 8 + (i % cols) * (cardW + gap);
            int y = capesY + 22 + (i / cols) * (cardH + gap);
            addRenderableWidget(new CapeCard(x, y, cardW, cardH, styles[i]));
        }
        capesH = 30 + rows * (cardH + gap);

        int panelRight = panelX + PANEL_W;
        playerCx = panelRight + (width - panelRight) / 2;
        planetR = Math.max(height * 0.62f, (width - panelRight) * 0.45f);
        planetTop = (int) (height + planetR * 0.45f - planetR);
        int ph = Math.max(80, Math.min(170, planetTop - 56));
        int pw = ph * 2 / 3;
        PlayerSkinWidget player = new PlayerSkinWidget(pw, ph, minecraft.getEntityModels(),
                () -> CapeRegistry.withCape(AccountCard.skin().get()));
        player.setPosition(playerCx - pw / 2, planetTop - ph + 8);
        addRenderableWidget(player);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        float time = dev.orbita.client.ui.anim.Animation.seconds();
        Starfield.SHARED.render(g, width, height, mouseX, mouseY);
        SpaceScene.fog(g, width, height, time);
        SpaceScene.moon(g, width * 0.93f, height * 0.80f, height * 0.08f);
        SpaceScene.planet(g, playerCx, height + planetR * 0.45f, planetR, -0.16f, time, false);
        SpaceScene.glow(g, playerCx, planetTop + 6, 34, 7, 0x90000000);

        float t = Animation.easeOutCubic(intro.update());
        UiDraw.roundedRect(g, panelX - 1, accountY - 1, PANEL_W + 2, ACCOUNT_H + 2, 7, OrbitaTheme.withAlpha(0x30FFFFFF, t));
        UiDraw.roundedRect(g, panelX, accountY, PANEL_W, ACCOUNT_H, 6, OrbitaTheme.withAlpha(0xD0101320, t));
        UiDraw.roundedRect(g, panelX - 1, capesY - 1, PANEL_W + 2, capesH + 2, 7, OrbitaTheme.withAlpha(0x30FFFFFF, t));
        UiDraw.roundedRect(g, panelX, capesY, PANEL_W, capesH, 6, OrbitaTheme.withAlpha(0xD0101320, t));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        String name = minecraft.getUser().getName();

        g.pose().pushMatrix();
        g.pose().translate(panelX, 26);
        g.pose().scale(2f, 2f);
        g.drawString(font, OrbitaFonts.menu("Perfil"), 0, 0, 0xFFFFFFFF, false);
        g.pose().popMatrix();
        g.drawString(font, OrbitaFonts.menu("Elige cómo te ven en el universo."), panelX, 46, OrbitaTheme.TEXT_MUTED, false);

        // Cuenta
        PlayerFaceRenderer.draw(g, AccountCard.skin().get(), panelX + 10, accountY + 10, 20);
        g.drawString(font, OrbitaFonts.bold(name), panelX + 38, accountY + 9, OrbitaTheme.TEXT, false);
        g.drawString(font, OrbitaFonts.menu(AccountCard.isMicrosoft() ? "Cuenta Microsoft" : "Modo offline"),
                panelX + 38, accountY + 21, OrbitaTheme.TEXT_MUTED, false);
        Component badge = OrbitaFonts.menu("En uso");
        int bw = font.width(badge) + 12;
        UiDraw.roundedRect(g, panelX + PANEL_W - bw - 8, accountY + 8, bw, 12, 6, 0x402FBF71);
        g.drawString(font, badge, panelX + PANEL_W - bw - 2, accountY + 10, 0xFF7CFFB2, false);

        // Capas
        g.drawString(font, OrbitaFonts.bold("Capas"), panelX + 10, capesY + 8, OrbitaTheme.TEXT, false);
        Component soon = OrbitaFonts.menu("Alas y emotes: pronto");
        g.drawString(font, soon, panelX + PANEL_W - 10 - font.width(soon), capesY + 8, 0xFF5F6478, false);

        // Nombre sobre el jugador
        Component title = OrbitaFonts.menu(name);
        float s = 1.8f;
        int tw = (int) (font.width(title) * s);
        int top = Math.max(26, planetTop - 196);
        g.pose().pushMatrix();
        g.pose().translate(playerCx - tw / 2f, top);
        g.pose().scale(s, s);
        g.drawString(font, title, 0, 0, 0xFFFFFFFF, false);
        g.pose().popMatrix();
        Component sub = OrbitaFonts.menu("Capa: " + CapeRegistry.selected().displayName + "  ·  arrastra para girar");
        g.drawString(font, sub, playerCx - font.width(sub) / 2, top + 20, OrbitaTheme.TEXT_MUTED, false);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
