package dev.orbita.client.nova.module;

import dev.orbita.client.nova.core.Config;
import dev.orbita.client.nova.core.Setting;
import dev.orbita.client.nova.module.cosmetic.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import java.util.ArrayList;
import java.util.List;

public final class ModuleManager {
    public final List<Module> all = new ArrayList<>();
    private final Config config = new Config(FabricLoader.getInstance().getConfigDir().resolve("orbita-nova.properties"));

    public ModuleManager() {
        // Solo lo que se trajo de Nova a Órbita (el resto del código de Nova no se usa).
        // ---- Cosméticos y emotes ----
        all.add(new Wings());
        all.add(new Cape());
        all.add(new Halo());
        all.add(new Crown());
        all.add(new Hat());
        all.add(new Bandana());
        all.add(new Pet());
        all.add(new Coins());
        all.add(new Trail());
        all.add(new AuraRings());
        all.add(new GroundSigil());
        all.add(new Emotes());
        // ---- Cielos y video de fondo ----
        all.add(new SkyModule());
        all.add(new VideoBackground());
        // ---- Modo ahorro y FPS ----
        all.add(new UnlockFps());
        all.add(new FpsBoost());
        all.add(new SmartFps());
        all.add(new LiteMode());
        all.add(new Visual.NoParticles());
        all.add(new Visual.NoClouds());
        all.add(new Visual.NoWeather());
        // ---- Extras visuales de PvP (sin el recorrido de perlas) ----
        all.add(new PvpExtras.CritEffects());
        all.add(new PvpExtras.Nametags());
        all.add(new PvpExtras.NameChanger());
        // ---- Ajustes (modo ahorro) ----
        all.add(new ClientSettings());
        load();
        for (Module m : all) if (m.alwaysOn) m.enabled = true;
        get(ClientSettings.class).apply();
    }

    @SuppressWarnings("unchecked")
    private final java.util.Map<Class<?>, Module> byClass = new java.util.HashMap<>();

    /** Busca un modulo por su clase (se guarda en memoria: se llama muchas veces por cuadro). */
    public <T extends Module> T get(Class<T> c) {
        Module r = byClass.get(c);
        if (r == null && !byClass.containsKey(c)) {
            for (Module m : all) if (c.isInstance(m)) { r = m; break; }
            byClass.put(c, r);
        }
        return (T) r;
    }

    public List<Module> in(Category c) {
        List<Module> l = new ArrayList<>();
        if (c == Category.ACTIVE) { // "Activos": lo que esta prendido (como la lista de modulos que habia en el HUD)
            for (Module m : all) if (m.enabled && !m.alwaysOn && m.category != Category.HUD && m.category != Category.SETTINGS && !(m instanceof Emotes)) l.add(m);
            return l;
        }
        for (Module m : all) if (m.category == c) l.add(m);
        return l;
    }

    private static String key(Module m, String what) { return (m.name + "." + what).replace(' ', '_'); }

    private void load() {
        for (Module m : all) {
            String e = config.get(key(m, "enabled"));
            for (Setting s : m.settings) {
                String v = config.get(key(m, s.name));
                if (v != null) s.load(v);
            }
            if (e != null) m.enabled = Boolean.parseBoolean(e); // onEnable se aplica en el primer tick
        }
    }

    public void save() {
        for (Module m : all) {
            config.put(key(m, "enabled"), Boolean.toString(m.enabled));
            for (Setting s : m.settings) config.put(key(m, s.name), s.save());
        }
        config.write();
    }

    private boolean started;

    public void tick() {
        if (!started) { // aplica onEnable de los modulos que estaban activos al guardar
            started = true;
            for (Module m : all) if (m.enabled) m.onEnable();
        }
        boolean eco = ClientSettings.eco(), extra = ClientSettings.extra();
        for (Module m : all) if (m.enabled && !(eco && m.ecoSkip()) && !(extra && m.extraSkip())) m.onTick();
    }

    public void hud(GuiGraphics ctx) {
        for (Module m : all) if (m.enabled) m.onHud(ctx);
    }

    public void worldBefore(dev.orbita.client.nova.gfx.WCtx c) {
        boolean eco = ClientSettings.eco(), extra = ClientSettings.extra();
        for (Module m : all) if (m.enabled && !(eco && m.ecoSkip()) && !(extra && m.extraSkip())) m.onWorldBefore(c);
    }

    public void worldAfter(dev.orbita.client.nova.gfx.WCtx c) {
        boolean eco = ClientSettings.eco(), extra = ClientSettings.extra();
        for (Module m : all) if (m.enabled && !(eco && m.ecoSkip()) && !(extra && m.extraSkip())) m.onWorldAfter(c);
    }
}
