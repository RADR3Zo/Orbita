package dev.orbita.launcher.ui;

import dev.orbita.launcher.Launcher;
import dev.orbita.launcher.core.Progress;
import dev.orbita.launcher.core.auth.Account;
import dev.orbita.launcher.core.auth.MicrosoftAuth;
import dev.orbita.launcher.ui.components.Dialogs;
import dev.orbita.launcher.ui.components.OButton;
import dev.orbita.launcher.ui.components.UiUtil;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Inicio de sesión con Microsoft (código de dispositivo, con el login oficial de Microsoft). Órbita nunca ve tu
 * contraseña: se escribe en la página oficial de Microsoft. La cuenta se añade a la lista de cuentas.
 */
public final class MicrosoftLogin {
    private MicrosoftLogin() {
    }

    public static void start(Launcher launcher, JFrame frame) {
        MicrosoftAuth auth = new MicrosoftAuth(launcher.config.microsoftClientId);
        AtomicBoolean cancelled = new AtomicBoolean();
        JDialog dialog = Dialogs.base(frame);
        JLabel code = UiUtil.label("…", Font.BOLD, 34f, Theme.TEXT);
        JLabel dialogStatus = UiUtil.muted("Solicitando código…");
        JPanel content = Dialogs.content();
        content.add(UiUtil.left(UiUtil.label("Añadir cuenta de Microsoft", Font.BOLD, 18f, Theme.TEXT)));
        content.add(UiUtil.vgap(10));
        content.add(UiUtil.left(Dialogs.text("Abre la página de Microsoft, introduce este código y acepta los permisos. "
                + "Órbita nunca ve tu contraseña.")));
        content.add(UiUtil.vgap(16));
        content.add(UiUtil.left(code));
        content.add(UiUtil.vgap(8));
        content.add(UiUtil.left(dialogStatus));
        content.add(UiUtil.vgap(18));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        OButton cancel = new OButton("Cancelar", OButton.Kind.GHOST);
        cancel.addActionListener(e -> {
            cancelled.set(true);
            dialog.dispose();
        });
        OButton open = new OButton("Copiar código y abrir navegador", OButton.Kind.SECONDARY);
        open.setEnabled(false);
        buttons.add(cancel);
        buttons.add(open);
        content.add(UiUtil.left(buttons));

        launcher.worker.submit(() -> {
            try {
                MicrosoftAuth.DeviceCode dc = auth.requestDeviceCode();
                UiUtil.ui(() -> {
                    code.setText(dc.userCode());
                    dialogStatus.setText("Esperando confirmación en " + dc.verificationUri());
                    open.setEnabled(true);
                    open.addActionListener(e -> {
                        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(dc.userCode()), null);
                        try {
                            Desktop.getDesktop().browse(URI.create(dc.verificationUri()));
                        } catch (Exception ex) {
                            dialogStatus.setText("Abre " + dc.verificationUri() + " en tu navegador.");
                        }
                    });
                });
                Account acc = auth.pollForAccount(dc, cancelled::get, new Progress() {
                    @Override
                    public void status(String t) {
                        UiUtil.ui(() -> dialogStatus.setText(t));
                    }

                    @Override
                    public void progress(double f) {
                    }

                    @Override
                    public void log(String line) {
                    }
                });
                launcher.addAccount(acc);
                UiUtil.ui(dialog::dispose);
            } catch (Exception e) {
                if (!cancelled.get()) {
                    UiUtil.ui(() -> {
                        dialogStatus.setForeground(Theme.DANGER);
                        dialogStatus.setText(e.getMessage());
                        cancel.setText("Cerrar");
                    });
                }
            }
        });
        Dialogs.show(dialog, content, frame);
        cancelled.set(true);
    }
}
