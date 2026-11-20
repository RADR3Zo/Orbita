package dev.orbita.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Versiones fijadas en tiempo de compilación (ver gradle.properties). */
public final class LauncherInfo {
    public static final String NAME = "Órbita";
    public static final String VERSION;
    public static final String MINECRAFT;
    public static final String FABRIC_LOADER;

    static {
        Properties p = new Properties();
        try (InputStream in = LauncherInfo.class.getResourceAsStream("/orbita-launcher.properties")) {
            if (in != null) {
                p.load(in);
            }
        } catch (IOException ignored) {
            // valores por defecto
        }
        VERSION = p.getProperty("version", "dev");
        MINECRAFT = p.getProperty("minecraft", "1.21.8");
        FABRIC_LOADER = p.getProperty("loader", "0.19.5");
    }

    private LauncherInfo() {
    }
}
