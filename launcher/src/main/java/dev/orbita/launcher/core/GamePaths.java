package dev.orbita.launcher.core;

import java.nio.file.Path;

/** Carpeta del juego de Órbita: %APPDATA%\.orbita (separada de .minecraft). */
public final class GamePaths {
    private GamePaths() {
    }

    public static Path root() {
        String override = System.getProperty("orbita.home");
        if (override != null && !override.isBlank()) {
            return Path.of(override);
        }
        String appData = System.getenv("APPDATA");
        Path base = appData != null ? Path.of(appData) : Path.of(System.getProperty("user.home"));
        return base.resolve(".orbita");
    }

    public static Path versions() {
        return root().resolve("versions");
    }

    public static Path libraries() {
        return root().resolve("libraries");
    }

    public static Path assets() {
        return root().resolve("assets");
    }

    public static Path mods() {
        return root().resolve("mods");
    }

    public static Path shaderpacks() {
        return root().resolve("shaderpacks");
    }

    public static Path natives() {
        return root().resolve("natives");
    }

    public static Path logs() {
        return root().resolve("logs");
    }

    public static Path launcherConfig() {
        return root().resolve("launcher.json");
    }
}
