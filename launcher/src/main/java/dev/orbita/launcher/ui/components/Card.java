package dev.orbita.launcher.ui.components;

import dev.orbita.launcher.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.geom.RoundRectangle2D;

/** Panel translúcido con esquinas redondeadas. */
public class Card extends JPanel {
    private final int arc;

    public Card(LayoutManager layout) {
        this(layout, 16);
    }

    public Card(LayoutManager layout, int arc) {
        super(layout);
        this.arc = arc;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.hq(g);
        RoundRectangle2D r = new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, arc, arc);
        g.setColor(Theme.SURFACE);
        g.fill(r);
        g.setColor(Theme.BORDER);
        g.draw(r);
        g.dispose();
    }
}
