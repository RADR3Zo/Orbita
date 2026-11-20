package dev.orbita.launcher.ui;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.RadialGradientPaint;
import java.awt.Toolkit;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Locale;
import java.util.Random;

/**
 * Fondo del launcher. Modos:
 * <ul>
 *   <li>UNIVERSE: nebulosas (con el color del tema) + estrellas con paralaje y estrellas fugaces.</li>
 *   <li>COLOR: degradado suave con el color del tema.</li>
 *   <li>IMAGE: imagen del usuario (los GIF se animan), recortada para cubrir y oscurecida.</li>
 *   <li>VIDEO: el panel es transparente y solo oscurece el video que hay detrás (ver MediaLayer).</li>
 * </ul>
 * La animación solo corre mientras la ventana se ve.
 */
public final class StarfieldPanel extends JPanel {
    public enum Mode { UNIVERSE, COLOR, IMAGE, VIDEO }

    private static final int STARS = 260;
    private final float[] sx = new float[STARS];
    private final float[] sy = new float[STARS];
    private final float[] depth = new float[STARS];
    private final float[] phase = new float[STARS];
    private final Random random = new Random(0x0B17A);
    private final Timer timer;
    private BufferedImage nebula;
    private Color nebulaAccent;
    private float shootX, shootY, shootStart = -100f;
    private float nextShoot = 3f;

    private Mode mode = Mode.UNIVERSE;
    private Image image;
    private int dim = 45;

    public StarfieldPanel() {
        setOpaque(true);
        setBackground(Theme.BG);
        for (int i = 0; i < STARS; i++) {
            sx[i] = random.nextFloat();
            sy[i] = random.nextFloat();
            depth[i] = 0.2f + random.nextFloat() * 0.8f;
            phase[i] = random.nextFloat() * 6.283f;
        }
        timer = new Timer(33, e -> repaint());
        timer.setCoalesce(true);
    }

    /** Cambia el modo de fondo. file solo se usa en IMAGE. dim: 0..80 (% de oscurecimiento). */
    public void setMode(Mode mode, File file, int dim) {
        this.mode = mode;
        this.dim = Math.max(0, Math.min(80, dim));
        if (image != null) {
            image.flush();
        }
        image = null;
        if (mode == Mode.IMAGE && file != null && file.isFile()) {
            String name = file.getName().toLowerCase(Locale.ROOT);
            try {
                // Los GIF se cargan con Toolkit para conservar la animación; el resto con ImageIO.
                image = name.endsWith(".gif") ? Toolkit.getDefaultToolkit().createImage(file.getAbsolutePath())
                        : ImageIO.read(file);
            } catch (Exception e) {
                image = null;
            }
        }
        setOpaque(mode != Mode.VIDEO);
        updateTimer();
        repaint();
    }

    public Mode mode() {
        return mode;
    }

    /** La animación solo corre mientras la ventana se ve (oculta o minimizada no gasta CPU). */
    @Override
    public void addNotify() {
        super.addNotify();
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0) {
                updateTimer();
            }
        });
        java.awt.Window w = SwingUtilities.getWindowAncestor(this);
        if (w instanceof java.awt.Frame f) {
            f.addWindowStateListener(e -> updateTimer());
        }
        updateTimer();
    }

    private void updateTimer() {
        java.awt.Window w = SwingUtilities.getWindowAncestor(this);
        boolean minimized = w instanceof java.awt.Frame f && (f.getExtendedState() & java.awt.Frame.ICONIFIED) != 0;
        // Color e imagen estática no necesitan animación; los GIF se repintan solos al avanzar de fotograma.
        boolean animated = mode == Mode.UNIVERSE;
        if (animated && isShowing() && !minimized) {
            if (!timer.isRunning()) {
                timer.start();
            }
        } else {
            timer.stop();
        }
    }

    @Override
    public void removeNotify() {
        timer.stop();
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        int w = getWidth();
        int h = getHeight();
        switch (mode) {
            case UNIVERSE -> paintUniverse(g, w, h);
            case COLOR -> paintColor(g, w, h);
            case IMAGE -> paintImage(g, w, h);
            case VIDEO -> paintDim(g, w, h);
        }
        g.dispose();
    }

    private void paintColor(Graphics2D g, int w, int h) {
        Theme.hq(g);
        g.setPaint(new GradientPaint(0, 0, Theme.mix(Theme.BG, Theme.ACCENT, 0.18f), w, h, Theme.mix(Theme.BG, Theme.ACCENT_2, 0.10f)));
        g.fillRect(0, 0, w, h);
        blob(g, w * 0.8f, h * 0.15f, Math.max(w, h) * 0.5f, Theme.alpha(Theme.ACCENT, 0.22f));
        blob(g, w * 0.1f, h * 0.95f, Math.max(w, h) * 0.45f, Theme.alpha(Theme.ACCENT_2, 0.16f));
    }

    private void paintImage(Graphics2D g, int w, int h) {
        g.setColor(Theme.BG);
        g.fillRect(0, 0, w, h);
        if (image != null) {
            int iw = image.getWidth(this);
            int ih = image.getHeight(this);
            if (iw > 0 && ih > 0) {
                double s = Math.max(w / (double) iw, h / (double) ih);
                int dw = (int) Math.ceil(iw * s);
                int dh = (int) Math.ceil(ih * s);
                g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(image, (w - dw) / 2, (h - dh) / 2, dw, dh, this);
            }
        }
        paintDim(g, w, h);
    }

    private void paintDim(Graphics2D g, int w, int h) {
        if (dim > 0) {
            g.setColor(new Color(0, 0, 0, Math.round(dim * 2.55f)));
            g.fillRect(0, 0, w, h);
        }
    }

    private void paintUniverse(Graphics2D g, int w, int h) {
        if (nebula == null || nebula.getWidth() != w || nebula.getHeight() != h || !Theme.ACCENT.equals(nebulaAccent)) {
            nebula = buildNebula(Math.max(1, w), Math.max(1, h));
            nebulaAccent = Theme.ACCENT;
        }
        g.drawImage(nebula, 0, 0, null);
        Theme.hq(g);

        float time = System.nanoTime() / 1_000_000_000f;
        float mx = 0;
        float my = 0;
        PointerInfo pi = MouseInfo.getPointerInfo();
        if (pi != null && isShowing()) {
            Point p = pi.getLocation();
            SwingUtilities.convertPointFromScreen(p, this);
            mx = (p.x / (float) Math.max(1, w) - 0.5f) * 18f;
            my = (p.y / (float) Math.max(1, h) - 0.5f) * 18f;
        }

        Color glow = Theme.mix(Color.WHITE, Theme.ACCENT, 0.4f);
        for (int i = 0; i < STARS; i++) {
            float d = depth[i];
            float x = ((sx[i] + time * 0.0025f * d) % 1f) * w + mx * d;
            float y = sy[i] * h + my * d;
            float tw = 0.55f + 0.45f * (float) Math.sin(time * (1.4f + d * 2f) + phase[i]);
            float a = tw * (0.25f + 0.75f * d);
            float size = 0.8f + d * 1.8f;
            g.setColor(new Color(1f, 1f, 1f, Math.min(1f, a)));
            g.fill(new Ellipse2D.Float(x, y, size, size));
            if (d > 0.93f) {
                g.setColor(Theme.alpha(glow, a * 0.18f));
                g.fill(new Ellipse2D.Float(x - size * 1.5f, y - size * 1.5f, size * 4, size * 4));
            }
        }

        float age = time - shootStart;
        if (age > nextShoot) {
            shootStart = time;
            shootX = w * (0.35f + random.nextFloat() * 0.6f);
            shootY = h * random.nextFloat() * 0.4f;
            nextShoot = 6f + random.nextFloat() * 6f;
            age = 0;
        }
        if (age < 1.2f) {
            float t = age / 1.2f;
            float hx = shootX - t * w * 0.3f;
            float hy = shootY + t * h * 0.22f;
            float fade = (float) Math.sin(t * Math.PI);
            g.setPaint(new GradientPaint(hx, hy, new Color(1f, 1f, 1f, 0.85f * fade), hx + 90, hy - 66, new Color(1f, 1f, 1f, 0f)));
            g.setStroke(new java.awt.BasicStroke(1.4f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
            g.draw(new java.awt.geom.Line2D.Float(hx, hy, hx + 90, hy - 66));
        }
    }

    private static BufferedImage buildNebula(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        Theme.hq(g);
        g.setPaint(new GradientPaint(0, 0, Theme.BG, 0, h, Theme.BG_2));
        g.fillRect(0, 0, w, h);
        blob(g, w * 0.78f, h * 0.22f, Math.max(w, h) * 0.55f, Theme.alpha(Theme.mix(Theme.ACCENT, Color.BLACK, 0.3f), 0.28f));
        blob(g, w * 0.12f, h * 0.9f, Math.max(w, h) * 0.5f, Theme.alpha(Theme.mix(Theme.ACCENT_2, Color.BLACK, 0.35f), 0.22f));
        blob(g, w * 0.5f, h * 0.55f, Math.max(w, h) * 0.3f, Theme.alpha(Theme.mix(Theme.ACCENT, Theme.ACCENT_2, 0.5f), 0.10f));
        g.dispose();
        return img;
    }

    private static void blob(Graphics2D g, float cx, float cy, float r, Color c) {
        g.setPaint(new RadialGradientPaint(new Point2D.Float(cx, cy), r, new float[]{0f, 1f},
                new Color[]{c, new Color(c.getRed(), c.getGreen(), c.getBlue(), 0)}));
        g.fill(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
    }
}
