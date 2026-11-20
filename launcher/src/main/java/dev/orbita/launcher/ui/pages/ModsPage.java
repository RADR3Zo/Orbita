package dev.orbita.launcher.ui.pages;

import dev.orbita.launcher.Launcher;
import dev.orbita.launcher.LauncherInfo;
import dev.orbita.launcher.core.GamePaths;
import dev.orbita.launcher.core.Progress;
import dev.orbita.launcher.core.mods.ModManager;
import dev.orbita.launcher.core.mods.ModManager.InstalledMod;
import dev.orbita.launcher.core.mods.ModrinthClient;
import dev.orbita.launcher.ui.Animator;
import dev.orbita.launcher.ui.Icons;
import dev.orbita.launcher.ui.Theme;
import dev.orbita.launcher.ui.components.Card;
import dev.orbita.launcher.ui.components.Dialogs;
import dev.orbita.launcher.ui.components.IconTile;
import dev.orbita.launcher.ui.components.OButton;
import dev.orbita.launcher.ui.components.OTextField;
import dev.orbita.launcher.ui.components.OToggle;
import dev.orbita.launcher.ui.components.UiUtil;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntConsumer;

/** Gestor de mods integrado: instalados, explorar Modrinth y pack de rendimiento. */
public final class ModsPage extends JPanel {
    private static final Map<String, String> DISPLAY = Map.of(
            "sodium", "Sodium", "lithium", "Lithium", "ferrite-core", "FerriteCore",
            "immediatelyfast", "ImmediatelyFast", "entityculling", "EntityCulling", "modernfix", "ModernFix");

    private final Launcher launcher;
    private final JFrame frame;
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final InstalledView installed = new InstalledView();
    private final ExploreView explore = new ExploreView();
    private final PerformanceView performance = new PerformanceView();
    private final Set<String> installedProjects = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final Progress logOnly = new Progress() {
        @Override
        public void status(String text) {
        }

        @Override
        public void progress(double fraction) {
        }

        @Override
        public void log(String line) {
            System.out.println(line);
        }
    };

    public ModsPage(Launcher launcher, JFrame frame) {
        this.launcher = launcher;
        this.frame = frame;
        setOpaque(false);
        setLayout(new BorderLayout(0, 18));
        setBorder(BorderFactory.createEmptyBorder(4, 40, 30, 40));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.add(UiUtil.left(UiUtil.title("Mods")));
        header.add(UiUtil.vgap(4));
        header.add(UiUtil.left(UiUtil.muted("Busca, instala y gestiona mods de Fabric para Minecraft "
                + LauncherInfo.MINECRAFT + " con un clic. Sin descargas manuales.")));
        header.add(UiUtil.vgap(16));
        Segmented tabs = new Segmented(new String[]{"Instalados", "Explorar", "Rendimiento"}, i -> {
            cards.show(content, Integer.toString(i));
            if (i == 1) {
                explore.ensureLoaded();
            } else if (i == 2) {
                performance.refresh();
            }
        });
        header.add(UiUtil.left(tabs));
        add(header, BorderLayout.NORTH);

        content.setOpaque(false);
        content.add(installed, "0");
        content.add(explore, "1");
        content.add(performance, "2");
        add(content, BorderLayout.CENTER);

        launcher.onModsChanged(installed::refresh);
        installed.refresh();
    }

    private void fail(String title, Exception e) {
        UiUtil.ui(() -> Dialogs.info(frame, title, e.getMessage() == null ? e.toString() : e.getMessage()));
    }

    private void openFolder(java.nio.file.Path dir) {
        try {
            Files.createDirectories(dir);
            Desktop.getDesktop().open(dir.toFile());
        } catch (Exception e) {
            fail("No se pudo abrir la carpeta", e);
        }
    }

    // ---------------------------------------------------------------- Instalados

    private final class InstalledView extends JPanel {
        private final JPanel list = column();
        private final JLabel summary = UiUtil.muted("Cargando mods…");
        private final OButton updateAll = new OButton("Actualizar todo", OButton.Kind.SECONDARY, Icons.Type.DOWNLOAD);
        private List<InstalledMod> mods = List.of();

        InstalledView() {
            setOpaque(false);
            setLayout(new BorderLayout(0, 12));
            JPanel toolbar = new JPanel(new BorderLayout());
            toolbar.setOpaque(false);
            toolbar.add(summary, BorderLayout.WEST);
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            actions.setOpaque(false);
            updateAll.setVisible(false);
            updateAll.addActionListener(e -> updateAll());
            OButton reload = new OButton("", OButton.Kind.GHOST, Icons.Type.REFRESH);
            reload.setToolTipText("Recargar");
            reload.addActionListener(e -> refresh());
            OButton folder = new OButton("", OButton.Kind.GHOST, Icons.Type.FOLDER);
            folder.setToolTipText("Abrir carpeta de mods");
            folder.addActionListener(e -> openFolder(GamePaths.mods()));
            actions.add(updateAll);
            actions.add(reload);
            actions.add(folder);
            toolbar.add(actions, BorderLayout.EAST);
            add(toolbar, BorderLayout.NORTH);
            add(UiUtil.scroll(wrapTop(list)), BorderLayout.CENTER);
        }

        void refresh() {
            summary.setText("Cargando mods…");
            launcher.worker.submit(() -> {
                List<InstalledMod> result;
                boolean offline = false;
                try {
                    result = launcher.mods.scan(true);
                } catch (Exception e) {
                    offline = true;
                    try {
                        result = launcher.mods.scan(false);
                    } catch (Exception e2) {
                        result = List.of();
                    }
                }
                List<InstalledMod> finalResult = result;
                boolean finalOffline = offline;
                UiUtil.ui(() -> show(finalResult, finalOffline));
            });
        }

        private void show(List<InstalledMod> result, boolean offline) {
            mods = result;
            installedProjects.clear();
            for (InstalledMod m : result) {
                if (m.projectId != null) {
                    installedProjects.add(m.projectId);
                }
            }
            long updates = result.stream().filter(m -> m.update != null).count();
            long enabled = result.stream().filter(m -> m.enabled).count();
            summary.setText(result.isEmpty()
                    ? "Aún no hay mods. Pulsa JUGAR una vez o instala desde Explorar."
                    : result.size() + " mods · " + enabled + " activos"
                    + (updates > 0 ? " · " + updates + " actualizaciones" : "")
                    + (offline ? " · sin conexión con Modrinth" : ""));
            updateAll.setVisible(updates > 0);

            list.removeAll();
            for (InstalledMod m : result) {
                list.add(UiUtil.left(installedRow(m)));
                list.add(UiUtil.vgap(8));
            }
            list.revalidate();
            list.repaint();
            revalidate();
        }

        private JComponent installedRow(InstalledMod m) {
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            actions.setOpaque(false);
            if (m.update != null) {
                OButton up = new OButton("Actualizar", OButton.Kind.SECONDARY, Icons.Type.DOWNLOAD);
                up.setToolTipText("Nueva versión: " + m.update.versionNumber());
                up.addActionListener(e -> {
                    up.setEnabled(false);
                    up.setText("Actualizando…");
                    launcher.worker.submit(() -> {
                        try {
                            launcher.mods.update(m, logOnly);
                        } catch (Exception ex) {
                            fail("No se pudo actualizar " + m.title, ex);
                        }
                        launcher.fireModsChanged();
                    });
                });
                actions.add(up);
            }
            if (m.core) {
                actions.add(badge("NÚCLEO"));
            } else {
                OToggle toggle = new OToggle(m.enabled);
                toggle.setToolTipText(m.enabled ? "Desactivar" : "Activar");
                toggle.onChange(v -> launcher.worker.submit(() -> {
                    try {
                        launcher.mods.setEnabled(m, v);
                    } catch (Exception ex) {
                        fail("No se pudo cambiar el mod", ex);
                    }
                    launcher.fireModsChanged();
                }));
                OButton delete = new OButton("", OButton.Kind.GHOST, Icons.Type.TRASH);
                delete.setToolTipText("Eliminar");
                delete.addActionListener(e -> {
                    if (Dialogs.confirm(frame, "Eliminar " + m.title,
                            "Se borrará " + m.fileName() + " de la carpeta mods. Puedes volver a instalarlo desde Explorar.",
                            "Eliminar", "Cancelar", true)) {
                        launcher.worker.submit(() -> {
                            try {
                                launcher.mods.delete(m);
                            } catch (Exception ex) {
                                fail("No se pudo eliminar", ex);
                            }
                            launcher.fireModsChanged();
                        });
                    }
                });
                actions.add(toggle);
                actions.add(delete);
            }
            String sub = (m.versionNumber != null ? m.versionNumber + " · " : "") + m.fileName();
            IconTile icon = m.core ? new IconTile(m.title, Icons.appIcons().get(5), 44) : new IconTile(m.title, m.iconUrl, 44);
            return row(icon, m.title, sub, m.description, actions, !m.enabled);
        }

        private void updateAll() {
            updateAll.setEnabled(false);
            launcher.worker.submit(() -> {
                for (InstalledMod m : mods) {
                    if (m.update != null) {
                        try {
                            launcher.mods.update(m, logOnly);
                        } catch (Exception ex) {
                            fail("No se pudo actualizar " + m.title, ex);
                        }
                    }
                }
                UiUtil.ui(() -> updateAll.setEnabled(true));
                launcher.fireModsChanged();
            });
        }
    }

    // ---------------------------------------------------------------- Explorar

    private final class ExploreView extends JPanel {
        private final OTextField query = new OTextField("", "Buscar mods en Modrinth (p. ej. minimap, shaders, zoom)…");
        private final JPanel results = column();
        private final JLabel info = UiUtil.muted("");
        private boolean loaded;

        ExploreView() {
            setOpaque(false);
            setLayout(new BorderLayout(0, 12));
            JPanel bar = new JPanel(new BorderLayout(10, 0));
            bar.setOpaque(false);
            bar.add(query, BorderLayout.CENTER);
            OButton search = new OButton("Buscar", OButton.Kind.SECONDARY, Icons.Type.SEARCH);
            search.addActionListener(e -> search());
            query.addActionListener(e -> search());
            bar.add(search, BorderLayout.EAST);
            JPanel top = column();
            top.add(UiUtil.left(bar));
            top.add(UiUtil.vgap(10));
            top.add(UiUtil.left(info));
            add(top, BorderLayout.NORTH);
            add(UiUtil.scroll(wrapTop(results)), BorderLayout.CENTER);
        }

        void ensureLoaded() {
            if (!loaded) {
                loaded = true;
                search();
            }
        }

        private void search() {
            String q = query.getText().trim();
            info.setText(q.isEmpty() ? "Mods más populares para " + LauncherInfo.MINECRAFT + "…" : "Buscando \"" + q + "\"…");
            launcher.worker.submit(() -> {
                try {
                    List<ModrinthClient.SearchHit> hits = launcher.mods.api().search(q, 30);
                    UiUtil.ui(() -> show(q, hits));
                } catch (Exception e) {
                    UiUtil.ui(() -> info.setText("No se pudo conectar con Modrinth: " + e.getMessage()));
                }
            });
        }

        private void show(String q, List<ModrinthClient.SearchHit> hits) {
            info.setText(hits.isEmpty() ? "Sin resultados." : (q.isEmpty() ? "Populares" : "Resultados") + " · " + hits.size() + " mods compatibles con Fabric " + LauncherInfo.MINECRAFT);
            results.removeAll();
            for (ModrinthClient.SearchHit hit : hits) {
                boolean have = installedProjects.contains(hit.projectId());
                OButton install = new OButton(have ? "Instalado" : "Instalar",
                        have ? OButton.Kind.GHOST : OButton.Kind.SECONDARY, have ? Icons.Type.CHECK : Icons.Type.DOWNLOAD);
                install.setEnabled(!have);
                install.addActionListener(e -> install(hit, install));
                JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
                actions.setOpaque(false);
                actions.add(install);
                String sub = "por " + hit.author() + " · " + compact(hit.downloads()) + " descargas";
                results.add(UiUtil.left(row(new IconTile(hit.title(), hit.iconUrl(), 44), hit.title(), sub, hit.description(), actions, false)));
                results.add(UiUtil.vgap(8));
            }
            results.revalidate();
            results.repaint();
        }

        private void install(ModrinthClient.SearchHit hit, OButton button) {
            button.setEnabled(false);
            button.setText("Instalando…");
            launcher.worker.submit(() -> {
                try {
                    List<String> files = launcher.mods.install(hit.projectId(), installedProjects, logOnly);
                    UiUtil.ui(() -> {
                        button.setText("Instalado");
                        button.icon(Icons.Type.CHECK);
                        if (launcher.gameRunning()) {
                            Dialogs.info(frame, hit.title() + " instalado", "Reinicia Minecraft para cargar " + String.join(", ", files) + ".");
                        }
                    });
                    launcher.fireModsChanged();
                } catch (Exception e) {
                    UiUtil.ui(() -> {
                        button.setEnabled(true);
                        button.setText("Instalar");
                    });
                    fail("No se pudo instalar " + hit.title(), e);
                }
            });
        }
    }

    // ---------------------------------------------------------------- Rendimiento

    private final class PerformanceView extends JPanel {
        private final JPanel list = column();
        private final OButton installPack = new OButton("Instalar pack de rendimiento", OButton.Kind.PRIMARY, Icons.Type.BOLT);
        private final OButton installIris = new OButton("Instalar Iris Shaders", OButton.Kind.SECONDARY, Icons.Type.DOWNLOAD);
        private Map<String, String> slugToId = Map.of();
        private Map<String, String> slugToIcon = Map.of();

        PerformanceView() {
            setOpaque(false);
            setLayout(new BorderLayout());
            JPanel col = column();

            Card hero = new Card(new BorderLayout(18, 0));
            JPanel heroText = column();
            heroText.add(UiUtil.left(UiUtil.label("Pack de rendimiento", Font.BOLD, 20f, Theme.TEXT)));
            heroText.add(UiUtil.vgap(6));
            heroText.add(UiUtil.left(UiUtil.muted("Más FPS, menos tirones y menos RAM. Se instala solo lo compatible con " + LauncherInfo.MINECRAFT + ".")));
            hero.add(heroText, BorderLayout.CENTER);
            installPack.setPreferredSize(new Dimension(290, 54));
            installPack.addActionListener(e -> installAll(new ArrayList<>(ModManager.PERFORMANCE_PACK.keySet()), installPack));
            hero.add(installPack, BorderLayout.EAST);
            hero.setMaximumSize(new Dimension(Integer.MAX_VALUE, 96));
            col.add(UiUtil.left(hero));
            col.add(UiUtil.vgap(14));
            col.add(UiUtil.left(list));
            col.add(UiUtil.vgap(14));

            Card shaders = new Card(new BorderLayout(18, 0));
            JPanel shaderText = column();
            shaderText.add(UiUtil.left(UiUtil.label("Shaders", Font.BOLD, 16f, Theme.TEXT)));
            shaderText.add(UiUtil.vgap(4));
            shaderText.add(UiUtil.left(UiUtil.muted("Iris + Sodium. Coloca los packs (.zip) en la carpeta shaderpacks y elígelos en el juego.")));
            shaders.add(shaderText, BorderLayout.CENTER);
            JPanel shaderActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            shaderActions.setOpaque(false);
            OButton folder = new OButton("Carpeta de shaders", OButton.Kind.GHOST, Icons.Type.FOLDER);
            folder.addActionListener(e -> openFolder(GamePaths.shaderpacks()));
            installIris.addActionListener(e -> installAll(List.of(ModManager.IRIS), installIris));
            shaderActions.add(folder);
            shaderActions.add(installIris);
            shaders.add(shaderActions, BorderLayout.EAST);
            shaders.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
            col.add(UiUtil.left(shaders));

            add(UiUtil.scroll(wrapTop(col)), BorderLayout.CENTER);
            rebuild();
        }

        void refresh() {
            launcher.worker.submit(() -> {
                try {
                    List<String> slugs = new ArrayList<>(ModManager.PERFORMANCE_PACK.keySet());
                    slugs.add(ModManager.IRIS);
                    Map<String, String> map = new java.util.HashMap<>();
                    Map<String, String> icons = new java.util.HashMap<>();
                    launcher.mods.api().projects(slugs).values().forEach(p -> {
                        map.put(p.slug(), p.id());
                        if (p.iconUrl() != null) {
                            icons.put(p.slug(), p.iconUrl());
                        }
                    });
                    UiUtil.ui(() -> {
                        slugToId = map;
                        slugToIcon = icons;
                        rebuild();
                    });
                } catch (Exception ignored) {
                    // sin conexión: se muestra la lista sin estado
                }
            });
        }

        private boolean has(String slug) {
            String id = slugToId.get(slug);
            return id != null && installedProjects.contains(id);
        }

        private void rebuild() {
            list.removeAll();
            for (Map.Entry<String, String> e : ModManager.PERFORMANCE_PACK.entrySet()) {
                JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
                actions.setOpaque(false);
                boolean have = has(e.getKey());
                actions.add(badge(have ? "INSTALADO" : "PENDIENTE"));
                String name = DISPLAY.getOrDefault(e.getKey(), e.getKey());
                String iconUrl = slugToIcon.get(e.getKey());
                list.add(UiUtil.left(row(new IconTile(name, iconUrl, 40), name, e.getValue(), null, actions, false)));
                list.add(UiUtil.vgap(8));
            }
            boolean all = ModManager.PERFORMANCE_PACK.keySet().stream().allMatch(this::has);
            installPack.setText(all ? "Pack instalado" : "Instalar pack de rendimiento");
            installPack.setEnabled(!all);
            boolean iris = has(ModManager.IRIS);
            installIris.setText(iris ? "Iris instalado" : "Instalar Iris Shaders");
            installIris.setEnabled(!iris);
            list.revalidate();
            list.repaint();
        }

        private void installAll(List<String> slugs, OButton button) {
            button.setEnabled(false);
            String old = button.getText();
            button.setText("Instalando…");
            launcher.worker.submit(() -> {
                List<String> skipped = new ArrayList<>();
                for (String slug : slugs) {
                    try {
                        launcher.mods.install(slug, installedProjects, logOnly);
                    } catch (Exception e) {
                        skipped.add(DISPLAY.getOrDefault(slug, slug) + ": " + e.getMessage());
                    }
                }
                launcher.fireModsChanged();
                UiUtil.ui(() -> {
                    button.setText(old);
                    refresh();
                    if (!skipped.isEmpty()) {
                        Dialogs.info(frame, "Algunos mods no se instalaron", String.join("\n", skipped));
                    }
                });
            });
        }
    }

    // ---------------------------------------------------------------- utilidades

    private static JPanel column() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        return p;
    }

    /** Envuelve una columna para que el scroll no estire sus filas verticalmente. */
    private static JPanel wrapTop(JComponent c) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.add(c, BorderLayout.NORTH);
        return p;
    }

    private static JComponent row(JComponent icon, String title, String sub, String description, JComponent actions, boolean dimmed) {
        Card card = new Card(new BorderLayout(14, 0), 14);
        card.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        JPanel iconWrap = new JPanel(new BorderLayout());
        iconWrap.setOpaque(false);
        iconWrap.add(icon, BorderLayout.NORTH);
        card.add(iconWrap, BorderLayout.WEST);
        JPanel text = column();
        text.add(UiUtil.left(UiUtil.label(title, Font.BOLD, 15f, dimmed ? Theme.MUTED : Theme.TEXT)));
        text.add(UiUtil.vgap(2));
        text.add(UiUtil.left(UiUtil.label(sub, Font.PLAIN, 12f, Theme.DISABLED)));
        if (description != null && !description.isBlank()) {
            text.add(UiUtil.vgap(4));
            JLabel desc = UiUtil.label("<html><div style='width:420px'>" + escape(description) + "</div></html>", Font.PLAIN, 13f, Theme.MUTED);
            text.add(UiUtil.left(desc));
        }
        card.add(text, BorderLayout.CENTER);
        JPanel actionWrap = new JPanel(new BorderLayout());
        actionWrap.setOpaque(false);
        actionWrap.add(actions, BorderLayout.CENTER);
        card.add(actionWrap, BorderLayout.EAST);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height));
        return card;
    }

    private static JComponent badge(String text) {
        JLabel l = new JLabel(text) {
            @Override
            public java.awt.Color getForeground() {
                return Theme.ACCENT;
            }

            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                Theme.hq(g);
                g.setColor(Theme.alpha(Theme.ACCENT, 0.18f));
                g.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), getHeight(), getHeight()));
                g.dispose();
                super.paintComponent(g0);
            }
        };
        l.setFont(Theme.tracked(Font.BOLD, 11f, 0.12f));
        l.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
        return l;
    }

    private static String compact(long n) {
        if (n >= 1_000_000) {
            return String.format("%.1f M", n / 1_000_000.0);
        }
        if (n >= 1_000) {
            return String.format("%.0f k", n / 1_000.0);
        }
        return Long.toString(n);
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** Selector segmentado con indicador deslizante. */
    private static final class Segmented extends JComponent {
        private final String[] labels;
        private final Animator pos = new Animator(0f, 14f, this::repaint);
        private int selected;

        Segmented(String[] labels, IntConsumer onSelect) {
            this.labels = labels;
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    int i = Math.min(labels.length - 1, Math.max(0, e.getX() * labels.length / Math.max(1, getWidth())));
                    if (i != selected) {
                        selected = i;
                        pos.setTarget(i);
                        repaint();
                        onSelect.accept(i);
                    }
                }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(labels.length * 130, 40);
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.hq(g);
            int w = getWidth();
            int h = getHeight();
            float seg = w / (float) labels.length;
            g.setColor(Theme.SURFACE);
            g.fill(new RoundRectangle2D.Float(0, 0, w, h, h, h));
            g.setColor(Theme.alpha(Theme.ACCENT, 0.28f));
            g.fill(new RoundRectangle2D.Float(pos.get() * seg + 4, 4, seg - 8, h - 8, h - 8, h - 8));
            g.setFont(Theme.font(Font.BOLD, 13f));
            FontMetrics fm = g.getFontMetrics();
            for (int i = 0; i < labels.length; i++) {
                g.setColor(i == selected ? Theme.TEXT : Theme.MUTED);
                g.drawString(labels[i], i * seg + (seg - fm.stringWidth(labels[i])) / 2, (h - fm.getHeight()) / 2f + fm.getAscent());
            }
            g.dispose();
        }
    }
}
