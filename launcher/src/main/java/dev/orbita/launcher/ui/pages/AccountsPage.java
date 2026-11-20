package dev.orbita.launcher.ui.pages;

import dev.orbita.launcher.Launcher;
import dev.orbita.launcher.core.auth.Account;
import dev.orbita.launcher.ui.Icons;
import dev.orbita.launcher.ui.MicrosoftWebLogin;
import dev.orbita.launcher.ui.Theme;
import dev.orbita.launcher.ui.components.Card;
import dev.orbita.launcher.ui.components.OButton;
import dev.orbita.launcher.ui.components.OTextField;
import dev.orbita.launcher.ui.components.UiUtil;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.geom.Ellipse2D;
import java.util.List;

/** Página Cuentas: varias cuentas sin premium y de Microsoft; eliges con cuál jugar. */
public final class AccountsPage extends JPanel {
    private final Launcher launcher;
    private final JFrame frame;
    private final JPanel list = new JPanel();
    private final JLabel error = UiUtil.label(" ", Font.PLAIN, 12f, Theme.DANGER);

    public AccountsPage(Launcher launcher, JFrame frame) {
        this.launcher = launcher;
        this.frame = frame;
        setOpaque(false);
        setLayout(new BorderLayout(0, 18));
        setBorder(BorderFactory.createEmptyBorder(4, 40, 30, 40));

        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        JLabel eyebrow = new UiUtil.AccentLabel("TUS CUENTAS");
        eyebrow.setFont(Theme.tracked(Font.BOLD, 12f, 0.18f));
        head.add(UiUtil.left(eyebrow));
        head.add(UiUtil.vgap(6));
        head.add(UiUtil.left(UiUtil.label("Cuentas", Font.BOLD, 34f, Theme.TEXT)));
        head.add(UiUtil.vgap(4));
        head.add(UiUtil.left(UiUtil.muted("Guarda varias cuentas y elige con cuál jugar. Las de Microsoft entran a servidores premium con tu skin.")));
        add(head, BorderLayout.NORTH);

        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setOpaque(false);
        body.add(list, BorderLayout.NORTH);

        // ---- añadir
        JPanel add = new JPanel(new GridLayout(1, 2, 14, 0));
        add.setOpaque(false);

        Card offline = new Card(null, 14);
        offline.setLayout(new BoxLayout(offline, BoxLayout.Y_AXIS));
        offline.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        offline.add(UiUtil.left(UiUtil.label("Añadir cuenta sin premium", Font.BOLD, 15f, Theme.TEXT)));
        offline.add(UiUtil.vgap(4));
        offline.add(UiUtil.left(UiUtil.muted("Un nombre de 3 a 16 letras, números o _.")));
        offline.add(UiUtil.vgap(12));
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        OTextField name = new OTextField("", "Nombre de jugador");
        name.setPreferredSize(new Dimension(180, 40));
        OButton addOffline = new OButton("Añadir", OButton.Kind.PRIMARY, Icons.Type.CHECK);
        Runnable doAdd = () -> {
            String v = name.getText().trim();
            if (!v.matches("[A-Za-z0-9_]{3,16}")) {
                error.setText("El nombre debe tener entre 3 y 16 caracteres: letras, números o _.");
                return;
            }
            error.setText(" ");
            launcher.addAccount(Account.offline(v));
            name.setText("");
        };
        addOffline.addActionListener(e -> doAdd.run());
        name.addActionListener(e -> doAdd.run());
        row.add(name, BorderLayout.CENTER);
        row.add(addOffline, BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        offline.add(UiUtil.left(row));
        offline.add(UiUtil.vgap(8));
        offline.add(UiUtil.left(error));
        add.add(offline);

        Card ms = new Card(null, 14);
        ms.setLayout(new BoxLayout(ms, BoxLayout.Y_AXIS));
        ms.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        ms.add(UiUtil.left(UiUtil.label("Añadir cuenta de Microsoft", Font.BOLD, 15f, Theme.TEXT)));
        ms.add(UiUtil.vgap(4));
        ms.add(UiUtil.left(UiUtil.muted("<html>Se abre la página oficial de Microsoft: pones tu correo y tu contraseña y listo. Órbita nunca ve tu contraseña.</html>")));
        ms.add(UiUtil.vgap(12));
        OButton msBtn = new OButton("Iniciar sesión con Microsoft", OButton.Kind.SECONDARY, Icons.Type.USER);
        msBtn.addActionListener(e -> MicrosoftWebLogin.start(launcher, frame));
        JPanel msRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        msRow.setOpaque(false);
        msRow.add(msBtn);
        ms.add(UiUtil.left(msRow));
        add.add(ms);
        body.add(add, BorderLayout.CENTER);

        add(UiUtil.scroll(body), BorderLayout.CENTER);
        launcher.onAccountChanged(this::refresh);
        refresh();
    }

    private void refresh() {
        list.removeAll();
        List<Account> all = launcher.accounts();
        int active = launcher.config.activeAccount;
        for (int i = 0; i < all.size(); i++) {
            list.add(UiUtil.left(row(all.get(i), i, i == active, all.size() > 1)));
            list.add(UiUtil.vgap(10));
        }
        list.revalidate();
        list.repaint();
    }

    private JComponent row(Account a, int index, boolean active, boolean removable) {
        Card c = new Card(new BorderLayout(14, 0), 14) {
            @Override
            protected void paintComponent(Graphics g0) {
                super.paintComponent(g0);
                if (active) {
                    Graphics2D g = (Graphics2D) g0.create();
                    Theme.hq(g);
                    g.setColor(Theme.alpha(Theme.ACCENT, 0.55f));
                    g.draw(new java.awt.geom.RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 14, 14));
                    g.dispose();
                }
            }
        };
        c.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, 66));
        c.add(new Avatar(a.name()), BorderLayout.WEST);
        JPanel who = new JPanel();
        who.setOpaque(false);
        who.setLayout(new BoxLayout(who, BoxLayout.Y_AXIS));
        who.add(UiUtil.left(UiUtil.label(a.name(), Font.BOLD, 15f, Theme.TEXT)));
        who.add(UiUtil.vgap(2));
        who.add(UiUtil.left(UiUtil.label(a.isMicrosoft() ? "Cuenta de Microsoft (premium)" : "Cuenta sin premium",
                Font.PLAIN, 12f, a.isMicrosoft() ? Theme.SUCCESS : Theme.MUTED)));
        c.add(who, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        actions.setOpaque(false);
        if (active) {
            actions.add(UiUtil.label("En uso", Font.BOLD, 12f, Theme.ACCENT));
        } else {
            OButton use = new OButton("Usar", OButton.Kind.SECONDARY);
            use.addActionListener(e -> launcher.selectAccount(index));
            actions.add(use);
        }
        if (removable) {
            OButton remove = new OButton("", OButton.Kind.GHOST, Icons.Type.TRASH);
            remove.setToolTipText("Quitar esta cuenta");
            remove.addActionListener(e -> launcher.removeAccount(index));
            actions.add(remove);
        }
        c.add(actions, BorderLayout.EAST);
        return c;
    }

    /** Círculo con la inicial (como en la barra lateral). */
    private static final class Avatar extends JComponent {
        private final String name;

        Avatar(String name) {
            this.name = name == null || name.isBlank() ? "?" : name;
            setPreferredSize(new Dimension(40, 40));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.hq(g);
            g.setPaint(new GradientPaint(0, 0, Theme.ACCENT, 40, 40, Theme.ACCENT_2));
            g.fill(new Ellipse2D.Float(2, 2, 36, 36));
            g.setColor(Color.WHITE);
            g.setFont(Theme.font(Font.BOLD, 16f));
            String s = name.substring(0, 1).toUpperCase();
            FontMetrics fm = g.getFontMetrics();
            g.drawString(s, 20 - fm.stringWidth(s) / 2, 20 - fm.getHeight() / 2 + fm.getAscent());
            g.dispose();
        }
    }
}
