package dev.orbita.client.nova.core;

import dev.orbita.client.ui.theme.OrbitaTheme;

/**
 * Color de acento de las partes que vienen de Nova (rueda de emotes, avisos...).
 * Usa el mismo acento que el resto de Órbita ({@link OrbitaTheme#ACCENT}) para que todo combine.
 */
public final class Theme {
    private static final int RGB = OrbitaTheme.ACCENT & 0xFFFFFF;
    /** Versión clara del acento, para texto o bordes resaltados. */
    private static final int LIGHT = OrbitaTheme.lerpColor(OrbitaTheme.ACCENT, 0xFFFFFFFF, 0.45f) & 0xFFFFFF;

    private Theme() { }

    public static int rgb() { return RGB; }
    public static int accent() { return 0xFF000000 | RGB; }
    public static int light() { return 0xFF000000 | LIGHT; }
    /** Acento con alpha 0..255. */
    public static int accent(int alpha) { return (clamp(alpha) << 24) | RGB; }
    public static int lightA(int alpha) { return (clamp(alpha) << 24) | LIGHT; }
    /** Texto sobre el color de acento. */
    public static int onAccent() { return 0xFFFFFFFF; }

    private static int clamp(int a) { return Math.max(0, Math.min(255, a)); }
}
