package dev.orbita.launcher.ui.components;

import dev.orbita.launcher.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.JTextField;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.RoundRectangle2D;

/** Campo de texto oscuro con borde que se ilumina al enfocarlo y texto de ayuda. */
public class OTextField extends JTextField {
    private final String placeholder;

    public OTextField(String text, String placeholder) {
        super(text);
        this.placeholder = placeholder;
        setOpaque(false);
        setForeground(Theme.TEXT);
        setCaretColor(Theme.ACCENT);
        setSelectionColor(Theme.alpha(Theme.ACCENT, 0.45f));
        setSelectedTextColor(Theme.TEXT);
        setFont(Theme.font(Font.PLAIN, 14f));
        setBorder(BorderFactory.createEmptyBorder(9, 14, 9, 14));
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                repaint();
            }
        });
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(Math.max(d.width, 200), 40);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, 40);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.hq(g);
        RoundRectangle2D r = new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 12, 12);
        g.setColor(Theme.alpha(Theme.SURFACE_SOLID, 0.9f));
        g.fill(r);
        g.setColor(hasFocus() ? Theme.alpha(Theme.ACCENT, 0.9f) : Theme.BORDER);
        g.draw(r);
        if (getText().isEmpty() && !hasFocus() && placeholder != null) {
            g.setColor(Theme.DISABLED);
            g.setFont(getFont());
            g.drawString(placeholder, getInsets().left, (getHeight() - g.getFontMetrics().getHeight()) / 2 + g.getFontMetrics().getAscent());
        }
        g.dispose();
        super.paintComponent(g0);
    }
}
