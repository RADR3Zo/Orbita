package dev.orbita.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.orbita.client.OrbitaClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

/** Configuración persistente del cliente en config/orbita.json. */
public final class OrbitaConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static OrbitaConfig instance = new OrbitaConfig();

    public Map<String, ModuleState> modules = new LinkedHashMap<>();
    public boolean zoomEnabled = true;
    public boolean spotifyCovers = true;
    public int lyricsOffsetMs = 0;
    public boolean lyricsShowNext = true;
    /** SKY (en el cielo, se queda donde apareció), FRONT (delante, te sigue) o HUD (caja). */
    public String lyricsWhere = "SCATTER";
    /** Tarjeta "Sonando ahora" que flota en el mundo al empezar una canción. */
    public boolean lyricsNowPlaying = true;
    /** En 1.4.3 llegan las palabras dispersas: se ponen por defecto una vez. */
    public boolean lyricsScatterMigrated = false;
    public boolean lyricsWordByWord = true;
    public String lyricsStyle = "LAVANDA";
    public String lyricsFont = "AUTO";
    public float lyricsSize = 2.0f;
    /** Widget de Spotify en forma de píldora (con la línea de la letra) en lugar de tarjeta. */
    public boolean spotifyPill = false;
    /** En 1.4.0 las letras venían apagadas; se encienden una vez al actualizar. */
    public boolean lyricsMigrated = false;
    public boolean freelookEnabled = true;
    public boolean freelookToggle = false;
    public boolean freelookInvert = false;
    public boolean toggleSneak = false;
    public double zoomLevel = 4.0;
    public boolean zoomAnimated = true;
    public boolean zoomSmoothCamera = true;
    public boolean zoomScroll = true;
    public String cape = "NONE";

    public static final class ModuleState {
        public boolean enabled;
        public int x;
        public int y;
        public float scale = 1f;
        public boolean background = true;
        public boolean brackets;
        public boolean shadow = true;
        public int color;
        /** null = valor por defecto del módulo (para configs de versiones anteriores). */
        public Boolean icon;
    }

    public static OrbitaConfig get() {
        return instance;
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("orbita.json");
    }

    public static void load() {
        Path file = path();
        if (!Files.exists(file)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            OrbitaConfig loaded = GSON.fromJson(reader, OrbitaConfig.class);
            if (loaded != null) {
                if (loaded.modules == null) {
                    loaded.modules = new LinkedHashMap<>();
                }
                instance = loaded;
            }
        } catch (Exception e) {
            OrbitaClient.LOG.warn("No se pudo leer orbita.json; se usan valores por defecto", e);
        }
    }

    public static void save() {
        Path file = path();
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling("orbita.json.tmp");
            try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                GSON.toJson(instance, writer);
            }
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            OrbitaClient.LOG.warn("No se pudo guardar orbita.json", e);
        }
    }
}
