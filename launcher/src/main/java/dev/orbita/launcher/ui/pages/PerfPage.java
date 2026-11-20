package dev.orbita.launcher.ui.pages;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.orbita.launcher.Launcher;
import dev.orbita.launcher.core.GamePaths;
import dev.orbita.launcher.core.LauncherConfig;
import dev.orbita.launcher.ui.Theme;
import dev.orbita.launcher.ui.components.Card;
import dev.orbita.launcher.ui.components.OButton;
import dev.orbita.launcher.ui.components.UiUtil;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.geom.Path2D;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Página Rendimiento (traída de Nova): los FPS de cada partida que anota el mod, datos de tu equipo, perfiles de
 * gráficos para options.txt y la RAM recomendada.
 */
public final class PerfPage extends JPanel {
    /** Partida guardada por el mod (config/nova-fps.json). */
    private record Session(double avg, double low, double max, int mode, long time, double seconds, String mc) {
    }

    private record Hw(int ramGB, int threads, String gpu, String kind) {
    }

    private static final Map<String, Map<String, String>> PROFILES = new LinkedHashMap<>();

    static {
        PROFILES.put("performance", map("renderDistance", "8", "simulationDistance", "6", "maxFps", "260", "enableVsync", "false",
                "mipmapLevels", "0", "particles", "2", "entityShadows", "false", "renderClouds", "\"false\"", "biomeBlendRadius", "0",
                "graphicsMode", "0", "viewBobbing", "false"));
        PROFILES.put("balanced", map("renderDistance", "10", "simulationDistance", "8", "maxFps", "260", "enableVsync", "false",
                "mipmapLevels", "2", "particles", "1", "entityShadows", "true", "renderClouds", "\"fast\"", "biomeBlendRadius", "1",
                "graphicsMode", "0", "viewBobbing", "true"));
        PROFILES.put("quality", map("renderDistance", "14", "simulationDistance", "10", "maxFps", "260", "enableVsync", "false",
                "mipmapLevels", "4", "particles", "0", "entityShadows", "true", "renderClouds", "\"true\"", "biomeBlendRadius", "2",
                "graphicsMode", "1", "viewBobbing", "true"));
    }

    private static final Map<String, String[]> NAMES = Map.of(
            "auto", new String[]{"Automático", "Según tu equipo"},
            "performance", new String[]{"Rendimiento", "Menos distancia y detalles para más FPS."},
            "balanced", new String[]{"Equilibrado", "Se ve bien y mantiene los FPS."},
            "quality", new String[]{"Calidad", "Más distancia, detalle y efectos."});

    private final Launcher launcher;
    private final JFrame frame;
    private final JPanel tiles = new JPanel(new GridLayout(1, 4, 12, 0));
    private final Chart chart = new Chart();
    private final JPanel sessions = new JPanel();
    private final JPanel hwBox = new JPanel();
    private final JPanel profiles = new JPanel();
    private final JPanel ram = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    private final JLabel toast = UiUtil.label(" ", Font.BOLD, 12f, Theme.SUCCESS);
    private Hw hw;

    public PerfPage(Launcher launcher, JFrame frame) {
        this.launcher = launcher;
        this.frame = frame;
        setOpaque(false);
        setLayout(new BorderLayout(0, 18));
        setBorder(BorderFactory.createEmptyBorder(4, 40, 30, 40));

        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        JLabel eyebrow = new UiUtil.AccentLabel("RENDIMIENTO");
        eyebrow.setFont(Theme.tracked(Font.BOLD, 12f, 0.18f));
        head.add(UiUtil.left(eyebrow));
        head.add(UiUtil.vgap(6));
        head.add(UiUtil.left(UiUtil.label("Tus FPS y tu equipo", Font.BOLD, 34f, Theme.TEXT)));
        head.add(UiUtil.vgap(4));
        head.add(UiUtil.left(toast));
        add(head, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

        Card fps = card("TUS FPS");
        tiles.setOpaque(false);
        tiles.setMaximumSize(new Dimension(Integer.MAX_VALUE, 92));
        fps.add(UiUtil.left(tiles));
        fps.add(UiUtil.vgap(12));
        chart.setPreferredSize(new Dimension(100, 130));
        chart.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
        fps.add(UiUtil.left(chart));
        fps.add(UiUtil.vgap(10));
        sessions.setOpaque(false);
        sessions.setLayout(new BoxLayout(sessions, BoxLayout.Y_AXIS));
        fps.add(UiUtil.left(sessions));
        fps.add(UiUtil.vgap(8));
        fps.add(UiUtil.left(UiUtil.label("Órbita anota los FPS de cada partida y los guarda al salir del mundo.", Font.PLAIN, 12f, Theme.DISABLED)));
        fps.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        body.add(UiUtil.left(fps));
        body.add(UiUtil.vgap(14));

        JPanel row = new JPanel(new GridLayout(1, 3, 14, 0));
        row.setOpaque(false);
        Card hwCard = card("TU EQUIPO");
        hwBox.setOpaque(false);
        hwBox.setLayout(new BoxLayout(hwBox, BoxLayout.Y_AXIS));
        hwBox.add(UiUtil.left(UiUtil.muted("Leyendo el equipo…")));
        hwCard.add(UiUtil.left(hwBox));
        row.add(hwCard);
        Card prof = card("PERFIL DE GRÁFICOS");
        prof.add(UiUtil.left(UiUtil.label("Se aplica a options.txt de Órbita.", Font.PLAIN, 12f, Theme.MUTED)));
        prof.add(UiUtil.vgap(8));
        profiles.setOpaque(false);
        profiles.setLayout(new BoxLayout(profiles, BoxLayout.Y_AXIS));
        prof.add(UiUtil.left(profiles));
        row.add(prof);
        Card mem = card("MEMORIA PARA MINECRAFT");
        ram.setOpaque(false);
        mem.add(UiUtil.left(ram));
        mem.add(UiUtil.vgap(10));
        mem.add(UiUtil.left(UiUtil.muted("<html>Más RAM no da más FPS: solo se la quita a Windows. Lo recomendado alcanza.</html>")));
        row.add(mem);
        body.add(UiUtil.left(row));

        add(UiUtil.scroll(body), BorderLayout.CENTER);
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                refresh();
            }
        });
        refresh();
    }

    private static Card card(String title) {
        Card c = new Card(null, 14);
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        c.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        JLabel t = new UiUtil.AccentLabel(title);
        t.setFont(Theme.tracked(Font.BOLD, 12f, 0.15f));
        c.add(UiUtil.left(t));
        c.add(UiUtil.vgap(12));
        return c;
    }

    private void refresh() {
        renderFps(readSessions());
        renderRam();
        renderProfiles();
        if (hw == null) {
            new SwingWorker<Hw, Void>() {
                @Override
                protected Hw doInBackground() {
                    return detect();
                }

                @Override
                protected void done() {
                    try {
                        hw = get();
                        renderHw();
                        renderProfiles();
                    } catch (Exception ignored) {
                        // sin datos
                    }
                }
            }.execute();
        }
    }

    // ------------------------------------------------------------------ FPS

    private static List<Session> readSessions() {
        List<Session> out = new ArrayList<>();
        Path f = GamePaths.root().resolve("config").resolve("nova-fps.json");
        try {
            JsonArray a = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonArray();
            for (JsonElement e : a) {
                JsonObject o = e.getAsJsonObject();
                if (o.has("avg")) {
                    out.add(new Session(num(o, "avg"), num(o, "low"), num(o, "max"), (int) num(o, "mode"), (long) num(o, "time"),
                            num(o, "seconds"), o.has("mc") ? o.get("mc").getAsString() : "?"));
                }
            }
        } catch (Exception ignored) {
            // todavía no hay partidas
        }
        out.sort((x, y) -> Long.compare(y.time(), x.time()));
        return out;
    }

    private static double num(JsonObject o, String k) {
        try {
            return o.has(k) ? o.get(k).getAsDouble() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private void renderFps(List<Session> list) {
        tiles.removeAll();
        sessions.removeAll();
        if (list.isEmpty()) {
            tiles.setVisible(false);
            chart.set(List.of());
            sessions.add(UiUtil.left(UiUtil.muted("Todavía no hay partidas. Juega un rato y al salir del mundo verás tus FPS aquí.")));
        } else {
            tiles.setVisible(true);
            Session last = list.get(0);
            Session prev = list.stream().skip(1).filter(s -> s.mc().equals(last.mc())).findFirst().orElse(null);
            String diff = "";
            Color dc = Theme.MUTED;
            if (prev != null) {
                long d = Math.round((last.avg() - prev.avg()) / Math.max(1, prev.avg()) * 100);
                diff = (d >= 0 ? "▲ " : "▼ ") + Math.abs(d) + " % que la anterior";
                dc = d >= 0 ? Theme.SUCCESS : Theme.DANGER;
            }
            tiles.add(tile("PROMEDIO", Math.round(last.avg()) + " FPS", diff, dc));
            tiles.add(tile("1 % MÁS BAJO", Math.round(last.low()) + " FPS", "Si es muy bajo, se sienten tirones", Theme.DISABLED));
            tiles.add(tile("MÁXIMO", Math.round(last.max()) + " FPS", "", Theme.MUTED));
            tiles.add(tile("PARTIDAS", String.valueOf(list.size()), "", Theme.MUTED));
            SimpleDateFormat fmt = new SimpleDateFormat("dd/MM HH:mm", Locale.forLanguageTag("es"));
            sessions.add(UiUtil.left(fpsRow(true, "CUÁNDO", "VERSIÓN", "MODO", "PROMEDIO", "1 % BAJO", "TIEMPO")));
            String[] modes = {"Normal", "Ahorro medio", "Ahorro extra"};
            for (Session s : list.subList(0, Math.min(12, list.size()))) {
                sessions.add(UiUtil.vgap(4));
                sessions.add(UiUtil.left(fpsRow(false, fmt.format(new Date(s.time())), s.mc(), modes[Math.max(0, Math.min(2, s.mode()))],
                        String.valueOf(Math.round(s.avg())), String.valueOf(Math.round(s.low())), Math.round(s.seconds() / 60) + " min")));
            }
            List<Session> recent = new ArrayList<>(list.subList(0, Math.min(20, list.size())));
            Collections.reverse(recent);
            chart.set(recent);
        }
        tiles.revalidate();
        sessions.revalidate();
        repaint();
    }

    private static JComponent tile(String key, String value, String sub, Color subColor) {
        Card c = new Card(null, 12);
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        c.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        c.add(UiUtil.left(UiUtil.label(key, Font.BOLD, 11f, Theme.MUTED)));
        c.add(UiUtil.vgap(4));
        c.add(UiUtil.left(UiUtil.label(value, Font.BOLD, 22f, Theme.TEXT)));
        if (!sub.isEmpty()) {
            c.add(UiUtil.vgap(2));
            c.add(UiUtil.left(UiUtil.label(sub, Font.BOLD, 11f, subColor)));
        }
        return c;
    }

    private static JComponent fpsRow(boolean head, String... cols) {
        JPanel r = new JPanel(new GridLayout(1, cols.length, 10, 0));
        r.setOpaque(false);
        r.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        for (int i = 0; i < cols.length; i++) {
            r.add(UiUtil.label(cols[i], head ? Font.BOLD : (i == 3 ? Font.BOLD : Font.PLAIN), head ? 10f : 12f,
                    head ? Theme.DISABLED : i == 2 ? Theme.ACCENT : Theme.TEXT));
        }
        return r;
    }

    // ------------------------------------------------------------------ equipo y perfiles

    private static Hw detect() {
        int ramGB = (int) Math.round(LauncherConfig.totalMemoryMb() / 1024.0);
        int threads = Runtime.getRuntime().availableProcessors();
        List<String> names = new ArrayList<>();
        try {
            Process p = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-Command",
                    "(Get-CimInstance Win32_VideoController).Name").redirectErrorStream(true).start();
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            p.waitFor(8, TimeUnit.SECONDS);
            for (String s : out.split("\\r?\\n")) {
                if (!s.isBlank()) {
                    names.add(s.trim());
                }
            }
        } catch (Exception ignored) {
            // sin PowerShell
        }
        String dedicated = names.stream().filter(n -> kindOf(n).equals("dedicated")).findFirst().orElse(null);
        String gpu = dedicated != null ? dedicated : names.isEmpty() ? "" : names.get(0);
        return new Hw(ramGB, threads, gpu, kindOf(gpu));
    }

    private static String kindOf(String name) {
        String n = name == null ? "" : name.toLowerCase(Locale.ROOT);
        if (n.matches(".*(nvidia|geforce|quadro|rtx|gtx|\\bmx\\d|radeon (rx|pro|r[579])|\\barc a\\d).*")) {
            return "dedicated";
        }
        if (n.matches(".*(intel|radeon\\(tm\\) graphics|radeon graphics|vega \\d|uhd|iris|microsoft basic).*")) {
            return "integrated";
        }
        return "unknown";
    }

    private static String profileFor(Hw h) {
        if (h.kind().equals("integrated") || h.ramGB() <= 8 || h.threads() <= 4) {
            return "performance";
        }
        if (h.kind().equals("dedicated") && h.ramGB() >= 16 && h.threads() >= 8) {
            return "quality";
        }
        return "balanced";
    }

    private static int recommendedRam(int totalGB) {
        return totalGB <= 4 ? 2 : totalGB <= 8 ? 3 : totalGB <= 16 ? 4 : 6;
    }

    private void renderHw() {
        hwBox.removeAll();
        String kind = hw.kind().equals("dedicated") ? " (dedicada)" : hw.kind().equals("integrated") ? " (integrada)" : "";
        String[][] rows = {{"Tarjeta gráfica", (hw.gpu().isEmpty() ? "No se pudo leer" : hw.gpu()) + kind},
                {"RAM", hw.ramGB() + " GB"}, {"Hilos del procesador", String.valueOf(hw.threads())},
                {"Recomendado", NAMES.get(profileFor(hw))[0] + " · " + recommendedRam(hw.ramGB()) + " GB RAM"}};
        for (String[] r : rows) {
            JPanel line = new JPanel(new BorderLayout(10, 0));
            line.setOpaque(false);
            line.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
            line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            line.add(UiUtil.label(r[0], Font.PLAIN, 13f, Theme.MUTED), BorderLayout.WEST);
            JLabel v = UiUtil.label(r[1], Font.BOLD, 13f, Theme.TEXT);
            v.setHorizontalAlignment(JLabel.RIGHT);
            v.setToolTipText(r[1]);
            line.add(v, BorderLayout.CENTER);
            hwBox.add(UiUtil.left(line));
        }
        hwBox.revalidate();
        hwBox.repaint();
    }

    private void renderProfiles() {
        profiles.removeAll();
        for (String p : List.of("auto", "performance", "balanced", "quality")) {
            JPanel line = new JPanel(new BorderLayout(8, 0));
            line.setOpaque(false);
            line.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
            line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
            JPanel txt = new JPanel();
            txt.setOpaque(false);
            txt.setLayout(new BoxLayout(txt, BoxLayout.Y_AXIS));
            txt.add(UiUtil.left(UiUtil.label(NAMES.get(p)[0] + (p.equals("auto") ? "  ·  recomendado" : ""), Font.BOLD, 13f,
                    p.equals("auto") ? Theme.ACCENT : Theme.TEXT)));
            String desc = p.equals("auto") && hw != null ? "Según tu equipo: " + NAMES.get(profileFor(hw))[0] : NAMES.get(p)[1];
            txt.add(UiUtil.left(UiUtil.label(desc, Font.PLAIN, 11f, Theme.MUTED)));
            line.add(txt, BorderLayout.CENTER);
            OButton apply = new OButton("Aplicar", OButton.Kind.GHOST);
            apply.addActionListener(e -> {
                String prof = p.equals("auto") ? profileFor(hw != null ? hw : detect()) : p;
                toast.setForeground(applyVideo(prof) ? Theme.SUCCESS : Theme.DANGER);
                toast.setText(launcher.gameRunning() ? "Cierra Minecraft para que el perfil se note."
                        : "Perfil " + NAMES.get(prof)[0] + " aplicado.");
            });
            line.add(apply, BorderLayout.EAST);
            profiles.add(UiUtil.left(line));
        }
        profiles.revalidate();
        profiles.repaint();
    }

    /** Escribe los valores del perfil en options.txt (sin tocar el resto de opciones). */
    private static boolean applyVideo(String profile) {
        Map<String, String> vals = PROFILES.get(profile);
        if (vals == null) {
            return false;
        }
        try {
            Path file = GamePaths.root().resolve("options.txt");
            List<String> out = new ArrayList<>();
            if (Files.exists(file)) {
                for (String l : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    if (!l.isBlank() && !vals.containsKey(l.split(":", 2)[0])) {
                        out.add(l);
                    }
                }
            }
            vals.forEach((k, v) -> out.add(k + ":" + v));
            Files.createDirectories(file.getParent());
            Files.writeString(file, String.join("\n", out) + "\n", StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void renderRam() {
        ram.removeAll();
        int total = (int) Math.round(LauncherConfig.totalMemoryMb() / 1024.0);
        int rec = recommendedRam(total);
        for (int gb : new int[]{2, 3, 4, 6, 8}) {
            if (gb > Math.max(1, total - 1)) {
                continue;
            }
            boolean on = launcher.config.ramMb == gb * 1024;
            OButton b = new OButton(gb + " GB" + (gb == rec ? " · rec." : ""), on ? OButton.Kind.PRIMARY : OButton.Kind.GHOST);
            b.setToolTipText(gb == rec ? "Recomendado para tu equipo" : null);
            b.addActionListener(e -> {
                launcher.config.ramMb = gb * 1024;
                launcher.config.save();
                renderRam();
                toast.setForeground(Theme.SUCCESS);
                toast.setText("Memoria: " + gb + " GB para Minecraft.");
            });
            ram.add(b);
        }
        ram.revalidate();
        ram.repaint();
    }

    private static Map<String, String> map(String... kv) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            m.put(kv[i], kv[i + 1]);
        }
        return m;
    }

    /** Barras con el promedio de cada partida y una línea con el 1 % más bajo (crecen con una animación). */
    private static final class Chart extends JComponent {
        private List<Session> data = List.of();
        private long start;
        private Timer timer;

        void set(List<Session> d) {
            data = d;
            start = System.currentTimeMillis();
            setVisible(!d.isEmpty());
            if (timer != null) {
                timer.stop();
            }
            timer = new Timer(16, e -> {
                repaint();
                if (System.currentTimeMillis() - start > 650) {
                    ((Timer) e.getSource()).stop();
                }
            });
            timer.start();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            if (data.isEmpty()) {
                return;
            }
            Graphics2D g = (Graphics2D) g0.create();
            Theme.hq(g);
            int w = getWidth(), h = getHeight();
            double k = Math.min(1, (System.currentTimeMillis() - start) / 600.0), e = 1 - Math.pow(1 - k, 3);
            double max = 30;
            for (Session s : data) {
                max = Math.max(max, s.max() > 0 ? s.max() : s.avg());
            }
            max *= 1.1;
            g.setColor(new Color(255, 255, 255, 15));
            for (int i = 1; i <= 3; i++) {
                g.drawLine(0, h - h * i / 4, w, h - h * i / 4);
            }
            double bw = w / (double) data.size();
            for (int i = 0; i < data.size(); i++) {
                double bh = data.get(i).avg() / max * (h - 16) * e;
                g.setPaint(new GradientPaint(0, (float) (h - bh), Theme.ACCENT, 0, h, new Color(255, 255, 255, 10)));
                g.fillRoundRect((int) (i * bw + bw * 0.18), (int) (h - bh), (int) (bw * 0.64), (int) bh + 5, 10, 10);
            }
            Path2D.Double line = new Path2D.Double();
            for (int i = 0; i < data.size(); i++) {
                double x = i * bw + bw / 2, y = h - data.get(i).low() / max * (h - 16) * e;
                if (i == 0) {
                    line.moveTo(x, y);
                } else {
                    line.lineTo(x, y);
                }
            }
            g.setColor(Theme.WARNING);
            g.setStroke(new BasicStroke(2f));
            g.draw(line);
            g.dispose();
        }
    }
}
