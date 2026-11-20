package dev.orbita.launcher.ui.components;

import dev.orbita.launcher.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.geom.RoundRectangle2D;

/** Diálogos oscuros sin bordes del sistema. */
public final class Dialogs {
    private Dialogs() {
    }

    public static boolean confirm(Window owner, String title, String message, String yes, String no, boolean danger) {
        boolean[] result = {false};
        JDialog d = base(owner);
        JPanel content = content();
        content.add(UiUtil.left(UiUtil.label(title, Font.BOLD, 18f, Theme.TEXT)));
        content.add(UiUtil.vgap(10));
        content.add(UiUtil.left(text(message)));
        content.add(UiUtil.vgap(18));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        OButton cancel = new OButton(no, OButton.Kind.GHOST);
        cancel.addActionListener(e -> d.dispose());
        OButton ok = new OButton(yes, danger ? OButton.Kind.DANGER : OButton.Kind.SECONDARY);
        ok.addActionListener(e -> {
            result[0] = true;
            d.dispose();
        });
        buttons.add(cancel);
        buttons.add(ok);
        content.add(UiUtil.left(buttons));
        show(d, content, owner);
        return result[0];
    }

    public static void info(Window owner, String title, String message) {
        JDialog d = base(owner);
        JPanel content = content();
        content.add(UiUtil.left(UiUtil.label(title, Font.BOLD, 18f, Theme.TEXT)));
        content.add(UiUtil.vgap(10));
        content.add(UiUtil.left(text(message)));
        content.add(UiUtil.vgap(18));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttons.setOpaque(false);
        OButton ok = new OButton("Entendido", OButton.Kind.SECONDARY);
        ok.addActionListener(e -> d.dispose());
        buttons.add(ok);
        content.add(UiUtil.left(buttons));
        show(d, content, owner);
    }

    public static JDialog base(Window owner) {
        JDialog d = new JDialog(owner, JDialog.ModalityType.APPLICATION_MODAL);
        d.setUndecorated(true);
        d.setBackground(new Color(0, 0, 0, 0));
        d.getRootPane().registerKeyboardAction(e -> d.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        return d;
    }

    public static JPanel content() {
        JPanel content = new JPanel() {
            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                Theme.hq(g);
                RoundRectangle2D r = new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 18, 18);
                g.setColor(Theme.SURFACE_SOLID);
                g.fill(r);
                g.setColor(Theme.alpha(Theme.ACCENT, 0.5f));
                g.draw(r);
                g.dispose();
            }
        };
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(22, 24, 20, 24));
        return content;
    }

    public static void show(JDialog d, JPanel content, Window owner) {
        d.getContentPane().setLayout(new BorderLayout());
        d.getContentPane().add(content);
        d.pack();
        d.setSize(Math.max(420, d.getWidth()), d.getHeight());
        d.setLocationRelativeTo(owner);
        d.setVisible(true);
    }

    public static JTextArea text(String message) {
        JTextArea t = new JTextArea(message);
        t.setLineWrap(true);
        t.setWrapStyleWord(true);
        t.setEditable(false);
        t.setFocusable(false);
        t.setOpaque(false);
        t.setForeground(Theme.MUTED);
        t.setFont(Theme.font(Font.PLAIN, 14f));
        t.setColumns(34);
        t.setBorder(null);
        return t;
    }
}
