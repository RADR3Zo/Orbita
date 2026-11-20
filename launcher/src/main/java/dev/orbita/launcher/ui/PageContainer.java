package dev.orbita.launcher.ui;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.AlphaComposite;
import java.awt.CardLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;

/** CardLayout con transición de desvanecimiento y deslizamiento al cambiar de página. */
public final class PageContainer extends JPanel {
    private final CardLayout cards = new CardLayout();
    private final Animator fade = new Animator(1f, 9f, this::repaint);
    private String current;

    public PageContainer() {
        setLayout(cards);
        setOpaque(false);
    }

    public void addPage(String name, JComponent page) {
        page.setOpaque(false);
        add(page, name);
        if (current == null) {
            current = name;
        }
    }

    public void show(String name) {
        if (name.equals(current)) {
            return;
        }
        current = name;
        cards.show(this, name);
        fade.snap(0f);
        fade.setTarget(1f);
    }

    @Override
    public void paint(Graphics g0) {
        float a = fade.get();
        if (a >= 0.999f) {
            super.paint(g0);
            return;
        }
        Graphics2D g = (Graphics2D) g0.create();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, a)));
        g.translate(0, (1f - a) * 14f);
        super.paint(g);
        g.dispose();
    }
}
