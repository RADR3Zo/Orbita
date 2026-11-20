package dev.orbita.client.nova.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.orbita.client.nova.NovaClient;
import dev.orbita.client.nova.module.ClientSettings;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Anota los FPS de cada partida (una muestra por segundo dentro de un mundo) y al salir los guarda en
 * config/nova-fps.json: el lanzador los lee y los muestra en "Rendimiento". Los primeros segundos (cargando) no cuentan.
 * Tambien se guarda cada minuto (la misma partida se va actualizando), asi no se pierde si el juego se cierra de golpe.
 */
public final class FpsRecorder {
    private FpsRecorder() { }

    private static final int SKIP_SECONDS = 8, KEEP = 30;
    private static final List<Integer> samples = new ArrayList<>();
    private static long lastSample, joinedAt, lastSave, backAt;
    private static boolean inWorld;
    private static int mode;

    public static void tick(Minecraft c) {
        boolean now = c.level != null && c.player != null;
        long t = System.currentTimeMillis();
        if (now && !inWorld) { inWorld = true; joinedAt = t; lastSample = t; lastSave = t; samples.clear(); }
        else if (!now && inWorld) { inWorld = false; save(); samples.clear(); }
        if (!inWorld || t - lastSample < 1000) return;
        lastSample = t;
        if (t - joinedAt < SKIP_SECONDS * 1000L) return;
        // fuera de la ventana o en pausa los FPS bajan a proposito: no cuentan (y 3 s despues de volver tampoco)
        if (!c.isWindowActive() || c.isPaused()) { backAt = t + 3000; return; }
        if (t < backAt) return;
        samples.add(c.getFps());
        mode = ClientSettings.level();
        if (t - lastSave >= 60_000) { lastSave = t; save(); }
    }

    /** Al cerrar el juego estando en un mundo. */
    public static void stop() { if (inWorld) { inWorld = false; save(); samples.clear(); } }

    private static void save() {
        if (samples.size() < 10) return; // partidas de menos de ~10 s no dicen nada
        List<Integer> s = new ArrayList<>(samples);
        Collections.sort(s);
        double sum = 0;
        for (int v : s) sum += v;
        int n1 = Math.max(1, s.size() / 100); // 1 % mas bajo: lo que se siente como tirones
        double low = 0;
        for (int i = 0; i < n1; i++) low += s.get(i);
        JsonObject o = new JsonObject();
        o.addProperty("start", joinedAt);
        o.addProperty("time", System.currentTimeMillis());
        o.addProperty("mc", FabricLoader.getInstance().getModContainer("minecraft").map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("?"));
        o.addProperty("seconds", s.size());
        o.addProperty("avg", Math.round(sum / s.size() * 10) / 10.0);
        o.addProperty("low", Math.round(low / n1 * 10) / 10.0);
        o.addProperty("min", s.get(0));
        o.addProperty("max", s.get(s.size() - 1));
        o.addProperty("median", s.get(s.size() / 2));
        o.addProperty("mode", mode); // 0 normal, 1 ahorro medio, 2 ahorro extra
        Path f = FabricLoader.getInstance().getConfigDir().resolve("nova-fps.json");
        try {
            JsonArray all = Files.exists(f) ? JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonArray() : new JsonArray();
            // la misma partida (mismo inicio) se reemplaza en vez de repetirse
            if (all.size() > 0 && all.get(all.size() - 1).getAsJsonObject().has("start")
                    && all.get(all.size() - 1).getAsJsonObject().get("start").getAsLong() == joinedAt) all.remove(all.size() - 1);
            all.add(o);
            while (all.size() > KEEP) all.remove(0);
            Gson g = new GsonBuilder().setPrettyPrinting().create();
            Files.writeString(f, g.toJson(all), StandardCharsets.UTF_8);
        } catch (Exception e) {
            NovaClient.LOG.warn("Nova: no se pudieron guardar los FPS de la partida", e);
        }
    }
}
