package dev.orbita.launcher.ui.components;

import dev.orbita.launcher.ui.Animator;
import dev.orbita.launcher.ui.Theme;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;

/** Barra de progreso con degradado; con valor negativo muestra una animación indeterminada. */
public class OProgressBar extends JComponent {
    private final Animator value = new Animator(0f, 8f, this::repaint);
    private boolean indeterminate;

    public OProgressBar() {
        setOpaque(false);
    }

    public void setValue(double v) {
        if (v < 0) {
            indeterminate = true;
            repaint();
            return;
        }
        indeterminate = false;
        value.setTarget((float) Math.min(1, v));
    }

    public void reset() {
        indeterminate = false;
        value.snap(0f);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(200, 6);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, 6);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.hq(g);
        int w = getWidth();
        int h = getHeight();
        g.setColor(new Color(255, 255, 255, 18));
        g.fill(new RoundRectangle2D.Float(0, 0, w, h, h, h));
        float x0;
        float fw;
        if (indeterminate) {
            float t = (System.nanoTime() / 1_000_000_000f * 0.7f) % 1f;
            fw = w * 0.3f;
            x0 = -fw + (w + fw) * t;
            repaint();
        } else {
            x0 = 0;
            fw = w * value.get();
        }
        if (fw > 0.5f) {
            g.clip(new RoundRectangle2D.Float(0, 0, w, h, h, h));
            g.setPaint(new GradientPaint(x0, 0, Theme.ACCENT, x0 + Math.max(fw, 1), 0, Theme.ACCENT_2));
            g.fill(new RoundRectangle2D.Float(x0, 0, fw, h, h, h));
        }
        g.dispose();
    }
}
