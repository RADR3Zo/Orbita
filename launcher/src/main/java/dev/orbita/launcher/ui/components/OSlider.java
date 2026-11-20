package dev.orbita.launcher.ui.components;

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
import java.util.function.IntConsumer;

/** Deslizador con pasos discretos (usado para la RAM). */
public class OSlider extends JComponent {
    private final int min;
    private final int max;
    private final int step;
    private int value;
    private IntConsumer listener = v -> { };
    private boolean dragging;

    public OSlider(int min, int max, int step, int value) {
        this.min = min;
        this.max = Math.max(min + step, max);
        this.step = step;
        this.value = clamp(value);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragging = true;
                update(e.getX());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                update(e.getX());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                dragging = false;
                repaint();
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
    }

    public void onChange(IntConsumer listener) {
        this.listener = listener;
    }

    public int getValue() {
        return value;
    }

    private void update(int mouseX) {
        int pad = 10;
        float t = (mouseX - pad) / (float) Math.max(1, getWidth() - 2 * pad);
        int v = clamp(Math.round((min + t * (max - min)) / step) * step);
        if (v != value) {
            value = v;
            listener.accept(v);
        }
        repaint();
    }

    private int clamp(int v) {
        return Math.max(min, Math.min(max, v));
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(260, 26);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, 26);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.hq(g);
        int pad = 10;
        int w = getWidth() - 2 * pad;
        int cy = getHeight() / 2;
        float t = (value - min) / (float) (max - min);
        g.setColor(new Color(255, 255, 255, 22));
        g.fill(new RoundRectangle2D.Float(pad, cy - 3, w, 6, 6, 6));
        g.setPaint(new GradientPaint(pad, 0, Theme.ACCENT, pad + w, 0, Theme.ACCENT_2));
        g.fill(new RoundRectangle2D.Float(pad, cy - 3, w * t, 6, 6, 6));
        float kx = pad + w * t;
        float r = dragging ? 9 : 8;
        g.setColor(Theme.alpha(Theme.ACCENT, 0.3f));
        g.fill(new Ellipse2D.Float(kx - r - 3, cy - r - 3, (r + 3) * 2, (r + 3) * 2));
        g.setColor(Color.WHITE);
        g.fill(new Ellipse2D.Float(kx - r, cy - r, r * 2, r * 2));
        g.dispose();
    }
}
