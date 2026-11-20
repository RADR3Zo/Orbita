package dev.orbita.launcher.ui;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Random;

/**
 * Animación de entrada del launcher (~2,6 s): viaje a la velocidad de la luz entre estrellas, destello,
 * aparición del planeta y el nombre, y fundido hacia la interfaz. Clic o tecla para saltarla.
 * Se instala como glass pane y se retira sola al terminar; su Timer solo existe mientras dura.
 */
public final class IntroOverlay extends JComponent {
    private static final float DURATION = 2.6f;
    private static final int STARS = 420;

    private final JFrame frame;
    private final float[] angle = new float[STARS];
    private final float[] dist = new float[STARS];
    private final float[] speed = new float[STARS];
    private final Timer timer;
    private long start;
    private boolean done;

    private IntroOverlay(JFrame frame) {
        this.frame = frame;
        Random r = new Random(7);
        for (int i = 0; i < STARS; i++) {
            angle[i] = (float) (r.nextFloat() * Math.PI * 2);
            dist[i] = 0.02f + r.nextFloat() * 0.5f;
            speed[i] = 0.6f + r.nextFloat() * 1.2f;
        }
        timer = new Timer(15, e -> {
            if (elapsed() >= DURATION) {
                finish();
            } else {
                repaint();
            }
        });
        // Se ignoran clics y teclas del primer medio segundo: Windows puede entregar pulsaciones que ya
        // estaban en cola al abrir la ventana, y no deben saltarse la animación.
        MouseAdapter skip = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (elapsed() > 0.5f) {
                    finish();
                }
            }
        };
        addMouseListener(skip);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int k = e.getKeyCode();
                if (elapsed() > 0.5f && (k == KeyEvent.VK_ESCAPE || k == KeyEvent.VK_SPACE || k == KeyEvent.VK_ENTER)) {
                    finish();
                }
            }
        });
        setFocusable(true);
    }

    /** Muestra la animación sobre la ventana (llamar justo después de hacerla visible). */
    public static void play(JFrame frame) {
        IntroOverlay o = new IntroOverlay(frame);
        frame.setGlassPane(o);
        o.setVisible(true);
        o.requestFocusInWindow();
        o.start = System.nanoTime();
        o.timer.start();
    }

    private float elapsed() {
        return (System.nanoTime() - start) / 1_000_000_000f;
    }

    private void finish() {
        if (done) {
            return;
        }
        done = true;
        timer.stop();
        setVisible(false);
        frame.repaint();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.hq(g);
        int w = getWidth();
        int h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;
        float t = Math.min(DURATION, elapsed());
        float maxR = (float) Math.hypot(w, h) / 2f;

        // Fundido final hacia la interfaz
        float fadeOut = t < 2.0f ? 1f : Math.max(0f, 1f - (t - 2.0f) / 0.6f);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fadeOut));

        g.setColor(new Color(3, 4, 8));
        g.fillRect(0, 0, w, h);
        g.setPaint(new RadialGradientPaint(new Point2D.Float(cx, cy), maxR, new float[]{0f, 1f},
                new Color[]{Theme.alpha(Theme.mix(Theme.ACCENT, Color.BLACK, 0.55f), 0.55f), new Color(0, 0, 0, 0)}));
        g.fillRect(0, 0, w, h);

        // 1) Velocidad de la luz: estelas que salen del centro y aceleran (0 – 1,4 s)
        float warp = t < 1.4f ? t / 1.4f : Math.max(0f, 1f - (t - 1.4f) / 0.4f);
        float accel = (float) Math.pow(Math.min(t, 1.4f) / 1.4f, 2.2);
        g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Color starColor = Theme.mix(Color.WHITE, Theme.ACCENT_2, 0.25f);
        for (int i = 0; i < STARS; i++) {
            float d = (dist[i] + t * speed[i] * (0.15f + accel * 1.6f)) % 1.05f;
            float r1 = d * d * maxR * 1.3f;
            float len = 4f + accel * 140f * d * speed[i];
            float r0 = Math.max(0, r1 - len);
            float ca = (float) Math.cos(angle[i]);
            float sa = (float) Math.sin(angle[i]);
            float a = Math.min(1f, d * 2.2f) * (0.35f + 0.65f * warp);
            g.setColor(Theme.alpha(starColor, a));
            g.draw(new Line2D.Float(cx + ca * r0, cy + sa * r0, cx + ca * r1, cy + sa * r1));
        }

        // 2) Destello de llegada (1,3 – 1,8 s)
        if (t > 1.3f && t < 1.9f) {
            float f = 1f - Math.abs(t - 1.5f) / 0.4f;
            f = Math.max(0f, f);
            g.setPaint(new RadialGradientPaint(new Point2D.Float(cx, cy), maxR * 0.9f, new float[]{0f, 1f},
                    new Color[]{new Color(1f, 1f, 1f, 0.75f * f), new Color(1f, 1f, 1f, 0f)}));
            g.fillRect(0, 0, w, h);
        }

        // 3) Planeta y nombre (desde 1,45 s)
        if (t > 1.45f) {
            float p = Math.min(1f, (t - 1.45f) / 0.5f);
            float ease = 1f - (float) Math.pow(1f - p, 3);
            float size = 120f * (0.7f + 0.3f * ease);
            Graphics2D lg = (Graphics2D) g.create();
            lg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fadeOut * ease));
            lg.setPaint(new RadialGradientPaint(new Point2D.Float(cx, cy - 30), size * 1.4f, new float[]{0f, 1f},
                    new Color[]{Theme.alpha(Theme.ACCENT, 0.35f), new Color(0, 0, 0, 0)}));
            lg.fill(new Ellipse2D.Float(cx - size * 1.4f, cy - 30 - size * 1.4f, size * 2.8f, size * 2.8f));
            Icons.planet(lg, cx - size / 2f, cy - 30 - size / 2f, size);
            lg.setFont(Theme.tracked(Font.BOLD, 34f, 0.32f));
            FontMetrics fm = lg.getFontMetrics();
            String name = "ÓRBITA";
            lg.setColor(Theme.TEXT);
            lg.drawString(name, cx - fm.stringWidth(name) / 2f, cy + size / 2f + 14 + (1f - ease) * 12f);
            lg.setFont(Theme.font(Font.PLAIN, 13f));
            fm = lg.getFontMetrics();
            String sub = "Preparando el despegue…";
            lg.setColor(Theme.MUTED);
            lg.drawString(sub, cx - fm.stringWidth(sub) / 2f, cy + size / 2f + 40);
            lg.dispose();
        }
        g.dispose();
    }
}
