package dev.orbita.launcher.ui.components;

import dev.orbita.launcher.ui.Animator;
import dev.orbita.launcher.ui.Icons;
import dev.orbita.launcher.ui.Theme;

import javax.swing.JButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;

/** Botón redondeado con brillo animado al pasar el ratón. */
public class OButton extends JButton {
    public enum Kind { PRIMARY, SECONDARY, GHOST, DANGER }

    private final Kind kind;
    private final Animator hover = new Animator(0f, 14f, this::repaint);
    private final Animator press = new Animator(0f, 24f, this::repaint);
    private Icons.Type icon;
    private int arc = 12;

    public OButton(String text, Kind kind) {
        super(text);
        this.kind = kind;
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setForeground(Theme.TEXT);
        setFont(Theme.font(Font.BOLD, 13f));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        getModel().addChangeListener(e -> {
            hover.setTarget(getModel().isRollover() && isEnabled() ? 1f : 0f);
            press.setTarget(getModel().isPressed() ? 1f : 0f);
        });
    }

    public OButton(String text, Kind kind, Icons.Type icon) {
        this(text, kind);
        this.icon = icon;
    }

    public OButton icon(Icons.Type icon) {
        this.icon = icon;
        repaint();
        return this;
    }

    public OButton arc(int arc) {
        this.arc = arc;
        return this;
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        String text = getText();
        int textW = text == null || text.isEmpty() ? 0 : fm.stringWidth(text);
        int iconW = icon == null ? 0 : 16 + (textW > 0 ? 8 : 0);
        int w = textW + iconW + (textW > 0 ? 36 : 20);
        return new Dimension(Math.max(w, 38), 38);
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.hq(g);
        float h = hover.get();
        float p = press.get();
        boolean en = isEnabled();
        int w = getWidth();
        int ht = getHeight();
        float inset = p * 1.2f;
        float pad = kind == Kind.PRIMARY ? 6f : 1f;
        RoundRectangle2D body = new RoundRectangle2D.Float(pad + inset, pad + inset, w - 2 * (pad + inset), ht - 2 * (pad + inset), arc, arc);

        Color textColor = en ? Theme.TEXT : Theme.DISABLED;
        switch (kind) {
            case PRIMARY -> {
                if (en) {
                    for (int i = 3; i >= 1; i--) {
                        g.setColor(Theme.alpha(Theme.ACCENT, (0.05f + 0.07f * h) * (4 - i) / 3f));
                        float e = i * 2f;
                        g.fill(new RoundRectangle2D.Float(pad - e, pad - e, w - 2 * pad + 2 * e, ht - 2 * pad + 2 * e, arc + e, arc + e));
                    }
                }
                Color a = en ? Theme.mix(Theme.ACCENT, Color.WHITE, h * 0.12f) : new Color(0x2A2D3A);
                Color b = en ? Theme.mix(Theme.ACCENT_2, Color.WHITE, h * 0.12f) : new Color(0x2A2D3A);
                g.setPaint(new GradientPaint(0, 0, a, w, ht, b));
                g.fill(body);
                textColor = en ? Color.WHITE : Theme.DISABLED;
            }
            case SECONDARY -> {
                g.setColor(Theme.mix(Theme.SURFACE, Theme.SURFACE_HOVER, h));
                g.fill(body);
                g.setColor(Theme.mix(Theme.BORDER, Theme.alpha(Theme.ACCENT, 0.8f), h));
                g.draw(body);
            }
            case GHOST -> {
                if (h > 0.01f) {
                    g.setColor(new Color(1f, 1f, 1f, 0.07f * h));
                    g.fill(body);
                }
                textColor = en ? Theme.mix(Theme.MUTED, Theme.TEXT, h) : Theme.DISABLED;
            }
            case DANGER -> {
                g.setColor(Theme.alpha(Theme.DANGER, 0.12f + 0.18f * h));
                g.fill(body);
                textColor = en ? Theme.mix(Theme.DANGER, Color.WHITE, h * 0.3f) : Theme.DISABLED;
            }
        }

        String text = getText();
        g.setFont(getFont());
        FontMetrics fm = g.getFontMetrics();
        int textW = text == null || text.isEmpty() ? 0 : fm.stringWidth(text);
        int iconSize = 16;
        int gap = textW > 0 && icon != null ? 8 : 0;
        int total = textW + (icon != null ? iconSize + gap : 0);
        int x = (w - total) / 2;
        if (icon != null) {
            Icons.paint(g, icon, x, (ht - iconSize) / 2f, iconSize, textColor);
            x += iconSize + gap;
        }
        if (textW > 0) {
            g.setColor(textColor);
            g.drawString(text, x, (ht - fm.getHeight()) / 2 + fm.getAscent());
        }
        g.dispose();
    }
}
