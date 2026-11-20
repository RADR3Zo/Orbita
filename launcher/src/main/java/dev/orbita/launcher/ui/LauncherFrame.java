package dev.orbita.launcher.ui;

import dev.orbita.launcher.Launcher;
import dev.orbita.launcher.ui.components.OButton;
import dev.orbita.launcher.ui.pages.ModsPage;
import dev.orbita.launcher.ui.pages.PlayPage;
import dev.orbita.launcher.ui.pages.SettingsPage;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** Ventana principal sin bordes del sistema: barra lateral + páginas con transición. */
public final class LauncherFrame extends JFrame {
    private static final String[] PAGES = {"play", "accounts", "mods", "perf", "settings"};

    public LauncherFrame(Launcher launcher) {
        super("Órbita");
        setUndecorated(true);
        setIconImages(Icons.appIcons());
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(980, 620));
        setSize(1140, 710);
        setLocationRelativeTo(null);

        Appearance.applyAccent(launcher.config);
        StarfieldPanel root = new StarfieldPanel();
        root.setLayout(new BorderLayout());
        root.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255, 22)));
        setContentPane(root);
        appearance = new Appearance(this, root, launcher.config);

        PageContainer pages = new PageContainer();
        pages.addPage(PAGES[0], new PlayPage(launcher, this));
        pages.addPage(PAGES[1], new dev.orbita.launcher.ui.pages.AccountsPage(launcher, this));
        pages.addPage(PAGES[2], new ModsPage(launcher, this));
        pages.addPage(PAGES[3], new dev.orbita.launcher.ui.pages.PerfPage(launcher, this));
        pages.addPage(PAGES[4], new SettingsPage(launcher, this));

        Sidebar sidebar = new Sidebar(launcher, index -> pages.show(PAGES[index]));
        launcher.navigate = name -> {
            int i = java.util.Arrays.asList(PAGES).indexOf(name);
            if (i >= 0) {
                sidebar.select(i);
                pages.show(name);
            }
        };
        installDrag(sidebar.dragHandle());

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.add(titleBar(), BorderLayout.NORTH);
        center.add(pages, BorderLayout.CENTER);

        root.add(sidebar, BorderLayout.WEST);
        root.add(center, BorderLayout.CENTER);
        this.introEnabled = launcher.config.introAnimation;
    }

    private final Appearance appearance;
    private final boolean introEnabled;

    public Appearance appearance() {
        return appearance;
    }

    /** Muestra la ventana con el tema aplicado y, si está activada, la animación de entrada. */
    public void open() {
        setVisible(true);
        appearance.apply();
        if (introEnabled) {
            IntroOverlay.play(this);
        }
    }

    private JComponent titleBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 6));
        bar.setOpaque(false);
        OButton min = new OButton("", OButton.Kind.GHOST, Icons.Type.MINUS).arc(8);
        min.setToolTipText("Minimizar");
        min.addActionListener(e -> setState(ICONIFIED));
        OButton close = new OButton("", OButton.Kind.GHOST, Icons.Type.CLOSE).arc(8);
        close.setToolTipText("Cerrar");
        close.addActionListener(e -> dispatchEvent(new java.awt.event.WindowEvent(this, java.awt.event.WindowEvent.WINDOW_CLOSING)));
        bar.add(min);
        bar.add(close);
        installDrag(bar);
        return bar;
    }

    /** Permite mover la ventana arrastrando y maximizarla con doble clic. */
    private void installDrag(JComponent handle) {
        MouseAdapter ma = new MouseAdapter() {
            private Point offset;

            @Override
            public void mousePressed(MouseEvent e) {
                offset = e.getPoint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (offset == null || (getExtendedState() & MAXIMIZED_BOTH) != 0) {
                    return;
                }
                Point p = e.getLocationOnScreen();
                Point origin = javax.swing.SwingUtilities.convertPoint(handle, offset, LauncherFrame.this);
                setLocation(p.x - origin.x, p.y - origin.y);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    setExtendedState((getExtendedState() & MAXIMIZED_BOTH) != 0 ? NORMAL : MAXIMIZED_BOTH);
                }
            }
        };
        handle.addMouseListener(ma);
        handle.addMouseMotionListener(ma);
    }
}
