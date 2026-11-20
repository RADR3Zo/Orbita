package dev.orbita.launcher.ui.components;

import dev.orbita.launcher.core.Http;
import dev.orbita.launcher.ui.Theme;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Icono de mod: imagen remota si ImageIO puede leerla (PNG/JPG), o una tesela con la inicial. */
public final class IconTile extends JComponent {
    private static final Map<String, Optional<Image>> CACHE = new ConcurrentHashMap<>();
    private static final ExecutorService LOADER = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "orbita-icons");
        t.setDaemon(true);
        return t;
    });
    private static final HttpClient CLIENT = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();

    private final String title;
    private final int size;
    private Image image;

    public IconTile(String title, String url, int size) {
        this.title = title == null || title.isBlank() ? "?" : title;
        this.size = size;
        setOpaque(false);
        if (url != null && !url.isBlank()) {
            Optional<Image> cached = CACHE.get(url);
            if (cached != null) {
                image = cached.orElse(null);
            } else {
                LOADER.submit(() -> {
                    Image img = load(url);
                    CACHE.put(url, Optional.ofNullable(img));
                    if (img != null) {
                        SwingUtilities.invokeLater(() -> {
                            image = img;
                            repaint();
                        });
                    }
                });
            }
        }
    }

    /** Icono ya disponible en memoria (p. ej. el planeta del núcleo de Órbita). */
    public IconTile(String title, Image image, int size) {
        this.title = title == null || title.isBlank() ? "?" : title;
        this.size = size;
        this.image = image;
        setOpaque(false);
    }

    private Image load(String url) {
        try {
            HttpResponse<byte[]> res = CLIENT.send(HttpRequest.newBuilder(URI.create(url))
                    .header("User-Agent", Http.USER_AGENT).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
            if (res.statusCode() != 200) {
                return null;
            }
            // PNG/JPG/GIF los lee ImageIO de serie; WebP gracias a TwelveMonkeys (registrado por ServiceLoader).
            BufferedImage src = ImageIO.read(new java.io.ByteArrayInputStream(res.body()));
            return src == null ? null : scale(src, size * 2);
        } catch (Exception e) {
            return null;
        }
    }

    /** Escalado de alta calidad en pasos (evita el aspecto pixelado al reducir iconos grandes). */
    private static BufferedImage scale(BufferedImage src, int target) {
        BufferedImage img = src;
        int w = src.getWidth();
        int h = src.getHeight();
        do {
            w = Math.max(target, w / 2);
            h = Math.max(target, h / 2);
            BufferedImage next = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = next.createGraphics();
            g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING, java.awt.RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(img, 0, 0, w, h, null);
            g.dispose();
            img = next;
        } while (w > target || h > target);
        return img;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(size, size);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.hq(g);
        RoundRectangle2D r = new RoundRectangle2D.Float(0, 0, size, size, 12, 12);
        if (image != null) {
            g.clip(r);
            g.drawImage(image, 0, 0, size, size, null);
        } else {
            int hue = Math.abs(title.hashCode());
            Color a = Color.getHSBColor((hue % 360) / 360f, 0.45f, 0.55f);
            Color b = Theme.mix(a, Theme.ACCENT, 0.5f);
            g.setPaint(new GradientPaint(0, 0, a, size, size, b));
            g.fill(r);
            g.setColor(Color.WHITE);
            g.setFont(Theme.font(Font.BOLD, size * 0.42f));
            String initial = title.substring(0, 1).toUpperCase();
            FontMetrics fm = g.getFontMetrics();
            g.drawString(initial, (size - fm.stringWidth(initial)) / 2f, (size - fm.getHeight()) / 2f + fm.getAscent());
        }
        g.dispose();
    }
}
