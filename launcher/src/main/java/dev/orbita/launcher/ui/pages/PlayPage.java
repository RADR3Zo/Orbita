package dev.orbita.launcher.ui.pages;

import dev.orbita.launcher.Launcher;
import dev.orbita.launcher.LauncherInfo;
import dev.orbita.launcher.core.GamePaths;
import dev.orbita.launcher.core.Progress;
import dev.orbita.launcher.core.auth.Account;
import dev.orbita.launcher.core.auth.MicrosoftAuth;
import dev.orbita.launcher.ui.Icons;
import dev.orbita.launcher.ui.Theme;
import dev.orbita.launcher.ui.UiProgress;
import dev.orbita.launcher.ui.components.Card;
import dev.orbita.launcher.ui.components.Dialogs;
import dev.orbita.launcher.ui.components.OButton;
import dev.orbita.launcher.ui.components.OProgressBar;
import dev.orbita.launcher.ui.components.OTextField;
import dev.orbita.launcher.ui.components.UiUtil;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/** Página principal: cuenta, estadísticas y botón JUGAR con progreso y consola. */
public final class PlayPage extends JPanel {
    private final Launcher launcher;
    private final JFrame frame;
    private final JLabel nameLabel = UiUtil.label("", Font.BOLD, 40f, Theme.TEXT);
    private final JLabel modsValue = UiUtil.label("—", Font.BOLD, 22f, Theme.TEXT);
    private final JLabel ramValue = UiUtil.label("", Font.BOLD, 22f, Theme.TEXT);
    private final JPanel accountBody = new JPanel();
    private final JLabel status = UiUtil.muted("Listo para despegar.");
    private final OProgressBar bar = new OProgressBar();
    private final JTextArea console = new JTextArea();
    private final JScrollPane consoleScroll;
    private final OButton play = new OButton("JUGAR", OButton.Kind.PRIMARY, Icons.Type.PLAY).arc(16);
    private final UiProgress progress;

    public PlayPage(Launcher launcher, JFrame frame) {
        this.launcher = launcher;
        this.frame = frame;
        setOpaque(false);
        setLayout(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(4, 40, 30, 40));

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        JLabel eyebrow = new UiUtil.AccentLabel("BIENVENIDO DE VUELTA");
        eyebrow.setFont(Theme.tracked(Font.BOLD, 12f, 0.18f));
        top.add(UiUtil.left(eyebrow));
        top.add(UiUtil.vgap(6));
        top.add(UiUtil.left(nameLabel));
        top.add(UiUtil.vgap(4));
        top.add(UiUtil.left(UiUtil.muted("Minecraft " + LauncherInfo.MINECRAFT + " con Fabric, optimizado y listo para despegar.")));
        top.add(UiUtil.vgap(22));

        JPanel stats = new JPanel(new GridLayout(1, 3, 14, 0));
        stats.setOpaque(false);
        stats.add(stat("VERSIÓN", UiUtil.label(LauncherInfo.MINECRAFT, Font.BOLD, 22f, Theme.TEXT), "Fabric Loader " + LauncherInfo.FABRIC_LOADER));
        stats.add(stat("MODS ACTIVOS", modsValue, "en la carpeta mods"));
        stats.add(stat("MEMORIA", ramValue, "asignados al juego"));
        stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 96));
        top.add(UiUtil.left(stats));
        top.add(UiUtil.vgap(14));

        Card account = new Card(new BorderLayout());
        accountBody.setOpaque(false);
        account.add(accountBody);
        account.setMaximumSize(new Dimension(Integer.MAX_VALUE, 86));
        top.add(UiUtil.left(account));
        top.add(UiUtil.vgap(14));

        JPanel hints = new JPanel(new GridLayout(1, 3, 14, 0));
        hints.setOpaque(false);
        hints.add(hint("Shift derecho", "Panel de Órbita: HUD, zoom, mods y cosméticos"));
        hints.add(hint("Z (mantener)", "Zoom suave; la rueda ajusta el aumento"));
        hints.add(hint("Guardarropa", "Capas espaciales desde el menú principal"));
        hints.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        top.add(UiUtil.left(hints));
        add(top, BorderLayout.NORTH);

        console.setEditable(false);
        console.setOpaque(false);
        console.setForeground(Theme.MUTED);
        console.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        console.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        Card consoleCard = new Card(new BorderLayout(), 14);
        consoleCard.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        consoleScroll = UiUtil.scroll(console);
        consoleCard.add(consoleScroll);
        consoleCard.setVisible(false);
        add(consoleCard, BorderLayout.CENTER);

        progress = new UiProgress(status, bar, console);

        JPanel bottom = new JPanel(new BorderLayout(20, 0));
        bottom.setOpaque(false);
        JPanel statusCol = new JPanel();
        statusCol.setOpaque(false);
        statusCol.setLayout(new BoxLayout(statusCol, BoxLayout.Y_AXIS));
        statusCol.add(Box.createVerticalGlue());
        statusCol.add(UiUtil.left(status));
        statusCol.add(UiUtil.vgap(10));
        statusCol.add(UiUtil.left(bar));
        statusCol.add(UiUtil.vgap(8));
        OButton consoleToggle = new OButton("Mostrar consola", OButton.Kind.GHOST);
        consoleToggle.setFont(Theme.font(Font.PLAIN, 12f));
        consoleToggle.addActionListener(e -> {
            consoleCard.setVisible(!consoleCard.isVisible());
            consoleToggle.setText(consoleCard.isVisible() ? "Ocultar consola" : "Mostrar consola");
            revalidate();
        });
        JPanel toggleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        toggleRow.setOpaque(false);
        toggleRow.add(consoleToggle);
        statusCol.add(UiUtil.left(toggleRow));
        bottom.add(statusCol, BorderLayout.CENTER);

        play.setFont(Theme.tracked(Font.BOLD, 18f, 0.12f));
        play.setPreferredSize(new Dimension(250, 72));
        play.addActionListener(e -> play(consoleCard));
        bottom.add(play, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        launcher.onAccountChanged(this::refreshAccount);
        launcher.onModsChanged(this::refreshStats);
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                refreshStats();
            }
        });
        refreshAccount();
        refreshStats();
    }

    private static Card stat(String title, JLabel value, String sub) {
        Card c = new Card(null, 14);
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        c.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        JLabel t = new JLabel(title);
        t.setFont(Theme.tracked(Font.BOLD, 11f, 0.15f));
        t.setForeground(Theme.MUTED);
        c.add(UiUtil.left(t));
        c.add(UiUtil.vgap(4));
        c.add(UiUtil.left(value));
        c.add(UiUtil.vgap(2));
        c.add(UiUtil.left(UiUtil.label(sub, Font.PLAIN, 12f, Theme.DISABLED)));
        return c;
    }

    private static JPanel hint(String key, String text) {
        JPanel p = new JPanel(new BorderLayout(10, 0));
        p.setOpaque(false);
        JLabel k = new JLabel(key) {
            @Override
            protected void paintComponent(java.awt.Graphics g0) {
                java.awt.Graphics2D g = (java.awt.Graphics2D) g0.create();
                Theme.hq(g);
                g.setColor(Theme.alpha(Theme.ACCENT, 0.16f));
                g.fill(new java.awt.geom.RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g.setColor(Theme.alpha(Theme.ACCENT, 0.45f));
                g.draw(new java.awt.geom.RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 10, 10));
                g.dispose();
                super.paintComponent(g0);
            }
        };
        k.setFont(Theme.font(Font.BOLD, 12f));
        k.setForeground(Theme.TEXT);
        k.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        JPanel kw = new JPanel(new BorderLayout());
        kw.setOpaque(false);
        kw.add(k, BorderLayout.NORTH);
        p.add(kw, BorderLayout.WEST);
        p.add(UiUtil.label("<html>" + text + "</html>", Font.PLAIN, 13f, Theme.MUTED), BorderLayout.CENTER);
        return p;
    }

    public void refreshStats() {
        ramValue.setText(String.format("%.1f GB", launcher.config.ramMb / 1024.0));
        long count = 0;
        try (Stream<java.nio.file.Path> files = Files.exists(GamePaths.mods()) ? Files.list(GamePaths.mods()) : Stream.empty()) {
            count = files.filter(f -> f.getFileName().toString().endsWith(".jar")).count();
        } catch (Exception ignored) {
            // carpeta aún no creada
        }
        modsValue.setText(Long.toString(count));
    }

    private void refreshAccount() {
        Account acc = launcher.account();
        nameLabel.setText(acc.name());
        accountBody.removeAll();
        accountBody.setLayout(new BorderLayout(16, 0));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(UiUtil.left(UiUtil.label("Jugando como " + acc.name(), Font.BOLD, 14f, Theme.TEXT)));
        text.add(UiUtil.vgap(3));
        int count = launcher.accounts().size();
        text.add(UiUtil.left(UiUtil.label((acc.isMicrosoft() ? "Cuenta premium (Microsoft): servidores premium y tu skin"
                : "Cuenta sin premium: un jugador y servidores no premium")
                + (count > 1 ? " · " + count + " cuentas guardadas" : ""), Font.PLAIN, 13f,
                acc.isMicrosoft() ? Theme.SUCCESS : Theme.MUTED)));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        OButton change = new OButton("Cambiar de cuenta", OButton.Kind.SECONDARY, Icons.Type.USER);
        change.setToolTipText("Elige otra cuenta guardada o añade una nueva");
        change.addActionListener(e -> dev.orbita.launcher.ui.AccountSwitcher.show(launcher, (javax.swing.JFrame) frame, change, true));
        OButton premium = new OButton("+ Premium", OButton.Kind.GHOST);
        premium.setToolTipText("Añadir una cuenta de Microsoft (premium)");
        premium.addActionListener(e -> dev.orbita.launcher.ui.MicrosoftWebLogin.start(launcher, (javax.swing.JFrame) frame));
        OButton offline = new OButton("+ Sin premium", OButton.Kind.GHOST);
        offline.setToolTipText("Añadir una cuenta sin premium (solo un nombre)");
        offline.addActionListener(e -> dev.orbita.launcher.ui.AccountSwitcher.addOffline(launcher, (javax.swing.JFrame) frame));
        actions.add(change);
        actions.add(premium);
        actions.add(offline);
        accountBody.add(text, BorderLayout.CENTER);
        accountBody.add(actions, BorderLayout.EAST);
        accountBody.revalidate();
        accountBody.repaint();
    }
    private void play(Card consoleCard) {
        if (launcher.gameRunning()) {
            return;
        }
        Account current = launcher.account();
        if (!current.isMicrosoft() && !current.name().matches("[A-Za-z0-9_]{3,16}")) {
            error("El nombre debe tener entre 3 y 16 caracteres: letras, números o _.");
            return;
        }
        launcher.config.save();
        play.setEnabled(false);
        play.setText("PREPARANDO");
        bar.setValue(-1);

        launcher.worker.submit(() -> {
            try {
                Process game = launcher.launch(progress);
                UiUtil.ui(() -> {
                    play.setText("EN JUEGO");
                    status.setText("Minecraft está en ejecución.");
                    bar.setValue(1);
                    if (launcher.config.hideWhilePlaying) {
                        frame.setVisible(false);
                    }
                });
                int code = game.waitFor();
                launcher.reloadAccounts(); // por si cambiaste o añadiste cuentas dentro del juego
                UiUtil.ui(() -> {
                    frame.setVisible(true);
                    frame.toFront();
                    play.setEnabled(true);
                    play.setText("JUGAR");
                    bar.reset();
                    refreshStats();
                    if (code == 0) {
                        status.setForeground(Theme.MUTED);
                        status.setText("Partida terminada. ¡Hasta la próxima órbita!");
                    } else {
                        error("Minecraft se cerró con el código " + code + ". Revisa la consola.");
                        consoleCard.setVisible(true);
                        revalidate();
                    }
                });
            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                progress.log(sw.toString());
                UiUtil.ui(() -> {
                    play.setEnabled(true);
                    play.setText("JUGAR");
                    bar.reset();
                    error(e.getMessage() == null ? e.toString() : e.getMessage());
                });
            }
        });
    }

    private void error(String message) {
        status.setForeground(Theme.DANGER);
        status.setText(message);
    }
}
