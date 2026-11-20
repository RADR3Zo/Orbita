package dev.orbita.client.ui.theme;

/** Paleta de Órbita. Todos los colores son ARGB (desde 1.21.6 el canal alfa es obligatorio). */
public final class OrbitaTheme {
    public static final int SPACE_BLACK = 0xFF05060A;
    public static final int CHARCOAL = 0xFF14161D;
    public static final int PANEL = 0xB0181A22;
    public static final int PANEL_SOLID = 0xF0101219;
    public static final int CARD = 0xC01B1E29;
    public static final int HUD_BG = 0x90101219;
    public static final int BORDER = 0x30FFFFFF;
    public static final int TEXT = 0xFFE8EAF2;
    public static final int TEXT_MUTED = 0xFF8A8FA3;
    public static final int TEXT_DISABLED = 0xFF555A6B;
    public static final int ACCENT = 0xFF8B7CFF;
    public static final int ACCENT_2 = 0xFF5CAAFF;
    public static final int ACCENT_GLOW = 0x408B7CFF;
    public static final int SUCCESS = 0xFF7CFFB2;
    public static final int WARNING = 0xFFFFD27C;
    public static final int DANGER = 0xFFFF7C7C;

    private OrbitaTheme() {
    }

    public static int withAlpha(int argb, float alpha) {
        float clamped = Math.max(0f, Math.min(1f, alpha));
        int a = Math.round(((argb >>> 24) & 0xFF) * clamped);
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    public static int lerpColor(int from, int to, float t) {
        float k = Math.max(0f, Math.min(1f, t));
        int a = (int) lerp((from >>> 24) & 0xFF, (to >>> 24) & 0xFF, k);
        int r = (int) lerp((from >> 16) & 0xFF, (to >> 16) & 0xFF, k);
        int g = (int) lerp((from >> 8) & 0xFF, (to >> 8) & 0xFF, k);
        int b = (int) lerp(from & 0xFF, to & 0xFF, k);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
