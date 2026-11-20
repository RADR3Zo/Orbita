package dev.orbita.client.ui.render;

import com.mojang.blaze3d.platform.NativeImage;
import dev.orbita.client.OrbitaClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Iconos propios de cada mod del panel (blancos y suavizados, dibujados una sola vez en una textura de 64 px):
 * "FPS" en letras para FPS, un muñequito para Emotes, un reloj para Reloj, alas para Alas, etc.
 */
public final class ModIcons {
    public static final int SIZE = 64;
    private static final Map<String, Identifier> CACHE = new HashMap<>();
    private static final float W = 4.6f; // grosor del trazo

    private ModIcons() {
    }

    /** Textura del icono (o null si ese mod no tiene uno propio: se usa el objeto de Minecraft). */
    public static Identifier get(String key) {
        if (key == null) {
            return null;
        }
        if (CACHE.containsKey(key)) {
            return CACHE.get(key);
        }
        Identifier id = null;
        BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(W, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        boolean drawn;
        try {
            drawn = paint(key, g);
        } catch (Exception e) {
            drawn = false;
        }
        g.dispose();
        if (drawn) {
            try {
                NativeImage n = new NativeImage(SIZE, SIZE, true);
                for (int y = 0; y < SIZE; y++) {
                    for (int x = 0; x < SIZE; x++) {
                        n.setPixel(x, y, img.getRGB(x, y));
                    }
                }
                id = OrbitaClient.id("modicon/" + key.toLowerCase(java.util.Locale.ROOT));
                Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> "orbita_modicon", n));
            } catch (Exception e) {
                id = null;
            }
        }
        CACHE.put(key, id);
        return id;
    }

    // ------------------------------------------------------------------ utilidades de dibujo

    private static void line(Graphics2D g, double x1, double y1, double x2, double y2) {
        g.draw(new Line2D.Double(x1, y1, x2, y2));
    }

    private static void circle(Graphics2D g, double cx, double cy, double r, boolean fill) {
        Shape s = new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2);
        if (fill) {
            g.fill(s);
        } else {
            g.draw(s);
        }
    }

    private static void rrect(Graphics2D g, double x, double y, double w, double h, double r, boolean fill) {
        Shape s = new RoundRectangle2D.Double(x, y, w, h, r * 2, r * 2);
        if (fill) {
            g.fill(s);
        } else {
            g.draw(s);
        }
    }

    private static void text(Graphics2D g, String s, float size, double cy) {
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 1).deriveFont(size));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(s, (SIZE - fm.stringWidth(s)) / 2f, (float) (cy + (fm.getAscent() - fm.getDescent()) / 2.0));
    }

    private static Path2D poly(double... xy) {
        Path2D.Double p = new Path2D.Double();
        p.moveTo(xy[0], xy[1]);
        for (int i = 2; i < xy.length; i += 2) {
            p.lineTo(xy[i], xy[i + 1]);
        }
        p.closePath();
        return p;
    }

    private static void star(Graphics2D g, double cx, double cy, double r, boolean fill) {
        Path2D.Double p = new Path2D.Double();
        for (int i = 0; i < 8; i++) {
            double a = -Math.PI / 2 + i * Math.PI / 4;
            double rr = i % 2 == 0 ? r : r * 0.38;
            double x = cx + Math.cos(a) * rr, y = cy + Math.sin(a) * rr;
            if (i == 0) {
                p.moveTo(x, y);
            } else {
                p.lineTo(x, y);
            }
        }
        p.closePath();
        if (fill) {
            g.fill(p);
        } else {
            g.draw(p);
        }
    }

    private static void cloud(Graphics2D g, double ox, double oy, double s) {
        Path2D.Double p = new Path2D.Double();
        p.moveTo(ox + 10 * s, oy + 40 * s);
        p.curveTo(ox + 2 * s, oy + 40 * s, ox + 2 * s, oy + 27 * s, ox + 12 * s, oy + 27 * s);
        p.curveTo(ox + 12 * s, oy + 15 * s, ox + 30 * s, oy + 11 * s, ox + 34 * s, oy + 22 * s);
        p.curveTo(ox + 40 * s, oy + 16 * s, ox + 52 * s, oy + 20 * s, ox + 50 * s, oy + 29 * s);
        p.curveTo(ox + 60 * s, oy + 29 * s, ox + 60 * s, oy + 40 * s, ox + 52 * s, oy + 40 * s);
        p.closePath();
        g.draw(p);
    }

    private static void slash(Graphics2D g) {
        g.setStroke(new BasicStroke(W + 1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        line(g, 12, 52, 52, 12);
        g.setStroke(new BasicStroke(W, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    }

    private static void person(Graphics2D g, double cx, double top, double s) {
        circle(g, cx, top + 7 * s, 6 * s, true);
        line(g, cx, top + 14 * s, cx, top + 32 * s);
        line(g, cx, top + 32 * s, cx - 9 * s, top + 46 * s);
        line(g, cx, top + 32 * s, cx + 9 * s, top + 46 * s);
    }

    // ------------------------------------------------------------------ iconos

    private static boolean paint(String key, Graphics2D g) {
        switch (key) {
            // ---- HUD
            case "fps" -> text(g, "FPS", 25f, 32);
            case "cps" -> { // ratón con clics
                rrect(g, 20, 14, 24, 38, 12, false);
                line(g, 32, 14, 32, 28);
                line(g, 20, 28, 44, 28);
                line(g, 10, 12, 15, 17);
                line(g, 54, 12, 49, 17);
                line(g, 32, 4, 32, 8);
            }
            case "ping" -> { // barras de señal
                for (int i = 0; i < 4; i++) {
                    double h = 10 + i * 10;
                    rrect(g, 10 + i * 12, 52 - h, 8, h, 2, true);
                }
            }
            case "coords" -> { // ejes X Y Z
                line(g, 22, 44, 22, 10);
                line(g, 22, 44, 56, 44);
                line(g, 22, 44, 8, 58);
                g.fill(poly(22, 4, 16, 14, 28, 14));
                g.fill(poly(62, 44, 52, 38, 52, 50));
                g.fill(poly(4, 62, 7, 51, 15, 59));
            }
            case "direction" -> { // brújula
                circle(g, 32, 32, 24, false);
                g.fill(poly(32, 12, 38, 32, 26, 32));
                g.draw(poly(32, 52, 38, 32, 26, 32));
            }
            case "keystrokes" -> { // W A S D
                rrect(g, 24, 8, 16, 16, 4, false);
                rrect(g, 6, 28, 16, 16, 4, false);
                rrect(g, 24, 28, 16, 16, 4, false);
                rrect(g, 42, 28, 16, 16, 4, false);
                rrect(g, 6, 48, 52, 9, 4, true);
            }
            case "togglesprint" -> { // persona corriendo con líneas de velocidad
                circle(g, 40, 12, 6, true);
                line(g, 37, 20, 31, 36);
                line(g, 31, 36, 40, 44);
                line(g, 40, 44, 38, 56);
                line(g, 31, 36, 22, 46);
                line(g, 22, 46, 14, 46);
                line(g, 35, 24, 46, 30);
                line(g, 35, 24, 26, 22);
                line(g, 4, 26, 16, 26);
                line(g, 8, 34, 18, 34);
            }
            case "potions" -> { // frasco
                Path2D.Double p = new Path2D.Double();
                p.moveTo(26, 8);
                p.lineTo(26, 24);
                p.curveTo(12, 30, 10, 56, 32, 56);
                p.curveTo(54, 56, 52, 30, 38, 24);
                p.lineTo(38, 8);
                g.draw(p);
                line(g, 22, 8, 42, 8);
                g.fill(new Arc2D.Double(15, 30, 34, 26, 180, 180, Arc2D.CHORD));
            }
            case "armor" -> // peto
                    g.draw(poly(14, 10, 24, 6, 28, 12, 36, 12, 40, 6, 50, 10, 56, 24, 48, 28, 48, 56, 16, 56, 16, 28, 8, 24));
            case "spotify" -> { // círculo con ondas
                circle(g, 32, 32, 26, false);
                g.draw(new Arc2D.Double(14, 20, 36, 18, 20, 140, Arc2D.OPEN));
                g.draw(new Arc2D.Double(18, 30, 28, 14, 20, 140, Arc2D.OPEN));
                g.draw(new Arc2D.Double(22, 40, 20, 10, 20, 140, Arc2D.OPEN));
            }
            case "lyrics" -> { // nota musical y renglones
                line(g, 26, 10, 26, 40);
                line(g, 26, 10, 42, 14);
                circle(g, 20, 42, 7, true);
                line(g, 40, 30, 58, 30);
                line(g, 40, 40, 58, 40);
                line(g, 8, 54, 58, 54);
            }
            case "clock" -> { // reloj
                circle(g, 32, 32, 25, false);
                line(g, 32, 32, 32, 16);
                line(g, 32, 32, 44, 38);
                circle(g, 32, 32, 3, true);
            }
            case "memory" -> { // chip de RAM
                rrect(g, 8, 18, 48, 24, 4, false);
                for (int i = 0; i < 5; i++) {
                    line(g, 14 + i * 9, 42, 14 + i * 9, 52);
                    rrect(g, 13 + i * 9, 24, 4, 12, 1, true);
                }
            }
            case "server" -> { // servidores
                rrect(g, 8, 10, 48, 18, 5, false);
                rrect(g, 8, 36, 48, 18, 5, false);
                circle(g, 18, 19, 3, true);
                circle(g, 18, 45, 3, true);
                line(g, 30, 19, 46, 19);
                line(g, 30, 45, 46, 45);
            }
            case "speed" -> { // velocímetro
                g.draw(new Arc2D.Double(8, 12, 48, 48, -20, 220, Arc2D.OPEN));
                line(g, 32, 36, 46, 22);
                circle(g, 32, 36, 4, true);
            }
            case "biome" -> { // montañas y sol
                g.draw(poly(4, 54, 24, 22, 36, 40, 44, 30, 60, 54));
                circle(g, 46, 14, 6, true);
            }
            case "day" -> { // sol
                circle(g, 32, 32, 11, true);
                for (int i = 0; i < 8; i++) {
                    double a = i * Math.PI / 4;
                    line(g, 32 + Math.cos(a) * 18, 32 + Math.sin(a) * 18, 32 + Math.cos(a) * 26, 32 + Math.sin(a) * 26);
                }
            }
            case "combo" -> { // x3 con chispa
                text(g, "x3", 28f, 36);
                star(g, 52, 12, 9, true);
            }
            case "reach" -> { // distancia entre dos marcas
                line(g, 8, 14, 8, 50);
                line(g, 56, 14, 56, 50);
                line(g, 14, 32, 50, 32);
                g.fill(poly(14, 32, 22, 26, 22, 38));
                g.fill(poly(50, 32, 42, 26, 42, 38));
            }
            case "session" -> { // reloj de arena
                line(g, 16, 8, 48, 8);
                line(g, 16, 56, 48, 56);
                g.draw(poly(20, 8, 44, 8, 32, 32, 44, 56, 20, 56, 32, 32));
                g.fill(poly(24, 52, 40, 52, 32, 42));
            }
            case "light" -> { // bombilla
                g.draw(new Arc2D.Double(14, 6, 36, 36, -40, 260, Arc2D.OPEN));
                line(g, 24, 40, 24, 46);
                line(g, 40, 40, 40, 46);
                line(g, 24, 48, 40, 48);
                line(g, 26, 55, 38, 55);
            }
            case "target" -> { // mira
                circle(g, 32, 32, 20, false);
                circle(g, 32, 32, 4, true);
                line(g, 32, 4, 32, 18);
                line(g, 32, 46, 32, 60);
                line(g, 4, 32, 18, 32);
                line(g, 46, 32, 60, 32);
            }
            case "gaps" -> { // manzana
                Path2D.Double p = new Path2D.Double();
                p.moveTo(32, 20);
                p.curveTo(20, 12, 8, 22, 12, 38);
                p.curveTo(15, 52, 26, 58, 32, 54);
                p.curveTo(38, 58, 49, 52, 52, 38);
                p.curveTo(56, 22, 44, 12, 32, 20);
                g.fill(p);
                line(g, 32, 20, 34, 8);
                g.fill(new Ellipse2D.Double(36, 6, 12, 7));
            }
            // ---- Órbita
            case "zoom" -> { // lupa con +
                circle(g, 26, 26, 17, false);
                line(g, 39, 39, 56, 56);
                line(g, 18, 26, 34, 26);
                line(g, 26, 18, 26, 34);
            }
            case "freelook" -> { // ojo
                Path2D.Double p = new Path2D.Double();
                p.moveTo(4, 32);
                p.curveTo(18, 12, 46, 12, 60, 32);
                p.curveTo(46, 52, 18, 52, 4, 32);
                g.draw(p);
                circle(g, 32, 32, 9, true);
            }
            case "fullbright" -> { // sol con rayos
                circle(g, 32, 32, 11, true);
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI / 4 * i;
                    line(g, 32 + Math.cos(a) * 18, 32 + Math.sin(a) * 18, 32 + Math.cos(a) * 27, 32 + Math.sin(a) * 27);
                }
            }
            case "profile" -> { // percha (guardarropa)
                g.draw(new Arc2D.Double(25, 6, 14, 14, -90, 270, Arc2D.OPEN));
                g.draw(poly(32, 20, 58, 42, 6, 42));
                line(g, 6, 48, 58, 48);
            }
            // ---- Nova: cosméticos
            case "Wings" -> { // alas
                for (int side = -1; side <= 1; side += 2) {
                    Path2D.Double p = new Path2D.Double();
                    p.moveTo(32, 30);
                    p.curveTo(32 + side * 10, 8, 32 + side * 28, 6, 32 + side * 29, 14);
                    p.curveTo(32 + side * 28, 28, 32 + side * 22, 40, 32, 42);
                    g.draw(p);
                    line(g, 32 + side * 8, 26, 32 + side * 22, 16);
                    line(g, 32 + side * 7, 34, 32 + side * 20, 30);
                }
            }
            case "Cape" -> { // capa
                Path2D.Double p = new Path2D.Double();
                p.moveTo(20, 8);
                p.lineTo(44, 8);
                p.lineTo(54, 52);
                p.curveTo(46, 58, 42, 50, 36, 56);
                p.curveTo(30, 50, 24, 58, 18, 54);
                p.curveTo(14, 56, 12, 54, 10, 52);
                p.closePath();
                g.draw(p);
                line(g, 26, 8, 26, 16);
                line(g, 38, 8, 38, 16);
            }
            case "Halo" -> { // aro sobre la cabeza
                g.draw(new Ellipse2D.Double(14, 6, 36, 12));
                circle(g, 32, 38, 13, true);
                line(g, 18, 58, 46, 58);
            }
            case "Crown" -> { // corona
                g.draw(poly(8, 48, 8, 20, 20, 34, 32, 14, 44, 34, 56, 20, 56, 48));
                line(g, 8, 56, 56, 56);
                circle(g, 32, 14, 4, true);
            }
            case "Hat" -> { // sombrero de copa
                rrect(g, 18, 8, 28, 36, 4, false);
                line(g, 6, 46, 58, 46);
                rrect(g, 18, 34, 28, 6, 1, true);
            }
            case "Bandana" -> { // pañuelo con nudo
                g.draw(poly(6, 18, 58, 18, 32, 52));
                line(g, 6, 18, 2, 30);
                line(g, 58, 18, 62, 30);
                circle(g, 32, 28, 3, true);
                circle(g, 22, 24, 2.5, true);
                circle(g, 42, 24, 2.5, true);
            }
            case "Pet" -> { // huella
                g.fill(new Ellipse2D.Double(18, 30, 28, 24));
                circle(g, 14, 26, 6, true);
                circle(g, 26, 14, 6, true);
                circle(g, 40, 14, 6, true);
                circle(g, 51, 26, 6, true);
            }
            case "Coins" -> { // monedas
                g.draw(new Ellipse2D.Double(10, 38, 34, 14));
                g.draw(new Ellipse2D.Double(10, 28, 34, 14));
                g.draw(new Ellipse2D.Double(10, 18, 34, 14));
                circle(g, 46, 18, 12, false);
                text(g, "", 1f, 0);
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
                g.drawString("$", 42, 23);
            }
            case "Trail" -> { // estela de estrellas
                star(g, 46, 18, 13, true);
                star(g, 28, 34, 9, true);
                star(g, 14, 50, 6, true);
            }
            case "AuraRings" -> { // anillos de aura alrededor de la persona
                person(g, 32, 8, 0.95);
                g.draw(new Ellipse2D.Double(6, 40, 52, 14));
                g.draw(new Ellipse2D.Double(14, 24, 36, 10));
            }
            case "GroundSigil" -> { // sello en el suelo
                g.draw(new Ellipse2D.Double(4, 22, 56, 26));
                g.draw(poly(32, 24, 46, 44, 12, 30, 52, 30, 18, 44));
            }
            case "Emotes" -> { // muñequito saludando
                circle(g, 32, 12, 7, true);
                line(g, 32, 20, 32, 40);
                line(g, 32, 40, 22, 56);
                line(g, 32, 40, 42, 56);
                line(g, 32, 26, 18, 34);
                line(g, 32, 26, 46, 16);
                line(g, 46, 16, 48, 6);
            }
            // ---- Nova: visual
            case "SkyModule" -> { // luna y estrellas
                Path2D.Double moon = new Path2D.Double();
                moon.append(new Arc2D.Double(10, 12, 40, 40, 60, 240, Arc2D.OPEN), false);
                moon.curveTo(28, 44, 26, 22, 40, 14.7);
                g.fill(moon);
                star(g, 50, 16, 7, true);
                star(g, 52, 44, 5, true);
            }
            case "VideoBackground" -> { // pantalla con play
                rrect(g, 6, 12, 52, 38, 7, false);
                g.fill(poly(26, 21, 26, 41, 42, 31));
                line(g, 22, 58, 42, 58);
            }
            case "CritEffects" -> { // estallido de golpe crítico
                star(g, 32, 32, 26, false);
                star(g, 32, 32, 10, true);
            }
            case "Nametags" -> { // etiqueta
                g.draw(poly(8, 20, 42, 20, 58, 32, 42, 44, 8, 44));
                circle(g, 44, 32, 3.5, true);
                line(g, 16, 32, 32, 32);
            }
            case "NameChanger" -> { // Aa con lápiz
                text(g, "Aa", 22f, 26);
                line(g, 18, 54, 46, 42);
                g.fill(poly(10, 58, 16, 50, 20, 56));
            }
            // ---- Nova: rendimiento
            case "UnlockFps" -> { // candado abierto
                rrect(g, 12, 30, 40, 28, 6, false);
                g.draw(new Arc2D.Double(16, 6, 26, 30, 0, 180, Arc2D.OPEN));
                line(g, 16, 21, 16, 26);
                circle(g, 32, 44, 4, true);
            }
            case "FpsBoost" -> // rayo
                    g.fill(poly(36, 4, 12, 36, 30, 36, 24, 60, 52, 24, 34, 24, 42, 4));
            case "SmartFps" -> { // velocímetro con A de automático
                g.draw(new Arc2D.Double(6, 10, 52, 52, -15, 210, Arc2D.OPEN));
                text(g, "A", 22f, 40);
            }
            case "LiteMode" -> { // pluma
                Path2D.Double p = new Path2D.Double();
                p.moveTo(10, 56);
                p.curveTo(18, 30, 34, 10, 56, 6);
                p.curveTo(52, 28, 36, 46, 14, 52);
                g.draw(p);
                line(g, 10, 56, 40, 24);
            }
            case "NoParticles" -> { // chispas tachadas
                star(g, 24, 24, 11, true);
                star(g, 44, 40, 8, true);
                star(g, 18, 46, 5, true);
                slash(g);
            }
            case "NoClouds" -> { // nube tachada
                cloud(g, 2, 6, 1.0);
                slash(g);
            }
            case "NoWeather" -> { // lluvia tachada
                cloud(g, 2, -4, 1.0);
                line(g, 20, 44, 16, 54);
                line(g, 32, 44, 28, 54);
                line(g, 44, 44, 40, 54);
                slash(g);
            }
            case "ClientSettings" -> { // modo ahorro: batería con hoja
                rrect(g, 6, 18, 46, 28, 6, false);
                rrect(g, 54, 26, 5, 12, 2, true);
                Path2D.Double leaf = new Path2D.Double();
                leaf.moveTo(18, 40);
                leaf.curveTo(18, 26, 30, 22, 42, 24);
                leaf.curveTo(40, 36, 30, 42, 18, 40);
                g.fill(leaf);
            }
            default -> {
                return false;
            }
        }
        return true;
    }
}
