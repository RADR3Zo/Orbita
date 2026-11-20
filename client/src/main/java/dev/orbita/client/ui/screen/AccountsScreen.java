package dev.orbita.client.ui.screen;

import dev.orbita.client.account.Accounts;
import dev.orbita.client.account.MicrosoftLogin;
import dev.orbita.client.account.SessionSwitcher;
import dev.orbita.client.ui.anim.Animation;
import dev.orbita.client.ui.render.UiDraw;
import dev.orbita.client.ui.theme.OrbitaFonts;
import dev.orbita.client.ui.theme.OrbitaTheme;
import dev.orbita.client.ui.widget.AccountCard;
import dev.orbita.client.ui.widget.OrbitaButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

import java.net.URI;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Apartado de cuentas del menú principal (se despliega bajo la tarjeta de arriba a la izquierda):
 * elegir con qué cuenta jugar, quitar cuentas y añadir cuentas premium (Microsoft) o sin premium.
 * Las cuentas son las mismas que las del launcher.
 */
public class AccountsScreen extends Screen {
    private static final int PX = 12;
    private static final int PY = 44;
    private static final int PW = 270;

    private final Screen parent;
    private final Animation open = new Animation(0f, 12f);
    private int ph;
    private String status = "";
    private int statusColor = OrbitaTheme.TEXT_MUTED;
    private boolean busy;
    /** Inicio de sesión premium en curso (muestra el código), o null. */
    private MicrosoftLogin.DeviceCode code;
    private AtomicBoolean cancel;
    private EditBox nameBox;
    private boolean addingOffline;

    public AccountsScreen(Screen parent) {
        super(Component.literal("Cuentas"));
        this.parent = parent;
        open.setTarget(1f);
    }

    @Override
    protected void init() {
        int y = PY + 26;
        List<Accounts.Acc> list = Accounts.list();
        int active = Accounts.active();
        for (int i = 0; i < list.size(); i++) {
            addRenderableWidget(new Row(PX + 8, y, PW - 16, 30, list.get(i), i, i == active, list.size() > 1));
            y += 32;
        }
        y += 8;
        if (code != null) {
            addRenderableWidget(new OrbitaButton(PX + 10, y + 48, PW - 20, 18,
                    OrbitaFonts.menu("Copiar código y abrir microsoft.com/link"), b -> openBrowser()).style(OrbitaButton.Style.PRIMARY).noIntro());
            addRenderableWidget(new OrbitaButton(PX + 10, y + 70, PW - 20, 16, OrbitaFonts.menu("Cancelar"), b -> {
                cancel.set(true);
                code = null;
                setStatus("", OrbitaTheme.TEXT_MUTED);
                rebuildWidgets();
            }).style(OrbitaButton.Style.GHOST).noIntro());
            y += 92;
        } else if (addingOffline) {
            nameBox = new EditBox(font, PX + 10, y, PW - 20 - 66, 18, Component.literal("Nombre"));
            nameBox.setMaxLength(16);
            nameBox.setHint(Component.literal("Nombre de jugador").withColor(0xFF5F6478));
            addRenderableWidget(nameBox);
            setInitialFocus(nameBox);
            addRenderableWidget(new OrbitaButton(PX + PW - 72, y, 62, 18, OrbitaFonts.menu("Añadir"), b -> addOffline())
                    .style(OrbitaButton.Style.PRIMARY).noIntro());
            addRenderableWidget(new OrbitaButton(PX + 10, y + 22, PW - 20, 16, OrbitaFonts.menu("Cancelar"), b -> {
                addingOffline = false;
                rebuildWidgets();
            }).style(OrbitaButton.Style.GHOST).noIntro());
            y += 42;
        } else {
            addRenderableWidget(new OrbitaButton(PX + 10, y, PW - 20, 18, OrbitaFonts.menu("+  Añadir cuenta premium (Microsoft)"),
                    b -> startPremium()).style(OrbitaButton.Style.PRIMARY).noIntro());
            addRenderableWidget(new OrbitaButton(PX + 10, y + 22, PW - 20, 18, OrbitaFonts.menu("+  Añadir cuenta sin premium"), b -> {
                addingOffline = true;
                rebuildWidgets();
            }).style(OrbitaButton.Style.GHOST).noIntro());
            addRenderableWidget(new OrbitaButton(PX + 10, y + 44, PW - 20, 16, OrbitaFonts.menu("Perfil y capas"),
                    b -> minecraft.setScreen(new ProfileScreen(this))).style(OrbitaButton.Style.GHOST).noIntro());
            y += 64;
        }
        ph = y - PY + 20;
    }

    private void setStatus(String s, int color) {
        status = s;
        statusColor = color;
    }

    // ------------------------------------------------------------------ acciones

    private void select(int index) {
        if (busy) {
            return;
        }
        busy = true;
        setStatus("Cambiando de cuenta…", OrbitaTheme.TEXT_MUTED);
        SessionSwitcher.switchTo(index, s -> minecraft.execute(() -> setStatus(s, OrbitaTheme.TEXT_MUTED)), err -> {
            busy = false;
            if (err == null) {
                setStatus("Ahora juegas como " + minecraft.getUser().getName(), 0xFF7CFFB2);
            } else {
                setStatus(err, 0xFFFF7C7C);
            }
            rebuildWidgets();
        });
    }

    private void remove(int index) {
        if (busy) {
            return;
        }
        if (index == Accounts.active()) {
            setStatus("Cambia primero a otra cuenta para quitar esta.", 0xFFFFD27C);
            return;
        }
        Accounts.remove(index);
        setStatus("Cuenta quitada", OrbitaTheme.TEXT_MUTED);
        rebuildWidgets();
    }

    private void addOffline() {
        String v = nameBox.getValue().trim();
        if (!v.matches("[A-Za-z0-9_]{3,16}")) {
            setStatus("De 3 a 16 letras, números o _", 0xFFFF7C7C);
            return;
        }
        addingOffline = false;
        int i = Accounts.add(Accounts.Acc.offline(v));
        rebuildWidgets();
        select(i);
    }

    private void startPremium() {
        if (busy) {
            return;
        }
        busy = true;
        cancel = new AtomicBoolean();
        AtomicBoolean myCancel = cancel;
        setStatus("Pidiendo un código a Microsoft…", OrbitaTheme.TEXT_MUTED);
        Util.backgroundExecutor().execute(() -> {
            try {
                MicrosoftLogin.DeviceCode dc = MicrosoftLogin.requestCode();
                minecraft.execute(() -> {
                    code = dc;
                    setStatus("Esperando que confirmes en el navegador…", OrbitaTheme.TEXT_MUTED);
                    rebuildWidgets();
                });
                Accounts.Acc acc = MicrosoftLogin.waitForAccount(dc, myCancel::get,
                        s -> minecraft.execute(() -> setStatus(s, OrbitaTheme.TEXT_MUTED)));
                int i = Accounts.add(acc);
                minecraft.execute(() -> {
                    code = null;
                    busy = false;
                    rebuildWidgets();
                    select(i);
                });
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : e.toString();
                minecraft.execute(() -> {
                    busy = false;
                    code = null;
                    if (!myCancel.get()) {
                        setStatus(msg, 0xFFFF7C7C);
                    }
                    rebuildWidgets();
                });
            }
        });
    }

    private void openBrowser() {
        if (code == null) {
            return;
        }
        minecraft.keyboardHandler.setClipboard(code.userCode());
        try {
            Util.getPlatform().openUri(URI.create(code.verificationUri()));
        } catch (Exception ignored) {
            // la dirección está escrita en pantalla
        }
        setStatus("Código copiado: pégalo en la página de Microsoft", 0xFF7CFFB2);
    }

    // ------------------------------------------------------------------ dibujo

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        parent.renderBackground(g, -1, -1, partialTick);
        parent.render(g, -1, -1, partialTick);
        g.fill(0, 0, width, height, 0x66000000);
        float t = Animation.easeOutCubic(open.update());
        int h = Math.round(ph * t);
        UiDraw.roundedRect(g, PX - 1, PY - 1, PW + 2, h + 2, 9, OrbitaTheme.withAlpha(0x668B7CFF, t));
        UiDraw.roundedRect(g, PX, PY, PW, h, 8, OrbitaTheme.withAlpha(0xF0101219, t));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawString(font, OrbitaFonts.bold("CUENTAS"), PX + 12, PY + 10, OrbitaTheme.ACCENT, false);
        g.drawString(font, OrbitaFonts.menu("premium y sin premium"), PX + 12 + font.width(OrbitaFonts.bold("CUENTAS")) + 6,
                PY + 10, OrbitaTheme.TEXT_MUTED, false);
        if (code != null) {
            int y = PY + 26 + Accounts.list().size() * 32 + 8;
            g.drawString(font, OrbitaFonts.menu("En microsoft.com/link escribe este código:"), PX + 12, y, OrbitaTheme.TEXT, false);
            g.pose().pushMatrix();
            g.pose().translate(PX + PW / 2f, y + 14);
            g.pose().scale(2f, 2f);
            String c = code.userCode();
            g.drawString(font, OrbitaFonts.bold(c), -font.width(OrbitaFonts.bold(c)) / 2, 0, 0xFFFFFFFF, false);
            g.pose().popMatrix();
        }
        if (!status.isEmpty()) {
            g.drawString(font, OrbitaFonts.menu(status), PX + 12, PY + ph - 14, statusColor, false);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (event.x() < PX || event.x() > PX + PW || event.y() < PY - 34 || event.y() > PY + ph) {
            onClose(); // clic fuera: se cierra
            return true;
        }
        return false;
    }

    @Override
    public void onClose() {
        if (cancel != null) {
            cancel.set(true);
        }
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Fila de una cuenta: clic para usarla; ✕ para quitarla. */
    private final class Row extends AbstractWidget {
        private final Accounts.Acc acc;
        private final int index;
        private final boolean active;
        private final boolean removable;
        private final Animation hover = new Animation(0f, 14f);

        Row(int x, int y, int w, int h, Accounts.Acc acc, int index, boolean active, boolean removable) {
            super(x, y, w, h, Component.literal(acc.name()));
            this.acc = acc;
            this.index = index;
            this.active = active;
            this.removable = removable;
        }

        private boolean overX(double mx, double my) {
            return removable && !active && mx >= getX() + getWidth() - 20 && mx <= getX() + getWidth() - 4
                    && my >= getY() + 7 && my <= getY() + getHeight() - 7;
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean doubleClick) {
            if (overX(event.x(), event.y())) {
                remove(index);
            } else if (!active) {
                select(index);
            }
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            hover.setTarget(isHovered() ? 1f : 0f);
            float h = hover.update();
            int x = getX(), y = getY(), w = getWidth(), ht = getHeight();
            int bg = active ? 0x308B7CFF : OrbitaTheme.withAlpha(0x18FFFFFF, h);
            if (active || h > 0.01f) {
                UiDraw.roundedRect(g, x, y, w, ht, 7, bg);
            }
            if (active) {
                PlayerFaceRenderer.draw(g, AccountCard.skin().get(), x + 6, y + 5, 20);
            } else {
                UiDraw.circle(g, x + 16, y + 15, 10, acc.premium() ? 0xFF2F6E4C : 0xFF3A3360);
                String ini = acc.name().substring(0, 1).toUpperCase();
                g.drawString(font, ini, x + 16 - font.width(ini) / 2, y + 11, 0xFFFFFFFF, false);
            }
            g.drawString(font, OrbitaFonts.bold(acc.name()), x + 32, y + 6, OrbitaTheme.TEXT, false);
            g.drawString(font, OrbitaFonts.menu(acc.premium() ? "Premium (Microsoft)" : "Sin premium"), x + 32, y + 17,
                    acc.premium() ? 0xFF7CFFB2 : OrbitaTheme.TEXT_MUTED, false);
            if (active) {
                String u = "En uso";
                g.drawString(font, OrbitaFonts.bold(u), x + w - 8 - font.width(OrbitaFonts.bold(u)), y + 11, OrbitaTheme.ACCENT, false);
            } else {
                String u = "Usar";
                int ux = x + w - (removable ? 26 : 8) - font.width(OrbitaFonts.menu(u));
                g.drawString(font, OrbitaFonts.menu(u), ux, y + 11, OrbitaTheme.withAlpha(OrbitaTheme.ACCENT, 0.5f + h * 0.5f), false);
                if (removable) {
                    boolean ox = overX(mouseX, mouseY);
                    g.drawString(font, "✕", x + w - 16, y + 11, ox ? 0xFFFF7C7C : OrbitaTheme.TEXT_MUTED, false);
                }
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
