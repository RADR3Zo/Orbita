package dev.orbita.client.feature;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;

/** Cuenta clics del último segundo. Se usa solo desde el hilo principal del cliente. */
public final class CpsTracker {
    private static final ArrayDeque<Long> LEFT = new ArrayDeque<>();
    private static final ArrayDeque<Long> RIGHT = new ArrayDeque<>();

    private CpsTracker() {
    }

    public static void click(int button) {
        long now = System.currentTimeMillis();
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            LEFT.addLast(now);
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            RIGHT.addLast(now);
        }
    }

    public static int left() {
        return count(LEFT);
    }

    public static int right() {
        return count(RIGHT);
    }

    private static int count(ArrayDeque<Long> clicks) {
        long cutoff = System.currentTimeMillis() - 1000;
        while (!clicks.isEmpty() && clicks.peekFirst() < cutoff) {
            clicks.removeFirst();
        }
        return clicks.size();
    }
}
