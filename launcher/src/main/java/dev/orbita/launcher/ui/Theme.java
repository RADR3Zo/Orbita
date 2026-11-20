package dev.orbita.launcher.ui;

import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.font.TextAttribute;
import java.util.Map;
import java.util.Set;

/** Paleta y tipografía del launcher (mismos tonos que el cliente). */
public final class Theme {
    public static final Color BG = new Color(0x05060A);
    public static final Color BG_2 = new Color(0x0E1018);
    public static final Color SURFACE = new Color(20, 22, 30, 200);
    public static final Color SURFACE_SOLID = new Color(0x12141C);
    public static final Color SURFACE_HOVER = new Color(32, 35, 48, 230);
    public static final Color BORDER = new Color(255, 255, 255, 26);
    public static final Color TEXT = new Color(0xE8EAF2);
    public static final Color MUTED = new Color(0x8A8FA3);
    public static final Color DISABLED = new Color(0x555A6B);
    /** Color de acento del tema (cambia en Ajustes → Apariencia; toda la interfaz lo lee al pintar). */
    public static Color ACCENT = new Color(0x8B7CFF);
    /** Segundo color de los degradados, derivado del acento. */
    public static Color ACCENT_2 = new Color(0x5CAAFF);

    /** Temas predefinidos: nombre, acento y segundo color. */
    public record Preset(String name, int accent, int accent2) {
    }

    public static final Preset[] PRESETS = {
            new Preset("Violeta", 0x8B7CFF, 0x5CAAFF),
            new Preset("Océano", 0x4FA3FF, 0x3FE0D0),
            new Preset("Aurora", 0x34D399, 0x22D3EE),
            new Preset("Nebulosa", 0xF472B6, 0xA78BFA),
            new Preset("Supernova", 0xFB923C, 0xF43F5E),
            new Preset("Solar", 0xFACC15, 0xFB923C),
            new Preset("Marte", 0xEF4444, 0xF97316),
            new Preset("Plata", 0xCBD5E1, 0x94A3B8)
    };

    /** Aplica un acento; el segundo color se calcula girando el tono si no se da. */
    public static void applyAccent(Color accent, Color accent2) {
        ACCENT = accent;
        if (accent2 == null) {
            float[] hsb = Color.RGBtoHSB(accent.getRed(), accent.getGreen(), accent.getBlue(), null);
            accent2 = Color.getHSBColor((hsb[0] + 0.12f) % 1f, Math.min(1f, hsb[1] * 0.9f), Math.min(1f, hsb[2] * 1.05f));
        }
        ACCENT_2 = accent2;
        UIManager.put("TextArea.selectionBackground", alpha(ACCENT, 0.5f));
        UIManager.put("TextField.selectionBackground", alpha(ACCENT, 0.5f));
    }
    public static final Color SUCCESS = new Color(0x7CFFB2);
    public static final Color WARNING = new Color(0xFFD27C);
    public static final Color DANGER = new Color(0xFF6E78);

    private static final String FAMILY = pickFamily("Segoe UI", "Inter", "Helvetica Neue");

    private Theme() {
    }

    public static Font font(int style, float size) {
        return new Font(FAMILY, style, 1).deriveFont(size);
    }

    /** Fuente con espaciado entre letras, para títulos tipo "Ó R B I T A". */
    public static Font tracked(int style, float size, float tracking) {
        return font(style, size).deriveFont(Map.of(TextAttribute.TRACKING, tracking));
    }

    public static Color alpha(Color c, float a) {
        int alpha = Math.round(c.getAlpha() * Math.max(0f, Math.min(1f, a)));
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    public static Color mix(Color a, Color b, float t) {
        float k = Math.max(0f, Math.min(1f, t));
        return new Color(
                Math.round(a.getRed() + (b.getRed() - a.getRed()) * k),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * k),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * k),
                Math.round(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * k));
    }

    public static void hq(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    }

    /** Colores por defecto de Swing para diálogos, tooltips y selección de texto. */
    public static void installDefaults() {
        UIManager.put("ToolTip.background", SURFACE_SOLID);
        UIManager.put("ToolTip.foreground", TEXT);
        UIManager.put("ToolTip.border", javax.swing.BorderFactory.createLineBorder(new Color(0x2A2D3A)));
        UIManager.put("ToolTip.font", font(Font.PLAIN, 12));
        UIManager.put("Panel.background", BG);
        UIManager.put("OptionPane.background", SURFACE_SOLID);
        UIManager.put("OptionPane.messageForeground", TEXT);
        UIManager.put("TextArea.selectionBackground", alpha(ACCENT, 0.5f));
        UIManager.put("TextField.selectionBackground", alpha(ACCENT, 0.5f));
        UIManager.put("TextField.selectionForeground", TEXT);
    }

    private static String pickFamily(String... names) {
        Set<String> available = Set.of(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        for (String n : names) {
            if (available.contains(n)) {
                return n;
            }
        }
        return Font.SANS_SERIF;
    }
}
