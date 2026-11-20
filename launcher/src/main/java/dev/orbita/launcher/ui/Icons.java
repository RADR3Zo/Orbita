package dev.orbita.launcher.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.RadialGradientPaint;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/** Iconos vectoriales dibujados en código (sin archivos de imagen). */
public final class Icons {
    public enum Type { PLAY, MODS, SETTINGS, CLOSE, MINUS, REFRESH, TRASH, DOWNLOAD, SEARCH, FOLDER, BOLT, USER, CHECK }

    private Icons() {
    }

    public static void paint(Graphics2D g0, Type type, float x, float y, float s, Color color) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.hq(g);
        g.setColor(color);
        g.translate(x, y);
        g.scale(s / 24f, s / 24f);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (type) {
            case PLAY -> {
                Path2D p = new Path2D.Float();
                p.moveTo(7, 4.5);
                p.lineTo(19.5, 12);
                p.lineTo(7, 19.5);
                p.closePath();
                g.fill(p);
            }
            case MODS -> {
                g.draw(new RoundRectangle2D.Float(3, 3, 8, 8, 3, 3));
                g.draw(new RoundRectangle2D.Float(13, 3, 8, 8, 3, 3));
                g.draw(new RoundRectangle2D.Float(3, 13, 8, 8, 3, 3));
                g.fill(new RoundRectangle2D.Float(13, 13, 8, 8, 3, 3));
            }
            case SETTINGS -> {
                for (int i = 0; i < 8; i++) {
                    Graphics2D t = (Graphics2D) g.create();
                    t.rotate(Math.PI / 4 * i, 12, 12);
                    t.fill(new RoundRectangle2D.Float(10.2f, 1.5f, 3.6f, 5f, 1.5f, 1.5f));
                    t.dispose();
                }
                g.draw(new Ellipse2D.Float(5, 5, 14, 14));
                g.draw(new Ellipse2D.Float(9, 9, 6, 6));
            }
            case CLOSE -> {
                g.draw(new Line2D.Float(6, 6, 18, 18));
                g.draw(new Line2D.Float(18, 6, 6, 18));
            }
            case MINUS -> g.draw(new Line2D.Float(6, 12, 18, 12));
            case REFRESH -> {
                g.draw(new java.awt.geom.Arc2D.Float(4, 4, 16, 16, 40, 290, java.awt.geom.Arc2D.OPEN));
                Path2D p = new Path2D.Float();
                p.moveTo(20, 3.5);
                p.lineTo(20, 9);
                p.lineTo(14.5, 9);
                g.draw(p);
            }
            case TRASH -> {
                g.draw(new Line2D.Float(4, 6.5f, 20, 6.5f));
                g.draw(new Line2D.Float(9.5f, 3.5f, 15f, 3.5f));
                Path2D p = new Path2D.Float();
                p.moveTo(6, 6.5);
                p.lineTo(7, 20.5);
                p.lineTo(17, 20.5);
                p.lineTo(18, 6.5);
                g.draw(p);
            }
            case DOWNLOAD -> {
                g.draw(new Line2D.Float(12, 3.5f, 12, 15));
                Path2D p = new Path2D.Float();
                p.moveTo(7, 10.5);
                p.lineTo(12, 15.5);
                p.lineTo(17, 10.5);
                g.draw(p);
                g.draw(new Line2D.Float(4.5f, 20, 19.5f, 20));
            }
            case SEARCH -> {
                g.draw(new Ellipse2D.Float(4, 4, 12, 12));
                g.draw(new Line2D.Float(15f, 15f, 20, 20));
            }
            case FOLDER -> {
                Path2D p = new Path2D.Float();
                p.moveTo(3, 7);
                p.lineTo(3, 19);
                p.lineTo(21, 19);
                p.lineTo(21, 8.5);
                p.lineTo(12, 8.5);
                p.lineTo(10, 5.5);
                p.lineTo(3, 5.5);
                p.closePath();
                g.draw(p);
            }
            case BOLT -> {
                Path2D p = new Path2D.Float();
                p.moveTo(13.5, 2.5);
                p.lineTo(5, 13.5);
                p.lineTo(11.5, 13.5);
                p.lineTo(10.5, 21.5);
                p.lineTo(19, 10.5);
                p.lineTo(12.5, 10.5);
                p.closePath();
                g.fill(p);
            }
            case USER -> {
                g.draw(new Ellipse2D.Float(8, 3.5f, 8, 8));
                g.draw(new java.awt.geom.Arc2D.Float(4, 13, 16, 16, 0, 180, java.awt.geom.Arc2D.OPEN));
            }
            case CHECK -> {
                Path2D p = new Path2D.Float();
                p.moveTo(5, 12.5);
                p.lineTo(10, 17.5);
                p.lineTo(19.5, 6.5);
                g.draw(p);
            }
        }
        g.dispose();
    }

    /** Planeta con anillo: logo de Órbita. */
    public static void planet(Graphics2D g0, float x, float y, float size) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.hq(g);
        float r = size * 0.32f;
        float cx = x + size / 2f;
        float cy = y + size / 2f;
        g.setStroke(new BasicStroke(size * 0.05f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Graphics2D ring = (Graphics2D) g.create();
        ring.rotate(Math.toRadians(-18), cx, cy);
        ring.setColor(Theme.alpha(Theme.ACCENT, 0.55f));
        ring.draw(new java.awt.geom.Arc2D.Float(cx - size * 0.48f, cy - size * 0.15f, size * 0.96f, size * 0.3f, 0, 180, java.awt.geom.Arc2D.OPEN));
        ring.dispose();

        g.setPaint(new RadialGradientPaint(new Point2D.Float(cx - r * 0.4f, cy - r * 0.4f), r * 1.6f,
                new float[]{0f, 0.55f, 1f},
                new Color[]{new Color(0xB3A8FF), new Color(0x5B4BD6), new Color(0x1B1636)}));
        g.fill(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));

        ring = (Graphics2D) g.create();
        ring.rotate(Math.toRadians(-18), cx, cy);
        ring.setColor(Theme.ACCENT);
        ring.draw(new java.awt.geom.Arc2D.Float(cx - size * 0.48f, cy - size * 0.15f, size * 0.96f, size * 0.3f, 180, 180, java.awt.geom.Arc2D.OPEN));
        ring.dispose();
        g.dispose();
    }

    public static List<BufferedImage> appIcons() {
        List<BufferedImage> list = new ArrayList<>();
        for (int s : new int[]{16, 24, 32, 48, 64, 128, 256}) {
            BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            planet(g, 0, 0, s);
            g.dispose();
            list.add(img);
        }
        return list;
    }
}
