package dev.orbita.launcher.ui;

import dev.orbita.launcher.core.Progress;
import dev.orbita.launcher.ui.components.OProgressBar;

import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Progress que agrupa las actualizaciones (pueden llegar miles por segundo desde las descargas)
 * y las aplica a la interfaz como mucho cada ~50 ms.
 *
 * <p>Las líneas del juego se guardan en un búfer circular (las últimas {@value #MAX_LINES}) y solo se
 * vuelcan a la consola cuando esta se ve: con el launcher oculto mientras juegas no se hace trabajo de UI.</p>
 */
public final class UiProgress implements Progress {
    private static final int MAX_LINES = 1500;

    private final JLabel status;
    private final OProgressBar bar;
    private final JTextArea console;
    private final AtomicReference<String> pendingStatus = new AtomicReference<>();
    private final AtomicReference<Double> pendingProgress = new AtomicReference<>();
    private final AtomicBoolean scheduled = new AtomicBoolean();
    private final ArrayDeque<String> lines = new ArrayDeque<>();
    private final Timer flush;
    private boolean dirty;

    public UiProgress(JLabel status, OProgressBar bar, JTextArea console) {
        this.status = status;
        this.bar = bar;
        this.console = console;
        flush = new Timer(250, e -> flushLog());
        flush.setCoalesce(true);
        flush.start();
    }

    @Override
    public void status(String text) {
        pendingStatus.set(text);
        schedule();
    }

    @Override
    public void progress(double fraction) {
        pendingProgress.set(fraction);
        schedule();
    }

    @Override
    public void log(String line) {
        synchronized (lines) {
            lines.addLast(line);
            if (lines.size() > MAX_LINES) {
                lines.removeFirst();
            }
            dirty = true;
        }
    }

    private void schedule() {
        if (scheduled.compareAndSet(false, true)) {
            Timer t = new Timer(50, e -> {
                scheduled.set(false);
                String s = pendingStatus.getAndSet(null);
                if (s != null) {
                    status.setForeground(Theme.MUTED);
                    status.setText(s);
                }
                Double p = pendingProgress.getAndSet(null);
                if (p != null) {
                    bar.setValue(p);
                }
            });
            t.setRepeats(false);
            SwingUtilities.invokeLater(t::start);
        }
    }

    private void flushLog() {
        if (!console.isShowing()) {
            return;
        }
        String text;
        synchronized (lines) {
            if (!dirty) {
                return;
            }
            dirty = false;
            text = String.join("\n", lines);
        }
        console.setText(text);
        console.setCaretPosition(console.getDocument().getLength());
    }
}
