package dev.orbita.launcher.ui;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;

import javax.swing.JComponent;
import java.io.File;
import java.util.function.Consumer;

/**
 * Video y música de fondo del launcher con JavaFX Media (MP4/M4V con H.264, y MP3, WAV o M4A para música).
 * Esta clase solo se carga si el usuario elige video o música: con los demás fondos JavaFX ni se inicia.
 * Todo se pausa mientras la ventana está oculta (por ejemplo, mientras juegas).
 */
public final class MediaLayer {
    private static boolean started;

    private JFXPanel videoPanel;
    private MediaPlayer video;
    private MediaPlayer music;

    private static synchronized void startToolkit() {
        if (!started) {
            started = true;
            Platform.setImplicitExit(false);
            try {
                Platform.startup(() -> { });
            } catch (IllegalStateException alreadyRunning) {
                // ya estaba iniciado
            }
        }
    }

    /** Componente Swing que muestra el video (va detrás de toda la interfaz). */
    public JComponent videoComponent() {
        if (videoPanel == null) {
            startToolkit();
            videoPanel = new JFXPanel();
            videoPanel.setOpaque(true);
        }
        return videoPanel;
    }

    public void playVideo(File file, boolean sound, int volume, Consumer<String> onError) {
        stopVideo();
        JFXPanel panel = (JFXPanel) videoComponent();
        Platform.runLater(() -> {
            try {
                MediaPlayer player = new MediaPlayer(new Media(file.toURI().toString()));
                player.setCycleCount(MediaPlayer.INDEFINITE);
                player.setMute(!sound);
                player.setVolume(volume / 100.0);
                player.setOnError(() -> onError.accept(describe(player.getError())));
                MediaView view = new MediaView(player);
                view.setPreserveRatio(true);
                view.setSmooth(true);
                Group root = new Group(view);
                Scene scene = new Scene(root, javafx.scene.paint.Color.BLACK);
                // Recorta el video para cubrir toda la ventana sin bandas negras.
                Runnable fit = () -> {
                    double w = scene.getWidth();
                    double h = scene.getHeight();
                    double vw = player.getMedia().getWidth();
                    double vh = player.getMedia().getHeight();
                    if (vw <= 0 || vh <= 0 || w <= 0 || h <= 0) {
                        view.setFitWidth(w);
                        return;
                    }
                    double s = Math.max(w / vw, h / vh);
                    view.setFitWidth(vw * s);
                    view.setFitHeight(vh * s);
                    view.setX((w - vw * s) / 2);
                    view.setY((h - vh * s) / 2);
                };
                scene.widthProperty().addListener((o, a, b) -> fit.run());
                scene.heightProperty().addListener((o, a, b) -> fit.run());
                player.setOnReady(() -> {
                    fit.run();
                    player.play();
                });
                panel.setScene(scene);
                video = player;
            } catch (Exception e) {
                onError.accept(describe(e));
            }
        });
    }

    public void setVideoSound(boolean sound, int volume) {
        MediaPlayer p = video;
        if (p != null) {
            Platform.runLater(() -> {
                p.setMute(!sound);
                p.setVolume(volume / 100.0);
            });
        }
    }

    public void stopVideo() {
        MediaPlayer p = video;
        video = null;
        if (p != null) {
            Platform.runLater(p::dispose);
        }
    }

    public void playMusic(File file, int volume, Consumer<String> onError) {
        stopMusic();
        startToolkit();
        Platform.runLater(() -> {
            try {
                MediaPlayer player = new MediaPlayer(new Media(file.toURI().toString()));
                player.setCycleCount(MediaPlayer.INDEFINITE);
                player.setVolume(volume / 100.0);
                player.setOnError(() -> onError.accept(describe(player.getError())));
                player.setOnReady(player::play);
                music = player;
            } catch (Exception e) {
                onError.accept(describe(e));
            }
        });
    }

    public void setMusicVolume(int volume) {
        MediaPlayer p = music;
        if (p != null) {
            Platform.runLater(() -> p.setVolume(volume / 100.0));
        }
    }

    public void stopMusic() {
        MediaPlayer p = music;
        music = null;
        if (p != null) {
            Platform.runLater(p::dispose);
        }
    }

    /** Pausa o reanuda todo (al ocultar o mostrar la ventana). */
    public void setPaused(boolean paused) {
        MediaPlayer v = video;
        MediaPlayer m = music;
        if (v == null && m == null) {
            return;
        }
        Platform.runLater(() -> {
            if (v != null) {
                if (paused) v.pause(); else v.play();
            }
            if (m != null) {
                if (paused) m.pause(); else m.play();
            }
        });
    }

    /** Traduce los errores de JavaFX Media a algo comprensible. */
    private static String describe(Throwable e) {
        String msg = e == null || e.getMessage() == null ? "" : e.getMessage();
        if (msg.contains("Unrecognized file signature") || msg.contains("MEDIA_UNSUPPORTED") || msg.contains("Unsupported")) {
            return "Este formato no es compatible. Para música usa MP3, WAV o M4A (los .aac sueltos no funcionan); "
                    + "para video, MP4 con H.264.";
        }
        if (msg.contains("MEDIA_INACCESSIBLE") || msg.contains("FileNotFound")) {
            return "No se encuentra el archivo o no se puede leer. Vuelve a elegirlo en Ajustes → Apariencia.";
        }
        return msg.isBlank() ? "Formato no compatible. Usa MP4 (H.264) para video o MP3/WAV/M4A para música." : msg;
    }
}
