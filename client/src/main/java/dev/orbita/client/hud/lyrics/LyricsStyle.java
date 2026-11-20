package dev.orbita.client.hud.lyrics;

import dev.orbita.client.OrbitaClient;
import net.minecraft.network.chat.FontDescription;

/** Estilos de las letras en 3D: degradado (de izquierda a derecha), contorno y fuente por defecto. */
public enum LyricsStyle {
    LAVANDA("Lavanda", 0xFFE6DCFF, 0xFFB79CFF, 0xFF3B2A6B, LyricsFont.POPPINS),
    SONADOR("Soñador", 0xFFFFC2EC, 0xFFB8DBFF, 0xFF5A3A6E, LyricsFont.SCRIPT),
    FUEGO("Fuego", 0xFFFFE08A, 0xFFFF4D3D, 0xFF4A1208, LyricsFont.MONTSERRAT),
    HIELO("Hielo", 0xFFF2FDFF, 0xFF6FD3FF, 0xFF0E3550, LyricsFont.MONTSERRAT),
    REAL("Real", 0xFFFFE6A0, 0xFFC49BFF, 0xFF3A2410, LyricsFont.CLASSIC),
    COMIC("Cómic", 0xFFFFF46B, 0xFFFFB13B, 0xFF1A1A1A, LyricsFont.COMIC),
    ORBITA("Órbita", 0xFFB3A8FF, 0xFF6FD3FF, 0xFF1A1240, LyricsFont.POPPINS),
    LIMPIO("Limpio", 0xFFFFFFFF, 0xFFFFFFFF, 0xC0000000, LyricsFont.POPPINS);

    public final String displayName;
    public final int from;
    public final int to;
    public final int outline;
    public final LyricsFont font;

    LyricsStyle(String displayName, int from, int to, int outline, LyricsFont font) {
        this.displayName = displayName;
        this.from = from;
        this.to = to;
        this.outline = outline;
        this.font = font;
    }

    public static LyricsStyle byName(String name) {
        try {
            return valueOf(name);
        } catch (Exception e) {
            return LAVANDA;
        }
    }

    /** Fuentes disponibles para las letras (todas con licencia SIL OFL). */
    public enum LyricsFont {
        AUTO("Según el estilo", null),
        POPPINS("Poppins", "lyrics_poppins"),
        MONTSERRAT("Montserrat", "lyrics_montserrat"),
        SCRIPT("Caligráfica", "lyrics_script"),
        COMIC("Cómic", "lyrics_comic"),
        CLASSIC("Clásica", "lyrics_classic"),
        RALEWAY("Raleway", "menu_bold"),
        MINECRAFT("Minecraft", null);

        public final String displayName;
        private final String id;
        private FontDescription description;

        LyricsFont(String displayName, String id) {
            this.displayName = displayName;
            this.id = id;
        }

        public FontDescription description() {
            if (description == null) {
                description = id == null ? FontDescription.DEFAULT : new FontDescription.Resource(OrbitaClient.id(id));
            }
            return description;
        }

        public static LyricsFont byName(String name) {
            try {
                return valueOf(name);
            } catch (Exception e) {
                return AUTO;
            }
        }
    }
}
