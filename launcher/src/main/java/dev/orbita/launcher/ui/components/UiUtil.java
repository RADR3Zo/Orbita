package dev.orbita.launcher.ui.components;

import dev.orbita.launcher.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.RoundRectangle2D;

public final class UiUtil {
    private UiUtil() {
    }

    public static JLabel label(String text, int style, float size, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.font(style, size));
        l.setForeground(color);
        return l;
    }

    /** Etiqueta que siempre usa el color de acento actual del tema (cambia en vivo). */
    public static final class AccentLabel extends JLabel {
        public AccentLabel(String text) {
            super(text);
        }

        @Override
        public Color getForeground() {
            return Theme.ACCENT;
        }
    }

    public static JLabel title(String text) {
        return label(text, Font.BOLD, 26f, Theme.TEXT);
    }

    public static JLabel muted(String text) {
        return label(text, Font.PLAIN, 13f, Theme.MUTED);
    }

    public static <T extends JComponent> T left(T c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    public static Component vgap(int h) {
        return Box.createRigidArea(new Dimension(0, h));
    }

    public static Component hgap(int w) {
        return Box.createRigidArea(new Dimension(w, 0));
    }

    /** ScrollPane transparente con barra fina; el contenido se ajusta al ancho visible. */
    public static JScrollPane scroll(JComponent view) {
        JScrollPane sp = new JScrollPane(view instanceof javax.swing.Scrollable ? view : new WidthTracking(view));
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setViewportBorder(null);
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sp.getVerticalScrollBar().setUnitIncrement(18);
        sp.getVerticalScrollBar().setOpaque(false);
        sp.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        sp.getVerticalScrollBar().setUI(new ThinScrollBarUI());
        return sp;
    }

    public static void ui(Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            SwingUtilities.invokeLater(r);
        }
    }

    /** Contenedor que ocupa siempre el ancho del viewport (sin scroll horizontal). */
    private static final class WidthTracking extends javax.swing.JPanel implements javax.swing.Scrollable {
        WidthTracking(JComponent view) {
            super(new java.awt.BorderLayout());
            setOpaque(false);
            add(view, java.awt.BorderLayout.CENTER);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
            return 18;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
            return Math.max(18, visible.height - 40);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    private static final class ThinScrollBarUI extends BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            thumbColor = new Color(255, 255, 255, 40);
            trackColor = new Color(0, 0, 0, 0);
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return zero();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return zero();
        }

        private static JButton zero() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            return b;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
        }

        @Override
        protected void paintThumb(Graphics g0, JComponent c, Rectangle r) {
            if (r.isEmpty()) {
                return;
            }
            Graphics2D g = (Graphics2D) g0.create();
            Theme.hq(g);
            g.setColor(isThumbRollover() ? new Color(255, 255, 255, 80) : thumbColor);
            g.fill(new RoundRectangle2D.Float(r.x + 1, r.y, r.width - 2, r.height, 6, 6));
            g.dispose();
        }

        @Override
        protected void installDefaults() {
            super.installDefaults();
            scrollbar.setBorder(null);
        }

        @SuppressWarnings("unused")
        JScrollBar bar() {
            return scrollbar;
        }
    }
}
