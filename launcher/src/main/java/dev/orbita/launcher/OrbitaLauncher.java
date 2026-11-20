package dev.orbita.launcher;

import dev.orbita.launcher.core.Progress;
import dev.orbita.launcher.core.install.GameInstaller;
import dev.orbita.launcher.ui.LauncherFrame;
import dev.orbita.launcher.ui.Theme;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.util.List;

public final class OrbitaLauncher {
    private OrbitaLauncher() {
    }

    public static void main(String[] args) {
        if (Runtime.version().feature() < 21) {
            JOptionPane.showMessageDialog(null, "Órbita necesita Java 21 o superior.", "Órbita", JOptionPane.ERROR_MESSAGE);
            return;
        }
        List<String> a = List.of(args);
        if (a.contains("--headless")) {
            System.exit(headless(a));
            return;
        }
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        int shots = a.indexOf("--devshots");
        SwingUtilities.invokeLater(() -> {
            Theme.installDefaults();
            Launcher launcher = new Launcher();
            LauncherFrame frame = new LauncherFrame(launcher);
            frame.open();
            if (shots >= 0 && shots + 1 < a.size()) {
                devShots(launcher, frame, java.nio.file.Path.of(a.get(shots + 1)));
            }
        });
    }

    private static java.awt.Component find(java.awt.Container c, String simpleName) {
        for (java.awt.Component k : c.getComponents()) {
            if (k.getClass().getSimpleName().equals(simpleName)) {
                return k;
            }
            if (k instanceof java.awt.Container cc) {
                java.awt.Component r = find(cc, simpleName);
                if (r != null) {
                    return r;
                }
            }
        }
        return null;
    }

    /** Solo pruebas ({@code --devshots carpeta}): guarda una imagen de cada página y cierra. */
    private static void devShots(Launcher launcher, LauncherFrame frame, java.nio.file.Path dir) {
        String[] pages = {"play", "accounts", "mods", "perf", "settings"};
        int[] i = {0};
        javax.swing.Timer t = new javax.swing.Timer(3500, null);
        t.addActionListener(e -> {
            try {
                if (i[0] > 0) {
                    java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(frame.getWidth(), frame.getHeight(),
                            java.awt.image.BufferedImage.TYPE_INT_RGB);
                    java.awt.Graphics2D g = img.createGraphics();
                    frame.getRootPane().paint(g);
                    g.dispose();
                    java.nio.file.Files.createDirectories(dir);
                    javax.imageio.ImageIO.write(img, "png", dir.resolve("launcher-" + pages[i[0] - 1] + ".png").toFile());
                }
                if (i[0] >= pages.length) {
                    t.stop();
                    if (Boolean.getBoolean("orbita.devshots.switcher")) { // menú de cuentas abierto desde la barra lateral
                        launcher.addAccount(dev.orbita.launcher.core.auth.Account.offline("Amigo_Uno"));
                        launcher.addAccount(dev.orbita.launcher.core.auth.Account.offline("Prueba_Dos"));
                        launcher.navigate.accept("play");
                        java.awt.Component badge = find(frame.getContentPane(), "AccountBadge");
                        javax.swing.Timer open = new javax.swing.Timer(1500, ev -> {
                            dev.orbita.launcher.ui.AccountSwitcher.show(launcher, frame, badge, false);
                            javax.swing.Timer shot = new javax.swing.Timer(800, ev2 -> {
                                try {
                                    java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(frame.getWidth(), frame.getHeight(),
                                            java.awt.image.BufferedImage.TYPE_INT_RGB);
                                    java.awt.Graphics2D g = img.createGraphics();
                                    frame.getRootPane().paint(g);
                                    g.dispose();
                                    javax.imageio.ImageIO.write(img, "png", dir.resolve("launcher-cambiar-cuenta.png").toFile());
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                                System.exit(0);
                            });
                            shot.setRepeats(false);
                            shot.start();
                        });
                        open.setRepeats(false);
                        open.start();
                        return;
                    }
                    if (Boolean.getBoolean("orbita.devshots.login")) { // abre la ventana de Microsoft y la fotografía
                        dev.orbita.launcher.ui.MicrosoftWebLogin.start(launcher, frame);
                        javax.swing.Timer shot = new javax.swing.Timer(9000, ev -> {
                            try {
                                java.awt.Rectangle b = frame.getBounds();
                                java.awt.Rectangle r = new java.awt.Rectangle(b.x + Math.max(0, (b.width - 500) / 2) - 10,
                                        b.y + Math.max(0, (b.height - 650) / 2) - 40, 520, 700);
                                javax.imageio.ImageIO.write(new java.awt.Robot().createScreenCapture(r), "png",
                                        dir.resolve("launcher-login-microsoft.png").toFile());
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                            System.exit(0);
                        });
                        shot.setRepeats(false);
                        shot.start();
                        return;
                    }
                    System.exit(0);
                    return;
                }
                launcher.navigate.accept(pages[i[0]++]);
            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(1);
            }
        });
        t.setInitialDelay(4000);
        t.start();
    }

    /**
     * Modo sin interfaz para pruebas y scripts:
     * {@code --headless [--install-only] [--username Nombre]}.
     */
    private static int headless(List<String> args) {
        Launcher launcher = new Launcher();
        int i = args.indexOf("--username");
        if (i >= 0 && i + 1 < args.size()) {
            launcher.addAccount(dev.orbita.launcher.core.auth.Account.offline(args.get(i + 1)));
        }
        try {
            if (args.contains("--install-only")) {
                new GameInstaller(false).install(Progress.CONSOLE);
                launcher.mods.migrateIfNeeded(Progress.CONSOLE);
                launcher.mods.quarantineIncompatible(Progress.CONSOLE);
                launcher.mods.ensureCore(Progress.CONSOLE);
                System.out.println("[Órbita] Instalación completa.");
                return 0;
            }
            Process game = launcher.launch(Progress.CONSOLE);
            return game.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
            return 1;
        }
    }
}
