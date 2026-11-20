package dev.orbita.launcher.ui;

import dev.orbita.launcher.Launcher;
import dev.orbita.launcher.LauncherInfo;
import dev.orbita.launcher.core.auth.Account;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/** Barra lateral: logo, navegación y tarjeta de la cuenta. */
public final class Sidebar extends JPanel {
    private static final int SIDEBAR_WIDTH = 236;
    private final List<NavItem> items = new ArrayList<>();
    private final JComponent logo;

    public Sidebar(Launcher launcher, IntConsumer onSelect) {
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(18, 14, 18, 14));
        setPreferredSize(new Dimension(SIDEBAR_WIDTH, 10));

        logo = new Logo();
        add(logo);
        add(Box.createRigidArea(new Dimension(0, 26)));

        String[] labels = {"Jugar", "Cuentas", "Mods", "Rendimiento", "Ajustes"};
        Icons.Type[] icons = {Icons.Type.PLAY, Icons.Type.USER, Icons.Type.MODS, Icons.Type.BOLT, Icons.Type.SETTINGS};
        for (int i = 0; i < labels.length; i++) {
            int index = i;
            NavItem item = new NavItem(labels[i], icons[i], () -> {
                select(index);
                onSelect.accept(index);
            });
            items.add(item);
            add(item);
            add(Box.createRigidArea(new Dimension(0, 6)));
        }
        select(0);

        add(Box.createVerticalGlue());
        AccountBadge badge = new AccountBadge(launcher);
        add(badge);
        launcher.onAccountChanged(badge::repaint);
    }

    public JComponent dragHandle() {
        return logo;
    }

    public void select(int index) {
        for (int i = 0; i < items.size(); i++) {
            items.get(i).setSelected(i == index);
        }
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setPaint(new GradientPaint(0, 0, new Color(10, 11, 17, 215), 0, getHeight(), new Color(14, 15, 24, 200)));
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(Theme.BORDER);
        g.fillRect(getWidth() - 1, 0, 1, getHeight());
        g.dispose();
    }

    private static final class Logo extends JComponent {
        Logo() {
            setAlignmentX(LEFT_ALIGNMENT);
            setPreferredSize(new Dimension(SIDEBAR_WIDTH - 28, 54));
            setMaximumSize(new Dimension(SIDEBAR_WIDTH - 28, 54));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.hq(g);
            Icons.planet(g, 2, 3, 46);
            g.setFont(Theme.tracked(Font.BOLD, 21f, 0.2f));
            g.setColor(Theme.TEXT);
            g.drawString("ÓRBITA", 58, 26);
            g.setFont(Theme.font(Font.PLAIN, 12f));
            g.setColor(Theme.MUTED);
            g.drawString("Minecraft " + LauncherInfo.MINECRAFT + " · Fabric", 59, 44);
            g.dispose();
        }
    }

    /** Elemento de navegación con indicador animado. */
    private static final class NavItem extends JComponent {
        private final String label;
        private final Icons.Type icon;
        private final Animator hover = new Animator(0f, 14f, this::repaint);
        private final Animator active = new Animator(0f, 12f, this::repaint);

        NavItem(String label, Icons.Type icon, Runnable onClick) {
            this.label = label;
            this.icon = icon;
            setAlignmentX(LEFT_ALIGNMENT);
            setPreferredSize(new Dimension(SIDEBAR_WIDTH - 28, 44));
            setMaximumSize(new Dimension(SIDEBAR_WIDTH - 28, 44));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover.setTarget(1f);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover.setTarget(0f);
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    onClick.run();
                }
            });
        }

        void setSelected(boolean selected) {
            active.setTarget(selected ? 1f : 0f);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.hq(g);
            float h = hover.get();
            float a = active.get();
            int w = getWidth();
            int ht = getHeight();
            if (a > 0.01f || h > 0.01f) {
                g.setPaint(new GradientPaint(0, 0, Theme.alpha(Theme.ACCENT, 0.22f * a + 0.06f * h),
                        w, 0, new Color(255, 255, 255, (int) (10 * h))));
                g.fill(new RoundRectangle2D.Float(0, 0, w, ht, 12, 12));
            }
            float barH = (ht - 18) * a;
            if (barH > 0.5f) {
                g.setColor(Theme.ACCENT);
                g.fill(new RoundRectangle2D.Float(0, (ht - barH) / 2f, 3, barH, 3, 3));
            }
            Color c = Theme.mix(Theme.MUTED, Theme.TEXT, Math.max(a, h));
            Icons.paint(g, icon, 16, (ht - 18) / 2f, 18, a > 0.5f ? Theme.mix(c, Theme.ACCENT, 0.5f) : c);
            g.setFont(Theme.font(a > 0.5f ? Font.BOLD : Font.PLAIN, 14f));
            g.setColor(c);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(label, 46, (ht - fm.getHeight()) / 2 + fm.getAscent());
            g.dispose();
        }
    }

    /** Tarjeta con el nombre y el tipo de cuenta. */
    private static final class AccountBadge extends JComponent {
        private final Launcher launcher;

        private boolean hover;

        AccountBadge(Launcher launcher) {
            this.launcher = launcher;
            setAlignmentX(LEFT_ALIGNMENT);
            setPreferredSize(new Dimension(SIDEBAR_WIDTH - 28, 62));
            setMaximumSize(new Dimension(SIDEBAR_WIDTH - 28, 62));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("Cambiar de cuenta o añadir una (premium y sin premium)");
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
                public void mousePressed(MouseEvent e) {
                    java.awt.Window w = javax.swing.SwingUtilities.getWindowAncestor(AccountBadge.this);
                    if (w instanceof javax.swing.JFrame f) {
                        AccountSwitcher.show(launcher, f, AccountBadge.this, false);
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.hq(g);
            int w = getWidth();
            int h = getHeight();
            g.setColor(hover ? Theme.SURFACE_HOVER : Theme.SURFACE);
            g.fill(new RoundRectangle2D.Float(0, 0, w, h, 14, 14));
            g.setColor(hover ? Theme.alpha(Theme.ACCENT, 0.5f) : Theme.BORDER);
            g.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 14, 14));
            // flechita: se puede pulsar para cambiar de cuenta
            g.setColor(hover ? Theme.ACCENT : Theme.MUTED);
            g.setStroke(new java.awt.BasicStroke(1.6f));
            int ax = w - 20, ay = h / 2;
            g.drawLine(ax - 4, ay + 2, ax, ay - 2);
            g.drawLine(ax, ay - 2, ax + 4, ay + 2);

            Account acc = launcher.account();
            String name = acc.name() == null || acc.name().isBlank() ? "Jugador" : acc.name();
            g.setPaint(new GradientPaint(12, 13, Theme.ACCENT, 48, 49, Theme.ACCENT_2));
            g.fill(new Ellipse2D.Float(12, 13, 36, 36));
            g.setColor(Color.WHITE);
            g.setFont(Theme.font(Font.BOLD, 16f));
            String initial = name.substring(0, 1).toUpperCase();
            FontMetrics fm = g.getFontMetrics();
            g.drawString(initial, 30 - fm.stringWidth(initial) / 2, 31 - fm.getHeight() / 2 + fm.getAscent());

            g.setFont(Theme.font(Font.BOLD, 14f));
            g.setColor(Theme.TEXT);
            g.drawString(name, 60, 28);
            g.setFont(Theme.font(Font.PLAIN, 12f));
            g.setColor(acc.isMicrosoft() ? Theme.SUCCESS : Theme.MUTED);
            g.drawString(acc.isMicrosoft() ? "Premium (Microsoft)" : "Sin premium", 60, 45);
            g.dispose();
        }
    }
}
