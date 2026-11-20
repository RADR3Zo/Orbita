package dev.orbita.client.nova.module;

import dev.orbita.client.nova.core.Setting;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.GraphicsPreset;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.server.level.ParticleStatus;

/**
 * Baja detalles de Minecraft para ganar FPS y los restaura al apagarlo. Los valores originales se guardan en la
 * configuracion de Nova, asi se restauran bien aunque cierres el juego con el modulo encendido.
 * (La mayor subida de FPS la da Sodium, que el lanzador instala con "Mods de FPS".)
 */
public final class FpsBoost extends Module {
    public final Setting.Choice preset = add(new Setting.Choice("Preset", 1, "Balanced", "Max FPS", "Potato"));
    /** El mundo se dibuja a esta resolucion y se estira (el HUD queda nitido). 1 = normal. */
    public final Setting.Num resolution = add(new Setting.Num("World resolution", 1.0, 0.5, 1.0, 0.05));
    /** Valores originales "opcion=valor;..." (oculto). */
    private final Setting.Pick saved = add(new Setting.Pick("Saved", "", java.util.List::of)).showIf(() -> false);

    public FpsBoost() { super("FPS Boost", "More FPS by lowering details", Category.UTILITY); }
    @Override public String info() { return dev.orbita.client.nova.core.Lang.t(preset.get()); }

    /** Una opcion de Minecraft que sabemos guardar como texto y volver a poner. */
    private record Opt<T>(OptionInstance<T> option, Function<String, T> parse) {
        String save() { T v = option.get(); return v instanceof Enum<?> e ? e.name() : String.valueOf(v); }
        void load(String s) { try { option.set(parse.apply(s)); } catch (Exception ignored) { } }
    }

    private Map<String, Opt<?>> options() {
        Options o = mc().options;
        Map<String, Opt<?>> m = new LinkedHashMap<>();
        m.put("graphics", new Opt<>(o.graphicsPreset(), GraphicsPreset::valueOf));
        m.put("clouds", new Opt<>(o.cloudStatus(), CloudStatus::valueOf));
        m.put("shadows", new Opt<>(o.entityShadows(), Boolean::parseBoolean));
        m.put("particles", new Opt<>(o.particles(), ParticleStatus::valueOf));
        m.put("biomeBlend", new Opt<>(o.biomeBlendRadius(), Integer::parseInt));
        m.put("ao", new Opt<>(o.ambientOcclusion(), Boolean::parseBoolean));
        m.put("vsync", new Opt<>(o.enableVsync(), Boolean::parseBoolean));
        m.put("maxFps", new Opt<>(o.framerateLimit(), Integer::parseInt));
        m.put("entityDistance", new Opt<>(o.entityDistanceScaling(), Double::parseDouble));
        m.put("simulation", new Opt<>(o.simulationDistance(), Integer::parseInt));
        m.put("view", new Opt<>(o.renderDistance(), Integer::parseInt));
        m.put("mipmaps", new Opt<>(o.mipmapLevels(), Integer::parseInt));
        m.put("chunkBuilder", new Opt<>(o.prioritizeChunkUpdates(), net.minecraft.client.PrioritizeChunkUpdates::valueOf));
        m.put("menuBlur", new Opt<>(o.menuBackgroundBlurriness(), Integer::parseInt));
        m.put("glint", new Opt<>(o.glintStrength(), Double::parseDouble));
        return m;
    }

    @Override public void onEnable() {
        var o = mc().options;
        int mipBefore = o.mipmapLevels().get();
        if (saved.value.isEmpty()) { // la primera vez: recuerda lo que tenia el jugador
            StringBuilder sb = new StringBuilder();
            options().forEach((k, opt) -> sb.append(k).append('=').append(opt.save()).append(';'));
            saved.value = sb.toString();
        }
        int p = preset.index; // 0 equilibrado, 1 maximo, 2 patata
        o.graphicsPreset().set(GraphicsPreset.FAST);
        o.cloudStatus().set(p == 0 ? CloudStatus.FAST : CloudStatus.OFF);
        o.entityShadows().set(false);
        o.particles().set(p == 0 ? ParticleStatus.DECREASED : ParticleStatus.MINIMAL);
        o.biomeBlendRadius().set(p == 0 ? 1 : 0);
        o.ambientOcclusion().set(p == 0);
        o.enableVsync().set(false);
        o.framerateLimit().set(260); // 260 = sin limite
        o.entityDistanceScaling().set(p == 0 ? 0.75 : 0.5);
        o.simulationDistance().set(Math.min(o.simulationDistance().get(), p == 0 ? 8 : p == 1 ? 6 : 5));
        o.renderDistance().set(Math.min(o.renderDistance().get(), p == 0 ? 12 : p == 1 ? 8 : 5));
        o.prioritizeChunkUpdates().set(net.minecraft.client.PrioritizeChunkUpdates.NONE); // los chunks se arman en hilos aparte
        o.menuBackgroundBlurriness().set(0);
        if (p >= 1) { o.mipmapLevels().set(0); o.glintStrength().set(0.0); }
        apply(mipBefore);
    }

    @Override public void onDisable() {
        int mipBefore = mc().options.mipmapLevels().get();
        Map<String, Opt<?>> opts = options();
        for (String part : saved.value.split(";")) {
            int eq = part.indexOf('=');
            if (eq <= 0) continue;
            Opt<?> opt = opts.get(part.substring(0, eq));
            if (opt != null) opt.load(part.substring(eq + 1));
        }
        saved.value = "";
        apply(mipBefore);
    }

    private int lastPreset = -1;

    @Override public void onTick() { // si cambias el modo con el modulo encendido, se aplica al momento
        if (lastPreset != preset.index) {
            boolean first = lastPreset == -1;
            lastPreset = preset.index;
            if (!first) onEnable();
        }
    }

    /** Guarda las opciones y recarga los chunks (calidad grafica y luz suave solo cambian al recargar). */
    private void apply(int mipBefore) {
        var mc = mc();
        int mip = mc.options.mipmapLevels().get();
        if (mip != mipBefore) mc.updateMaxMipLevel(mip); // texturas sin mipmaps: menos trabajo para la grafica
        mc.options.save();
        if (mc.level != null && mc.levelRenderer != null) mc.levelRenderer.allChanged();
    }
}
