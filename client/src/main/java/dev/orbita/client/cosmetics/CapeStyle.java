package dev.orbita.client.cosmetics;

/** Capas procedurales de Órbita. Los colores son ARGB: fondo, medio y brillo. */
public enum CapeStyle {
    NONE("Ninguna", 0, 0, 0),
    NEBULA("Nebulosa", 0xFF120B2E, 0xFF5B2A86, 0xFFE76FC4),
    GALAXY("Galaxia", 0xFF050816, 0xFF1B3A7A, 0xFF7FD3FF),
    ECLIPSE("Eclipse", 0xFF040404, 0xFF1A1206, 0xFFFFB347),
    AURORA("Aurora", 0xFF041A1A, 0xFF0F6B5C, 0xFF7CFFB2),
    SUPERNOVA("Supernova", 0xFF1A0505, 0xFF8A1E1E, 0xFFFFD27C),
    VOID("Vacío", 0xFF000000, 0xFF14161D, 0xFF8B7CFF);

    public final String displayName;
    public final int dark;
    public final int mid;
    public final int bright;

    CapeStyle(String displayName, int dark, int mid, int bright) {
        this.displayName = displayName;
        this.dark = dark;
        this.mid = mid;
        this.bright = bright;
    }

    public static CapeStyle byName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException | NullPointerException e) {
            return NONE;
        }
    }
}
