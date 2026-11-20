package dev.orbita.launcher.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.orbita.launcher.core.auth.Account;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Ajustes del launcher en %APPDATA%\.orbita\launcher.json. */
public final class LauncherConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public String username = "Jugador";
    public int ramMb = defaultRam();
    public String jvmArgs = "";
    /** Client ID de una app de Azure con acceso a la API de Minecraft. Vacío = solo modo offline. */
    public String microsoftClientId = "";
    public boolean hideWhilePlaying = true;
    /** ZGC generacional (pausas &lt;1 ms) en lugar de G1. */
    public boolean useZgc = true;
    /** Se instala el pack de rendimiento la primera vez que se juega. */
    public boolean installPerformancePack = true;
    /** Versión del pack de rendimiento ya instalada (para añadir mods nuevos del pack una sola vez). */
    public int performancePackVersion = 0;
    /** Cuenta Microsoft guardada (formato viejo, una sola cuenta): se pasa a {@link #accounts} al abrir. */
    public Account account;
    /** Todas las cuentas guardadas (sin premium y de Microsoft). */
    public java.util.List<Account> accounts = new java.util.ArrayList<>();
    /** Índice de la cuenta con la que se juega. */
    public int activeAccount = 0;

    // ---------------------------------------------------------------- apariencia
    /** Índice de Theme.PRESETS, o -1 para el color personalizado {@link #customAccent}. */
    public int themePreset = 0;
    public int customAccent = 0x8B7CFF;
    /** UNIVERSE, COLOR, IMAGE o VIDEO. */
    public String backgroundMode = "UNIVERSE";
    /** Imagen o video de fondo (copiado a .orbita\launcher-theme). */
    public String backgroundFile = "";
    /** 0..80: cuánto se oscurece la imagen/video para que el texto se lea bien. */
    public int backgroundDim = 45;
    public boolean videoSound = false;
    public int videoVolume = 60;
    public boolean musicEnabled = false;
    public String musicFile = "";
    public int musicVolume = 40;
    /** Animación del universo al abrir el launcher. */
    public boolean introAnimation = true;

    public static LauncherConfig load() {
        Path file = GamePaths.launcherConfig();
        if (Files.exists(file)) {
            try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                LauncherConfig cfg = GSON.fromJson(r, LauncherConfig.class);
                if (cfg != null) {
                    return cfg;
                }
            } catch (Exception e) {
                System.err.println("No se pudo leer launcher.json: " + e.getMessage());
            }
        }
        return new LauncherConfig();
    }

    public synchronized void save() {
        Path file = GamePaths.launcherConfig();
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling("launcher.json.tmp");
            try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                GSON.toJson(this, w);
            }
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("No se pudo guardar launcher.json: " + e.getMessage());
        }
    }

    public static int totalMemoryMb() {
        try {
            var os = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            return (int) (os.getTotalMemorySize() / (1024 * 1024));
        } catch (Throwable t) {
            return 8192;
        }
    }

    private static int defaultRam() {
        int half = totalMemoryMb() / 2;
        return Math.max(2048, Math.min(4096, half / 512 * 512));
    }
}
