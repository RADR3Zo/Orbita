package dev.orbita.launcher.ui;

import dev.orbita.launcher.Launcher;
import dev.orbita.launcher.core.auth.Account;
import dev.orbita.launcher.ui.components.Dialogs;
import dev.orbita.launcher.ui.components.OButton;
import dev.orbita.launcher.ui.components.OTextField;
import dev.orbita.launcher.ui.components.UiUtil;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

/**
 * Menú para cambiar de cuenta y añadir cuentas (premium de Microsoft y sin premium) desde cualquier sitio del
 * launcher: la página Jugar y la cuenta de la barra lateral.
 */
public final class AccountSwitcher {
    private static final int W = 300;

    private AccountSwitcher() {
    }

    /** Muestra el menú debajo (o encima, si below = false) del componente. */
    public static void show(Launcher launcher, JFrame frame, Component anchor, boolean below) {
        JPopupMenu popup = new JPopupMenu();
        popup.setBorder(BorderFactory.createEmptyBorder());
        popup.setOpaque(false);
        JPanel box = new JPanel() {
            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                Theme.hq(g);
                RoundRectangle2D r = new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 16, 16);
                g.setColor(Theme.SURFACE_SOLID);
                g.fill(r);
                g.setColor(Theme.alpha(Theme.ACCENT, 0.45f));
                g.draw(r);
                g.dispose();
            }
        };
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));

        JLabel title = UiUtil.label("CAMBIAR DE CUENTA", Font.BOLD, 11f, Theme.MUTED);
        title.setBorder(BorderFactory.createEmptyBorder(2, 8, 6, 8));
        box.add(UiUtil.left(title));
        List<Account> all = launcher.accounts();
        int active = launcher.config.activeAccount;
        for (int i = 0; i < all.size(); i++) {
            int index = i;
            Account a = all.get(i);
            box.add(UiUtil.left(new Row(a.name(), a.isMicrosoft() ? "Premium (Microsoft)" : "Sin premium", a.isMicrosoft(), i == active, false, () -> {
                popup.setVisible(false);
                launcher.selectAccount(index);
            })));
        }
        box.add(UiUtil.vgap(6));
        JComponent line = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(Theme.BORDER);
                g.fillRect(8, 0, getWidth() - 16, 1);
            }
        };
        line.setPreferredSize(new Dimension(W, 1));
        line.setMaximumSize(new Dimension(W, 1));
        box.add(UiUtil.left(line));
        box.add(UiUtil.vgap(6));
        box.add(UiUtil.left(new Row("Añadir cuenta premium", "Inicia sesión con Microsoft", true, false, true, () -> {
            popup.setVisible(false);
            MicrosoftWebLogin.start(launcher, frame);
        })));
        box.add(UiUtil.left(new Row("Añadir cuenta sin premium", "Solo un nombre de jugador", false, false, true, () -> {
            popup.setVisible(false);
            addOffline(launcher, frame);
        })));
        box.add(UiUtil.left(new Row("Administrar cuentas", "Ver, elegir y quitar cuentas", false, false, true, () -> {
            popup.setVisible(false);
            launcher.navigate.accept("accounts");
        })));
        popup.add(box);
        popup.pack();
        int h = popup.getPreferredSize().height;
        popup.show(anchor, 0, below ? anchor.getHeight() + 6 : -h - 6);
    }

    /** Pequeño diálogo para escribir el nombre de una cuenta sin premium. */
    public static void addOffline(Launcher launcher, JFrame frame) {
        JDialog d = Dialogs.base(frame);
        JPanel content = Dialogs.content();
        content.add(UiUtil.left(UiUtil.label("Añadir cuenta sin premium", Font.BOLD, 18f, Theme.TEXT)));
        content.add(UiUtil.vgap(8));
        content.add(UiUtil.left(UiUtil.muted("Un nombre de 3 a 16 letras, números o _.")));
        content.add(UiUtil.vgap(14));
        OTextField name = new OTextField("", "Nombre de jugador");
        name.setPreferredSize(new Dimension(360, 40));
        name.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        content.add(UiUtil.left(name));
        JLabel error = UiUtil.label(" ", Font.PLAIN, 12f, Theme.DANGER);
        content.add(UiUtil.vgap(6));
        content.add(UiUtil.left(error));
        content.add(UiUtil.vgap(12));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        OButton cancel = new OButton("Cancelar", OButton.Kind.GHOST);
        cancel.addActionListener(e -> d.dispose());
        OButton ok = new OButton("Añadir y usar", OButton.Kind.PRIMARY, Icons.Type.CHECK);
        Runnable add = () -> {
            String v = name.getText().trim();
            if (!v.matches("[A-Za-z0-9_]{3,16}")) {
                error.setText("El nombre debe tener entre 3 y 16 caracteres: letras, números o _.");
                return;
            }
            launcher.addAccount(Account.offline(v));
            d.dispose();
        };
        ok.addActionListener(e -> add.run());
        name.addActionListener(e -> add.run());
        buttons.add(cancel);
        buttons.add(ok);
        content.add(UiUtil.left(buttons));
        Dialogs.show(d, content, frame);
    }

    /** Fila del menú: cuenta (con su inicial) o acción (con un "+"). */
    private static final class Row extends JComponent {
        private final String name;
        private final String sub;
        private final boolean premium;
        private final boolean active;
        private final boolean action;
        private boolean hover;

        Row(String name, String sub, boolean premium, boolean active, boolean action, Runnable onClick) {
            this.name = name == null || name.isBlank() ? "?" : name;
            this.sub = sub;
            this.premium = premium;
            this.active = active;
            this.action = action;
            setPreferredSize(new Dimension(W, 46));
            setMaximumSize(new Dimension(W, 46));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    onClick.run();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.hq(g);
            int w = getWidth(), h = getHeight();
            if (hover || active) {
                g.setColor(active ? Theme.alpha(Theme.ACCENT, 0.18f) : new Color(255, 255, 255, 14));
                g.fill(new RoundRectangle2D.Float(0, 2, w, h - 4, 12, 12));
            }
            int cx = 10, cy = (h - 30) / 2;
            if (action) {
                g.setColor(Theme.alpha(Theme.ACCENT, 0.22f));
                g.fill(new Ellipse2D.Float(cx, cy, 30, 30));
                g.setColor(Theme.ACCENT);
                g.setFont(Theme.font(Font.BOLD, 18f));
                String plus = name.startsWith("Administrar") ? "…" : "+";
                FontMetrics fm = g.getFontMetrics();
                g.drawString(plus, cx + 15 - fm.stringWidth(plus) / 2, cy + 15 - fm.getHeight() / 2 + fm.getAscent());
            } else {
                g.setPaint(new GradientPaint(cx, cy, Theme.ACCENT, cx + 30, cy + 30, Theme.ACCENT_2));
                g.fill(new Ellipse2D.Float(cx, cy, 30, 30));
                g.setColor(Color.WHITE);
                g.setFont(Theme.font(Font.BOLD, 14f));
                String s = name.substring(0, 1).toUpperCase();
                FontMetrics fm = g.getFontMetrics();
                g.drawString(s, cx + 15 - fm.stringWidth(s) / 2, cy + 15 - fm.getHeight() / 2 + fm.getAscent());
            }
            g.setFont(Theme.font(Font.BOLD, 13f));
            g.setColor(Theme.TEXT);
            g.drawString(name, 50, h / 2 - 2);
            g.setFont(Theme.font(Font.PLAIN, 11f));
            g.setColor(!action && premium ? Theme.SUCCESS : Theme.MUTED);
            g.drawString(sub, 50, h / 2 + 13);
            if (active) {
                g.setFont(Theme.font(Font.BOLD, 11f));
                g.setColor(Theme.ACCENT);
                String u = "En uso";
                g.drawString(u, w - 12 - g.getFontMetrics().stringWidth(u), h / 2 + 4);
            }
            g.dispose();
        }
    }
}
