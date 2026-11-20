package dev.orbita.launcher.ui.pages;

import dev.orbita.launcher.Launcher;
import dev.orbita.launcher.LauncherInfo;
import dev.orbita.launcher.core.GamePaths;
import dev.orbita.launcher.core.LauncherConfig;
import dev.orbita.launcher.ui.Icons;
import dev.orbita.launcher.ui.Theme;
import dev.orbita.launcher.ui.components.Card;
import dev.orbita.launcher.ui.components.Dialogs;
import dev.orbita.launcher.ui.components.OButton;
import dev.orbita.launcher.ui.components.OSlider;
import dev.orbita.launcher.ui.components.OTextField;
import dev.orbita.launcher.ui.components.OToggle;
import dev.orbita.launcher.ui.components.UiUtil;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

/** Ajustes: memoria, Java, comportamiento del launcher, cuenta Microsoft y archivos. */
public final class SettingsPage extends JPanel {
    private final Launcher launcher;
    private final JFrame frame;

    public SettingsPage(Launcher launcher, JFrame frame) {
        this.launcher = launcher;
        this.frame = frame;
        LauncherConfig cfg = launcher.config;
        setOpaque(false);
        setLayout(new BorderLayout(0, 18));
        setBorder(BorderFactory.createEmptyBorder(4, 40, 30, 40));

        JPanel header = column();
        header.add(UiUtil.left(UiUtil.title("Ajustes")));
        header.add(UiUtil.vgap(4));
        header.add(UiUtil.left(UiUtil.muted("Los cambios se guardan automáticamente.")));
        add(header, BorderLayout.NORTH);

        JPanel col = column();

        col.add(appearanceSection(cfg));

        // Memoria
        int total = LauncherConfig.totalMemoryMb();
        int max = Math.max(2048, (total - 1536) / 512 * 512);
        JLabel ramLabel = UiUtil.label(gb(cfg.ramMb), Font.BOLD, 14f, Theme.TEXT);
        OSlider ram = new OSlider(1024, max, 512, cfg.ramMb);
        ram.setPreferredSize(new Dimension(240, 26));
        ram.onChange(v -> {
            cfg.ramMb = v;
            ramLabel.setText(gb(v));
            cfg.save();
        });
        JPanel ramBox = new JPanel(new BorderLayout(12, 0));
        ramBox.setOpaque(false);
        ramBox.add(ram, BorderLayout.CENTER);
        ramBox.add(ramLabel, BorderLayout.EAST);
        col.add(section("Rendimiento",
                setting("Memoria RAM", "Tu equipo tiene " + gb(total) + ". Con 4 GB va bien incluso con shaders.", ramBox),
                setting("Argumentos JVM extra", "Solo si sabes lo que haces. Órbita ya aplica ZGC y memoria fija.",
                        field(cfg.jvmArgs, "-Dalgo=valor …", 320, v -> cfg.jvmArgs = v))));

        // Launcher
        OToggle hide = new OToggle(cfg.hideWhilePlaying);
        hide.onChange(v -> {
            cfg.hideWhilePlaying = v;
            cfg.save();
        });
        OToggle pack = new OToggle(cfg.installPerformancePack);
        pack.onChange(v -> {
            cfg.installPerformancePack = v;
            cfg.save();
        });
        OToggle zgc = new OToggle(cfg.useZgc);
        zgc.onChange(v -> {
            cfg.useZgc = v;
            cfg.save();
        });
        col.add(section("Launcher",
                setting("Ocultar el launcher mientras juegas", "Vuelve a aparecer al cerrar Minecraft. Oculto no gasta CPU.", hide),
                setting("Recolector ZGC (menos tirones)", "Pausas de memoria de menos de 1 ms. Desactívalo para usar G1 si notas algo raro.", zgc),
                setting("Pack de rendimiento automático", "Instala Sodium, Lithium y compañía en la próxima partida.", pack)));

        // Microsoft
        col.add(section("Cuenta Microsoft",
                setting("Client ID de Azure (opcional)", "Déjalo vacío: el inicio de sesión premium ya funciona con el login oficial de Microsoft. Solo pon uno si tienes tu propia app de Azure aprobada por Mojang.",
                        field(cfg.microsoftClientId, "00000000-0000-0000-0000-000000000000", 320, v -> {
                            cfg.microsoftClientId = v;
                            launcher.fireAccountChanged();
                        }))));

        // Archivos
        JPanel fileButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        fileButtons.setOpaque(false);
        fileButtons.add(button("Carpeta del juego", Icons.Type.FOLDER, () -> open(GamePaths.root())));
        fileButtons.add(button("Mods", Icons.Type.FOLDER, () -> open(GamePaths.mods())));
        fileButtons.add(button("Registros", Icons.Type.FOLDER, () -> open(GamePaths.logs())));
        OButton verify = new OButton("Verificar archivos", OButton.Kind.SECONDARY, Icons.Type.CHECK);
        verify.addActionListener(e -> {
            launcher.requestVerify();
            Dialogs.info(frame, "Verificación programada",
                    "En la próxima partida Órbita comprobará el hash de todos los archivos de Minecraft y volverá a descargar los dañados.");
        });
        fileButtons.add(verify);
        col.add(section("Archivos", UiUtil.left(fileButtons)));

        col.add(UiUtil.vgap(6));
        col.add(UiUtil.left(UiUtil.label("Órbita Launcher " + LauncherInfo.VERSION + " · Minecraft " + LauncherInfo.MINECRAFT
                + " · Fabric Loader " + LauncherInfo.FABRIC_LOADER + " · Java " + Runtime.version().feature(), Font.PLAIN, 12f, Theme.DISABLED)));
        col.add(UiUtil.left(UiUtil.label("Órbita no está afiliado a Mojang ni a Microsoft.", Font.PLAIN, 12f, Theme.DISABLED)));

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(col, BorderLayout.NORTH);
        add(UiUtil.scroll(wrap), BorderLayout.CENTER);
    }

    // ------------------------------------------------------------------ apariencia

    private dev.orbita.launcher.ui.Appearance appearance() {
        return frame instanceof dev.orbita.launcher.ui.LauncherFrame lf ? lf.appearance() : null;
    }

    private void applyAppearance() {
        launcher.config.save();
        var a = appearance();
        if (a != null) {
            a.apply();
        }
        frame.repaint();
    }

    private JComponent appearanceSection(LauncherConfig cfg) {
        // Color del tema
        ColorSwatches swatches = new ColorSwatches(cfg.themePreset, () -> {
            java.awt.Color chosen = javax.swing.JColorChooser.showDialog(frame, "Color del tema", new java.awt.Color(cfg.customAccent));
            return chosen;
        }, (preset, custom) -> {
            cfg.themePreset = preset;
            if (custom != null) {
                cfg.customAccent = custom.getRGB() & 0xFFFFFF;
            }
            applyAppearance();
        });

        // Tipo de fondo
        JLabel fileLabel = UiUtil.label(fileName(cfg.backgroundFile), Font.PLAIN, 12f, Theme.MUTED);
        OButton pick = new OButton("Elegir archivo…", OButton.Kind.SECONDARY, Icons.Type.FOLDER);
        String[] modes = {"UNIVERSE", "COLOR", "IMAGE", "VIDEO"};
        String[] labels = {"Universo", "Color", "Imagen", "Video"};
        int current = java.util.Arrays.asList(modes).indexOf(cfg.backgroundMode);
        Choice bgChoice = new Choice(labels, Math.max(0, current), i -> {
            cfg.backgroundMode = modes[i];
            boolean needsFile = i >= 2;
            pick.setEnabled(needsFile);
            if (needsFile && (cfg.backgroundFile.isBlank() || !matches(cfg.backgroundFile, i == 2
                    ? dev.orbita.launcher.ui.Appearance.IMAGE_EXT : dev.orbita.launcher.ui.Appearance.VIDEO_EXT))) {
                chooseBackground(cfg, i == 3, fileLabel);
            }
            applyAppearance();
        });
        pick.setEnabled(current >= 2);
        pick.addActionListener(e -> {
            chooseBackground(cfg, "VIDEO".equals(cfg.backgroundMode), fileLabel);
            applyAppearance();
        });
        JPanel fileRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        fileRow.setOpaque(false);
        fileRow.add(fileLabel);
        fileRow.add(pick);

        OSlider dim = new OSlider(0, 80, 5, cfg.backgroundDim);
        dim.setPreferredSize(new Dimension(200, 26));
        dim.onChange(v -> {
            cfg.backgroundDim = v;
            cfg.save();
            var a = appearance();
            if (a != null) {
                a.updateDim();
            }
        });

        OToggle videoSound = new OToggle(cfg.videoSound);
        OSlider videoVol = new OSlider(0, 100, 5, cfg.videoVolume);
        videoVol.setPreferredSize(new Dimension(140, 26));
        videoSound.onChange(v -> {
            cfg.videoSound = v;
            cfg.save();
            var a = appearance();
            if (a != null) {
                a.updateVolumes();
            }
        });
        videoVol.onChange(v -> {
            cfg.videoVolume = v;
            cfg.save();
            var a = appearance();
            if (a != null) {
                a.updateVolumes();
            }
        });
        JPanel videoRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        videoRow.setOpaque(false);
        videoRow.add(videoVol);
        videoRow.add(videoSound);

        // Música
        JLabel musicLabel = UiUtil.label(fileName(cfg.musicFile), Font.PLAIN, 12f, Theme.MUTED);
        OToggle music = new OToggle(cfg.musicEnabled);
        OButton pickMusic = new OButton("Elegir canción…", OButton.Kind.SECONDARY, Icons.Type.FOLDER);
        pickMusic.addActionListener(e -> {
            java.io.File f = choose("Música de fondo", dev.orbita.launcher.ui.Appearance.AUDIO_EXT);
            if (f != null) {
                try {
                    cfg.musicFile = dev.orbita.launcher.ui.Appearance.importFile(f, "music").getAbsolutePath();
                    musicLabel.setText(fileName(f.getAbsolutePath()));
                    cfg.musicEnabled = true;
                    music.setSelected(true);
                    applyAppearance();
                } catch (Exception ex) {
                    Dialogs.info(frame, "No se pudo copiar la canción", String.valueOf(ex.getMessage()));
                }
            }
        });
        music.onChange(v -> {
            cfg.musicEnabled = v;
            if (v && cfg.musicFile.isBlank()) {
                pickMusic.doClick();
                return;
            }
            applyAppearance();
        });
        OSlider musicVol = new OSlider(0, 100, 5, cfg.musicVolume);
        musicVol.setPreferredSize(new Dimension(140, 26));
        musicVol.onChange(v -> {
            cfg.musicVolume = v;
            cfg.save();
            var a = appearance();
            if (a != null) {
                a.updateVolumes();
            }
        });
        JPanel musicRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        musicRow.setOpaque(false);
        musicRow.add(musicLabel);
        musicRow.add(pickMusic);
        musicRow.add(musicVol);
        musicRow.add(music);

        // Animación de inicio
        OToggle intro = new OToggle(cfg.introAnimation);
        intro.onChange(v -> {
            cfg.introAnimation = v;
            cfg.save();
        });
        OButton preview = new OButton("Ver", OButton.Kind.GHOST, Icons.Type.PLAY);
        preview.addActionListener(e -> dev.orbita.launcher.ui.IntroOverlay.play(frame));
        JPanel introRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        introRow.setOpaque(false);
        introRow.add(preview);
        introRow.add(intro);

        return section("Apariencia",
                setting("Color del tema", "Botones, brillos y nebulosas usan este color.", swatches),
                setting("Fondo", "Universo animado, un color, una imagen (también GIF) o un video MP4.", bgChoice),
                setting("Archivo de fondo", "Se copia a tu carpeta de Órbita para que no se pierda.", fileRow),
                setting("Oscurecer fondo", "Para que el texto se lea bien sobre imágenes y videos.", dim),
                setting("Sonido del video", "Reproduce el audio del video de fondo.", videoRow),
                setting("Música de fondo", "MP3, WAV o M4A en bucle. Se pausa mientras juegas.", musicRow),
                setting("Animación de inicio", "Viaje por el universo al abrir el launcher.", introRow));
    }

    private void chooseBackground(LauncherConfig cfg, boolean video, JLabel label) {
        String[] ext = video ? dev.orbita.launcher.ui.Appearance.VIDEO_EXT : dev.orbita.launcher.ui.Appearance.IMAGE_EXT;
        java.io.File f = choose(video ? "Video de fondo" : "Imagen de fondo", ext);
        if (f == null) {
            return;
        }
        try {
            cfg.backgroundFile = dev.orbita.launcher.ui.Appearance.importFile(f, "background").getAbsolutePath();
            label.setText(fileName(f.getAbsolutePath()));
        } catch (Exception ex) {
            Dialogs.info(frame, "No se pudo copiar el archivo", String.valueOf(ex.getMessage()));
        }
    }

    /**
     * Selector de archivos nativo de Windows (el mismo del Explorador), abierto directamente en la carpeta
     * que toca: Videos, Música o Imágenes (también si están en OneDrive). Solo muestra formatos compatibles.
     */
    private java.io.File choose(String title, String[] ext) {
        java.awt.FileDialog fd = new java.awt.FileDialog(frame, title, java.awt.FileDialog.LOAD);
        java.io.File start = startFolder(ext);
        if (start != null) {
            fd.setDirectory(start.getAbsolutePath());
        }
        StringBuilder pattern = new StringBuilder();
        for (String e : ext) {
            if (pattern.length() > 0) {
                pattern.append(';');
            }
            pattern.append("*.").append(e);
        }
        fd.setFile(pattern.toString());
        fd.setFilenameFilter((dir, name) -> dev.orbita.launcher.ui.Appearance.hasExtension(new java.io.File(name), ext));
        fd.setVisible(true);
        if (fd.getFile() == null) {
            return null;
        }
        java.io.File f = new java.io.File(fd.getDirectory(), fd.getFile());
        if (!dev.orbita.launcher.ui.Appearance.hasExtension(f, ext)) {
            Dialogs.info(frame, "Formato no compatible",
                    "Elige un archivo " + String.join(", ", ext).toUpperCase() + ".");
            return null;
        }
        return f;
    }

    /** Carpeta de Videos, Música o Imágenes del usuario según el tipo de archivo. */
    private static java.io.File startFolder(String[] ext) {
        String home = System.getProperty("user.home");
        String[] names = ext == dev.orbita.launcher.ui.Appearance.VIDEO_EXT ? new String[]{"Videos", "Vídeos"}
                : ext == dev.orbita.launcher.ui.Appearance.AUDIO_EXT ? new String[]{"Music", "Música"}
                : new String[]{"Pictures", "Imágenes"};
        for (String base : new String[]{home + "\\OneDrive", home}) {
            for (String n : names) {
                java.io.File d = new java.io.File(base, n);
                if (d.isDirectory()) {
                    return d;
                }
            }
        }
        return new java.io.File(home);
    }

    private static boolean matches(String path, String[] ext) {
        return dev.orbita.launcher.ui.Appearance.hasExtension(new java.io.File(path), ext);
    }

    private static String fileName(String path) {
        if (path == null || path.isBlank()) {
            return "Ningún archivo";
        }
        String n = new java.io.File(path).getName();
        return n.length() > 28 ? n.substring(0, 25) + "…" : n;
    }

    /** Fila de círculos de color (temas predefinidos) + uno arcoíris para elegir un color propio. */
    private static final class ColorSwatches extends JComponent {
        private static final int D = 22;
        private static final int GAP = 8;
        private int selected;

        ColorSwatches(int selected, java.util.function.Supplier<java.awt.Color> customPicker,
                      java.util.function.BiConsumer<Integer, java.awt.Color> onChange) {
            this.selected = selected;
            setOpaque(false);
            setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mousePressed(java.awt.event.MouseEvent e) {
                    int i = e.getX() / (D + GAP);
                    if (i < Theme.PRESETS.length) {
                        ColorSwatches.this.selected = i;
                        onChange.accept(i, null);
                    } else if (i == Theme.PRESETS.length) {
                        java.awt.Color c = customPicker.get();
                        if (c != null) {
                            ColorSwatches.this.selected = -1;
                            onChange.accept(-1, c);
                        }
                    }
                    repaint();
                }
            });
            StringBuilder tip = new StringBuilder("<html>");
            for (Theme.Preset p : Theme.PRESETS) {
                tip.append(p.name()).append(" · ");
            }
            setToolTipText(tip.append("Personalizado</html>").toString());
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension((Theme.PRESETS.length + 1) * (D + GAP), D + 6);
        }

        @Override
        protected void paintComponent(java.awt.Graphics g0) {
            java.awt.Graphics2D g = (java.awt.Graphics2D) g0.create();
            Theme.hq(g);
            int y = 3;
            for (int i = 0; i <= Theme.PRESETS.length; i++) {
                int x = i * (D + GAP) + 3;
                boolean sel = i == selected || (i == Theme.PRESETS.length && selected == -1);
                if (i < Theme.PRESETS.length) {
                    Theme.Preset p = Theme.PRESETS[i];
                    g.setPaint(new java.awt.GradientPaint(x, y, new java.awt.Color(p.accent()), x + D, y + D, new java.awt.Color(p.accent2())));
                } else {
                    g.setPaint(new java.awt.LinearGradientPaint(x, y, x + D, y + D, new float[]{0f, 0.33f, 0.66f, 1f},
                            new java.awt.Color[]{java.awt.Color.RED, java.awt.Color.YELLOW, java.awt.Color.CYAN, java.awt.Color.MAGENTA}));
                }
                g.fill(new java.awt.geom.Ellipse2D.Float(x, y, D, D));
                if (sel) {
                    g.setColor(java.awt.Color.WHITE);
                    g.setStroke(new java.awt.BasicStroke(2f));
                    g.draw(new java.awt.geom.Ellipse2D.Float(x - 3, y - 3, D + 6, D + 6));
                }
            }
            g.dispose();
        }
    }

    /** Selector segmentado con indicador deslizante. */
    private static final class Choice extends JComponent {
        private final String[] labels;
        private final dev.orbita.launcher.ui.Animator pos;
        private int selected;

        Choice(String[] labels, int selected, java.util.function.IntConsumer onSelect) {
            this.labels = labels;
            this.selected = selected;
            this.pos = new dev.orbita.launcher.ui.Animator(selected, 14f, this::repaint);
            setOpaque(false);
            setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mousePressed(java.awt.event.MouseEvent e) {
                    int i = Math.min(labels.length - 1, Math.max(0, e.getX() * labels.length / Math.max(1, getWidth())));
                    if (i != Choice.this.selected) {
                        Choice.this.selected = i;
                        pos.setTarget(i);
                        onSelect.accept(i);
                    }
                }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(labels.length * 84, 34);
        }

        @Override
        protected void paintComponent(java.awt.Graphics g0) {
            java.awt.Graphics2D g = (java.awt.Graphics2D) g0.create();
            Theme.hq(g);
            int w = getWidth();
            int h = getHeight();
            float seg = w / (float) labels.length;
            g.setColor(Theme.SURFACE);
            g.fill(new java.awt.geom.RoundRectangle2D.Float(0, 0, w, h, h, h));
            g.setColor(Theme.alpha(Theme.ACCENT, 0.3f));
            g.fill(new java.awt.geom.RoundRectangle2D.Float(pos.get() * seg + 3, 3, seg - 6, h - 6, h - 6, h - 6));
            g.setFont(Theme.font(Font.BOLD, 12f));
            java.awt.FontMetrics fm = g.getFontMetrics();
            for (int i = 0; i < labels.length; i++) {
                g.setColor(i == selected ? Theme.TEXT : Theme.MUTED);
                g.drawString(labels[i], i * seg + (seg - fm.stringWidth(labels[i])) / 2, (h - fm.getHeight()) / 2f + fm.getAscent());
            }
            g.dispose();
        }
    }

    private JComponent section(String title, JComponent... rows) {
        JPanel box = column();
        JLabel t = new UiUtil.AccentLabel(title.toUpperCase());
        t.setFont(Theme.tracked(Font.BOLD, 11f, 0.16f));
        box.add(UiUtil.left(t));
        box.add(UiUtil.vgap(8));
        Card card = new Card(null, 14);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        for (int i = 0; i < rows.length; i++) {
            if (i > 0) {
                card.add(UiUtil.vgap(14));
            }
            card.add(UiUtil.left(rows[i]));
        }
        box.add(UiUtil.left(card));
        box.add(UiUtil.vgap(18));
        box.setAlignmentX(LEFT_ALIGNMENT);
        return box;
    }

    private JComponent setting(String title, String desc, JComponent control) {
        JPanel row = new JPanel(new BorderLayout(24, 0));
        row.setOpaque(false);
        JPanel text = column();
        text.add(UiUtil.left(UiUtil.label(title, Font.BOLD, 14f, Theme.TEXT)));
        text.add(UiUtil.vgap(3));
        text.add(UiUtil.left(UiUtil.label("<html><div style='width:300px'>" + desc + "</div></html>", Font.PLAIN, 13f, Theme.MUTED)));
        row.add(text, BorderLayout.CENTER);
        JPanel ctl = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        ctl.setOpaque(false);
        ctl.add(control);
        row.add(ctl, BorderLayout.EAST);
        return row;
    }

    private JComponent field(String value, String placeholder, int width, Consumer<String> onChange) {
        OTextField f = new OTextField(value == null ? "" : value, placeholder);
        f.setPreferredSize(new Dimension(width, 40));
        f.getDocument().addDocumentListener(new DocumentListener() {
            private void changed() {
                onChange.accept(f.getText().trim());
                launcher.config.save();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                changed();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                changed();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                changed();
            }
        });
        return f;
    }

    private OButton button(String text, Icons.Type icon, Runnable action) {
        OButton b = new OButton(text, OButton.Kind.GHOST, icon);
        b.addActionListener(e -> action.run());
        return b;
    }

    private void open(Path dir) {
        try {
            Files.createDirectories(dir);
            Desktop.getDesktop().open(dir.toFile());
        } catch (Exception e) {
            Dialogs.info(frame, "No se pudo abrir la carpeta", String.valueOf(e.getMessage()));
        }
    }

    private static JPanel column() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        return p;
    }

    private static String gb(int mb) {
        return String.format("%.1f GB", mb / 1024.0);
    }
}
