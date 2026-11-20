package dev.orbita.client.nova.core;

/** Color de acento global de la interfaz (como el "Tema de color" de la referencia). */
public final class Theme {
    public static final String[] NAMES = {"Purple", "Blue", "Cyan", "Green", "White", "Pink"};
    private static final int[] RGB = {0x8B5CF6, 0x3B82F6, 0x22D3EE, 0x34D399, 0xF0F0FA, 0xF472B6};
    private static final int[] LIGHT = {0xC4B5FD, 0x93C5FD, 0xA5F3FC, 0x86EFAC, 0xFFFFFF, 0xFBCFE8};

    public static int index = 0;

    private Theme() { }

    public static int rgb() { return RGB[index]; }
    public static int accent() { return 0xFF000000 | RGB[index]; }
    public static int light() { return 0xFF000000 | LIGHT[index]; }
    /** Acento con alpha 0..255. */
    public static int accent(int alpha) { return (clamp(alpha) << 24) | RGB[index]; }
    public static int lightA(int alpha) { return (clamp(alpha) << 24) | LIGHT[index]; }
    /** Texto sobre el color de acento (oscuro si el tema es blanco). */
    public static int onAccent() { return index == 4 ? 0xFF111118 : 0xFFFFFFFF; }

    private static int clamp(int a) { return Math.max(0, Math.min(255, a)); }
}
