package dev.orbita.launcher.ui;

import dev.orbita.launcher.LauncherInfo;
import dev.orbita.launcher.core.Progress;
import dev.orbita.launcher.core.update.Updater;
import dev.orbita.launcher.ui.components.Dialogs;
import dev.orbita.launcher.ui.components.UiUtil;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Desktop;
import java.awt.Font;
import java.net.URI;

/** Busca actualizaciones en GitHub al abrir el launcher y, si hay una, ofrece instalarla. */
public final class UpdatePrompt {
    private UpdatePrompt() {
    }

    /** Se llama al abrir la ventana. Todo el trabajo de red va en otro hilo para no congelar el launcher. */
    public static void checkInBackground(LauncherFrame frame) {
        Updater.cleanupOldJars();
        String repo = Updater.repo();
        if (repo.isEmpty()) {
            return; // sin repositorio configurado (compilación local)
        }
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(2500); // que primero se vea el launcher
                Updater.Release r = Updater.check(repo, LauncherInfo.VERSION);
                if (r != null) {
                    SwingUtilities.invokeLater(() -> offer(frame, r));
                }
            } catch (Exception e) {
                System.err.println("[Órbita] No se pudo buscar actualizaciones: " + e.getMessage());
            }
        }, "orbita-update-check");
        t.setDaemon(true);
        t.start();
    }

    private static void offer(LauncherFrame frame, Updater.Release r) {
        String notes = r.notes() == null ? "" : r.notes().replace(Updater.FULL_DOWNLOAD_TAG, "").trim();
        if (notes.length() > 600) {
            notes = notes.substring(0, 600) + "…";
        }
        String msg = "Hay una versión nueva de Órbita: " + r.version() + " (tienes " + LauncherInfo.VERSION + ")."
                + (notes.isEmpty() ? "" : "\n\nNovedades:\n" + notes);

        boolean auto = !r.needsFullDownload() && Updater.canSelfUpdate();
        if (!auto) {
            if (Dialogs.confirm(frame, "Actualización disponible", msg
                            + "\n\nEsta actualización se descarga completa desde GitHub.",
                    "Abrir descarga", "Más tarde", false)) {
                browse(r.zipUrl() != null ? r.zipUrl() : r.pageUrl());
            }
            return;
        }
        if (!Dialogs.confirm(frame, "Actualización disponible", msg, "Actualizar ahora", "Más tarde", false)) {
            return;
        }
        install(frame, r);
    }

    private static void install(LauncherFrame frame, Updater.Release r) {
        JDialog d = Dialogs.base(frame);
        d.setModalityType(java.awt.Dialog.ModalityType.MODELESS);
        JPanel content = Dialogs.content();
        content.add(UiUtil.left(UiUtil.label("Actualizando Órbita", Font.BOLD, 18f, Theme.TEXT)));
        content.add(UiUtil.vgap(10));
        JLabel status = UiUtil.muted("Preparando…");
        content.add(UiUtil.left(status));
        Dialogs.show(d, content, frame);

        Progress p = new Progress() {
            @Override
            public void status(String text) {
                SwingUtilities.invokeLater(() -> status.setText(text));
            }

            @Override
            public void progress(double fraction) {
            }

            @Override
            public void log(String line) {
            }
        };
        Thread t = new Thread(() -> {
            try {
                Updater.install(r, p);
                Thread.sleep(800);
                Updater.restart();
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    d.dispose();
                    if (Dialogs.confirm(frame, "No se pudo actualizar", e.getMessage()
                                    + "\n\nPuedes descargarla a mano desde GitHub.",
                            "Abrir descarga", "Cerrar", false)) {
                        browse(r.zipUrl() != null ? r.zipUrl() : r.pageUrl());
                    }
                });
            }
        }, "orbita-update-install");
        t.setDaemon(true);
        t.start();
    }

    private static void browse(String url) {
        if (url == null) {
            return;
        }
        try {
            Desktop.getDesktop().browse(URI.create(url));
        } catch (Exception e) {
            System.err.println("[Órbita] No se pudo abrir el navegador: " + e.getMessage());
        }
    }
}
