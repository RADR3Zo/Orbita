package dev.orbita.launcher.ui;

import javax.swing.Timer;

/** Valor animado con suavizado exponencial; su Timer solo corre mientras hay movimiento. */
public final class Animator {
    private float value;
    private float target;
    private final float speed;
    private final Runnable onFrame;
    private final Timer timer;
    private long last;

    public Animator(float initial, float speed, Runnable onFrame) {
        this.value = initial;
        this.target = initial;
        this.speed = speed;
        this.onFrame = onFrame;
        this.timer = new Timer(15, e -> step());
        this.timer.setCoalesce(true);
    }

    public void setTarget(float t) {
        if (t == target && !timer.isRunning() && value == t) {
            return;
        }
        target = t;
        if (!timer.isRunning()) {
            last = System.nanoTime();
            timer.start();
        }
    }

    public void snap(float v) {
        value = v;
        target = v;
        timer.stop();
        onFrame.run();
    }

    public float get() {
        return value;
    }

    private void step() {
        long now = System.nanoTime();
        float dt = Math.min((now - last) / 1_000_000_000f, 0.1f);
        last = now;
        value += (target - value) * (1f - (float) Math.exp(-speed * dt));
        if (Math.abs(target - value) < 0.002f) {
            value = target;
            timer.stop();
        }
        onFrame.run();
    }
}
