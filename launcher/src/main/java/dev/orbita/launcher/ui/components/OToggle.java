package dev.orbita.launcher.ui.components;

import dev.orbita.launcher.ui.Animator;
import dev.orbita.launcher.ui.Theme;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.function.Consumer;

/** Interruptor on/off animado. */
public class OToggle extends JComponent {
    private boolean selected;
    private final Animator knob;
    private final Animator hover = new Animator(0f, 14f, this::repaint);
    private Consumer<Boolean> listener = v -> { };

    public OToggle(boolean selected) {
        this.selected = selected;
        this.knob = new Animator(selected ? 1f : 0f, 16f, this::repaint);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (isEnabled()) {
                    setSelected(!OToggle.this.selected);
                    listener.accept(OToggle.this.selected);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                hover.setTarget(1f);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover.setTarget(0f);
            }
        });
    }

    public void onChange(Consumer<Boolean> listener) {
        this.listener = listener;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        knob.setTarget(selected ? 1f : 0f);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(44, 24);
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.hq(g);
        float t = knob.get();
        float h = hover.get();
        int w = 44;
        int ht = 24;
        int y = (getHeight() - ht) / 2;
        RoundRectangle2D track = new RoundRectangle2D.Float(0, y, w, ht, ht, ht);
        Color off = Theme.mix(new Color(0x2A2D3A), new Color(0x353948), h);
        if (t > 0f) {
            g.setColor(off);
            g.fill(track);
            g.setPaint(new GradientPaint(0, 0, Theme.alpha(Theme.ACCENT, t), w, 0, Theme.alpha(Theme.ACCENT_2, t)));
            g.fill(track);
        } else {
            g.setColor(off);
            g.fill(track);
        }
        if (!isEnabled()) {
            g.setColor(new Color(0, 0, 0, 120));
            g.fill(track);
        }
        float d = ht - 6;
        float kx = 3 + (w - ht) * t;
        g.setColor(new Color(0, 0, 0, 60));
        g.fill(new Ellipse2D.Float(kx, y + 4, d, d));
        g.setColor(Color.WHITE);
        g.fill(new Ellipse2D.Float(kx, y + 3, d, d));
        g.dispose();
    }
}
