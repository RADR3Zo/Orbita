package dev.orbita.client.ui.anim;

/** Valor que se acerca a un objetivo con suavizado exponencial, independiente de los FPS. */
public final class Animation {
    private float value;
    private float target;
    private final float speed;
    private long lastNanos = System.nanoTime();

    public Animation(float initial, float speed) {
        this.value = initial;
        this.target = initial;
        this.speed = speed;
    }

    public void setTarget(float target) {
        this.target = target;
    }

    public void snap(float value) {
        this.value = value;
        this.target = value;
    }

    public float update() {
        long now = System.nanoTime();
        float dt = Math.min((now - lastNanos) / 1_000_000_000f, 0.1f);
        lastNanos = now;
        value += (target - value) * (1f - (float) Math.exp(-speed * dt));
        if (Math.abs(target - value) < 0.001f) {
            value = target;
        }
        return value;
    }

    public float value() {
        return value;
    }

    private static final long START_NANOS = System.nanoTime();

    /**
     * Segundos desde que arrancó el juego. Las animaciones deben usar esto y no System.nanoTime() a secas:
     * nanoTime cuenta desde que se encendió Windows, y con el PC encendido varios días ese número es tan
     * grande que en float pierde precisión (animaciones a saltos) y los senos de Mth se desbordan (se congelan).
     */
    public static float seconds() {
        return (System.nanoTime() - START_NANOS) / 1_000_000_000f;
    }

    public static float easeOutCubic(float t) {
        float f = 1f - Math.max(0f, Math.min(1f, t));
        return 1f - f * f * f;
    }
}
