package dev.orbita.launcher.ui;

import dev.orbita.launcher.core.GamePaths;
import dev.orbita.launcher.core.LauncherConfig;
import dev.orbita.launcher.ui.components.Dialogs;

import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

/**
 * Aplica el tema elegido por el usuario: color de acento, fondo (universo, color, imagen o video) y música.
 * Los archivos elegidos se copian a .orbita\launcher-theme para que sigan funcionando aunque el original se mueva.
 */
public final class Appearance {
    public static final String[] IMAGE_EXT = {"png", "jpg", "jpeg", "gif", "bmp", "webp"};
    public static final String[] VIDEO_EXT = {"mp4", "m4v"};
    public static final String[] AUDIO_EXT = {"mp3", "wav", "m4a"};

    private final JFrame frame;
    private final StarfieldPanel background;
    private final LauncherConfig cfg;
    private MediaLayer media;
    private JComponent videoView;

    public Appearance(JFrame frame, StarfieldPanel background, LauncherConfig cfg) {
        this.frame = frame;
        this.background = background;
        this.cfg = cfg;
        // Con la ventana oculta (jugando) o minimizada, video y música se pausan.
        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentHidden(ComponentEvent e) {
                pause(true);
            }

            @Override
            public void componentShown(ComponentEvent e) {
                pause(false);
            }
        });
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowIconified(WindowEvent e) {
                pause(true);
            }

            @Override
            public void windowDeiconified(WindowEvent e) {
                pause(false);
            }
        });
    }

    public static void applyAccent(LauncherConfig cfg) {
        if (cfg.themePreset >= 0 && cfg.themePreset < Theme.PRESETS.length) {
            Theme.Preset p = Theme.PRESETS[cfg.themePreset];
            Theme.applyAccent(new Color(p.accent()), new Color(p.accent2()));
        } else {
            Theme.applyAccent(new Color(cfg.customAccent), null);
        }
    }

    /** Aplica todo según la configuración actual. */
    public void apply() {
        applyAccent(cfg);
        StarfieldPanel.Mode mode = parse(cfg.backgroundMode);
        File file = cfg.backgroundFile == null || cfg.backgroundFile.isBlank() ? null : new File(cfg.backgroundFile);
        if ((mode == StarfieldPanel.Mode.IMAGE || mode == StarfieldPanel.Mode.VIDEO) && (file == null || !file.isFile())) {
            mode = StarfieldPanel.Mode.UNIVERSE;
        }
        background.setMode(mode, file, cfg.backgroundDim);

        if (mode == StarfieldPanel.Mode.VIDEO) {
            MediaLayer m = media();
            if (videoView == null) {
                videoView = m.videoComponent();
                JLayeredPane layers = frame.getLayeredPane();
                layers.add(videoView, Integer.valueOf(JLayeredPane.FRAME_CONTENT_LAYER - 1));
                videoView.setBounds(0, 0, frame.getWidth(), frame.getHeight());
                frame.addComponentListener(new ComponentAdapter() {
                    @Override
                    public void componentResized(ComponentEvent e) {
                        videoView.setBounds(0, 0, frame.getWidth(), frame.getHeight());
                    }
                });
            }
            videoView.setVisible(true);
            m.playVideo(file, cfg.videoSound, cfg.videoVolume, msg -> error(file, msg));
        } else if (media != null) {
            media.stopVideo();
            if (videoView != null) {
                videoView.setVisible(false);
            }
        }

        File music = cfg.musicFile == null || cfg.musicFile.isBlank() ? null : new File(cfg.musicFile);
        if (cfg.musicEnabled && music != null && music.isFile()) {
            media().playMusic(music, cfg.musicVolume, msg -> error(music, msg));
        } else if (media != null) {
            media.stopMusic();
        }
        frame.repaint();
    }

    public void updateVolumes() {
        if (media != null) {
            media.setVideoSound(cfg.videoSound, cfg.videoVolume);
            media.setMusicVolume(cfg.musicVolume);
        }
    }

    public void updateDim() {
        background.setMode(background.mode(), cfg.backgroundFile == null ? null : new File(cfg.backgroundFile), cfg.backgroundDim);
    }

    private MediaLayer media() {
        if (media == null) {
            media = new MediaLayer();
        }
        return media;
    }

    private void pause(boolean paused) {
        if (media != null) {
            media.setPaused(paused);
        }
    }

    /** Archivos que ya dieron error: se avisa una sola vez por archivo, no en cada cambio del tema. */
    private final java.util.Set<String> reported = new java.util.HashSet<>();

    private void error(File file, String message) {
        SwingUtilities.invokeLater(() -> {
            if (!reported.add(file.getAbsolutePath() + "|" + file.length())) {
                return;
            }
            Dialogs.info(frame, "No se pudo reproducir " + file.getName(), message);
        });
    }

    /** Copia el archivo elegido a la carpeta del tema y devuelve la copia. */
    public static File importFile(File source, String kind) throws IOException {
        Path dir = GamePaths.root().resolve("launcher-theme");
        Files.createDirectories(dir);
        String name = source.getName();
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.')).toLowerCase(Locale.ROOT) : "";
        Path target = dir.resolve(kind + ext);
        if (!source.toPath().toAbsolutePath().equals(target.toAbsolutePath())) {
            // Se borran las copias anteriores de este tipo (con otra extensión).
            try (var old = Files.list(dir)) {
                for (Path p : (Iterable<Path>) old::iterator) {
                    if (p.getFileName().toString().startsWith(kind + ".") && !p.equals(target)) {
                        Files.deleteIfExists(p);
                    }
                }
            }
            Files.copy(source.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
        }
        return target.toFile();
    }

    public static StarfieldPanel.Mode parse(String mode) {
        try {
            return StarfieldPanel.Mode.valueOf(mode);
        } catch (Exception e) {
            return StarfieldPanel.Mode.UNIVERSE;
        }
    }

    public static boolean hasExtension(File f, String[] exts) {
        String n = f.getName().toLowerCase(Locale.ROOT);
        for (String e : exts) {
            if (n.endsWith("." + e)) {
                return true;
            }
        }
        return false;
    }
}
