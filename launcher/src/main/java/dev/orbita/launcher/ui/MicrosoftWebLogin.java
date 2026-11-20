package dev.orbita.launcher.ui;

import dev.orbita.launcher.Launcher;
import dev.orbita.launcher.core.Progress;
import dev.orbita.launcher.core.auth.Account;
import dev.orbita.launcher.core.auth.MicrosoftAuth;
import dev.orbita.launcher.ui.components.Dialogs;
import dev.orbita.launcher.ui.components.UiUtil;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Dialog;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Inicio de sesión con Microsoft como en el launcher oficial y en Nova: se abre una ventana con la página de
 * Microsoft, pones tu correo y contraseña ahí, y al terminar Microsoft devuelve una autorización que Órbita cambia por
 * tu cuenta de Minecraft. Órbita nunca ve tu contraseña (la escribes en la página de Microsoft).
 */
public final class MicrosoftWebLogin {
    private static boolean started;

    private MicrosoftWebLogin() {
    }

    private static synchronized void startToolkit() {
        if (!started) {
            started = true;
            Platform.setImplicitExit(false);
            try {
                Platform.startup(() -> { });
            } catch (IllegalStateException alreadyRunning) {
                // ya estaba iniciado (p. ej. por el video de fondo)
            }
        }
    }

    public static void start(Launcher launcher, JFrame frame) {
        try {
            Class.forName("javafx.scene.web.WebView");
            startToolkit();
        } catch (Throwable t) {
            Dialogs.info(frame, "No se pudo abrir el inicio de sesión",
                    "No se pudo abrir la ventana de Microsoft (" + t + "). Reinstala Órbita e inténtalo de nuevo.");
            return;
        }
        // Ventana propia de JavaFX (no se mete dentro de Swing): así no depende de jdk.swing.interop.
        Rectangle owner = frame.getBounds();
        List<java.awt.Image> icons = frame.getIconImages();
        AtomicBoolean done = new AtomicBoolean();
        frame.setEnabled(false); // como una ventana modal: Órbita espera a que termines
        Platform.runLater(() -> {
            Stage stage = new Stage();
            try {
                stage.setTitle("Sign in to Minecraft");
                for (java.awt.Image icon : icons) {
                    javafx.scene.image.Image fx = toFx(icon);
                    if (fx != null) {
                        stage.getIcons().add(fx);
                    }
                }
                CookieHandler.setDefault(new CookieManager()); // sesión limpia: no se cuela una cuenta anterior
                WebView web = new WebView();
                // la ventana se llama como la página ("Sign in to Minecraft"), igual que en Nova
                web.getEngine().titleProperty().addListener((obs, oldTitle, title) -> {
                    if (title != null && !title.isBlank()) {
                        stage.setTitle(title);
                    }
                });
                web.getEngine().locationProperty().addListener((obs, oldUrl, url) -> {
                    if (url == null || !url.startsWith(MicrosoftAuth.LIVE_REDIRECT) || !done.compareAndSet(false, true)) {
                        return;
                    }
                    String code = param(url, "code");
                    String error = param(url, "error");
                    String errorText = param(url, "error_description");
                    stage.close();
                    SwingUtilities.invokeLater(() -> {
                        frame.setEnabled(true);
                        frame.toFront();
                        if (code != null) {
                            finish(launcher, frame, code);
                        } else if (error != null && !"access_denied".equals(error)) {
                            Dialogs.info(frame, "No se pudo iniciar sesión", errorText != null ? errorText : error);
                        }
                    });
                });
                stage.setOnHidden(e -> { // cerrada con la X sin terminar
                    if (done.compareAndSet(false, true)) {
                        SwingUtilities.invokeLater(() -> {
                            frame.setEnabled(true);
                            frame.toFront();
                        });
                    }
                });
                double w = 500, h = 650;
                stage.setScene(new Scene(web, w, h));
                stage.setX(owner.x + Math.max(0, (owner.width - w) / 2));
                stage.setY(owner.y + Math.max(0, (owner.height - h) / 2));
                stage.setAlwaysOnTop(true);
                web.getEngine().load(MicrosoftAuth.browserLoginUrl());
                stage.show();
            } catch (Throwable t) {
                done.set(true);
                stage.close();
                SwingUtilities.invokeLater(() -> {
                    frame.setEnabled(true);
                    Dialogs.info(frame, "No se pudo abrir el inicio de sesión",
                            "No se pudo abrir la página de Microsoft (" + t + ").");
                });
            }
        });
    }

    /** Icono de Swing -> icono de JavaFX (sin usar javafx.swing). */
    private static javafx.scene.image.Image toFx(java.awt.Image img) {
        int w = img.getWidth(null), h = img.getHeight(null);
        if (w <= 0 || h <= 0) {
            return null;
        }
        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = bi.createGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();
        WritableImage out = new WritableImage(w, h);
        PixelWriter pw = out.getPixelWriter();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                pw.setArgb(x, y, bi.getRGB(x, y));
            }
        }
        return out;
    }

    /** Cambia el código por la cuenta (Xbox → Minecraft) mostrando el progreso. */
    private static void finish(Launcher launcher, JFrame frame, String code) {
        JDialog status = Dialogs.base(frame);
        status.setModalityType(Dialog.ModalityType.MODELESS);
        JLabel text = UiUtil.muted("Conectando con Microsoft…");
        JPanel content = Dialogs.content();
        content.add(UiUtil.left(UiUtil.label("Añadiendo tu cuenta", Font.BOLD, 18f, Theme.TEXT)));
        content.add(UiUtil.vgap(10));
        content.add(UiUtil.left(text));
        Dialogs.show(status, content, frame);

        launcher.worker.submit(() -> {
            try {
                Account acc = new MicrosoftAuth(launcher.config.microsoftClientId).loginWithCode(code, new Progress() {
                    @Override
                    public void status(String t) {
                        UiUtil.ui(() -> text.setText(t));
                    }

                    @Override
                    public void progress(double f) {
                    }

                    @Override
                    public void log(String line) {
                    }
                });
                launcher.addAccount(acc);
                UiUtil.ui(status::dispose);
            } catch (Exception e) {
                UiUtil.ui(() -> {
                    status.dispose();
                    Dialogs.info(frame, "No se pudo iniciar sesión", e.getMessage());
                });
            }
        });
    }

    private static String param(String url, String name) {
        String q = URI.create(url).getRawQuery();
        if (q == null) {
            return null;
        }
        for (String part : q.split("&")) {
            int i = part.indexOf('=');
            if (i > 0 && part.substring(0, i).equals(name)) {
                return URLDecoder.decode(part.substring(i + 1), StandardCharsets.UTF_8);
            }
        }
        return null;
    }
}
