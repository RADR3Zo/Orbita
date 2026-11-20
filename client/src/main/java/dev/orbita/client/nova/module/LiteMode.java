package dev.orbita.client.nova.module;

import dev.orbita.client.nova.NovaClient;
import dev.orbita.client.nova.core.Config;
import dev.orbita.client.nova.core.Setting;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Recorta animaciones y detalles para ganar FPS: texturas animadas (agua, lava, fuego), brillo de encantamientos,
 * viñeta oscura, cielo normal, y opcionalmente bloques de un solo color (texturas planas).
 */
public final class LiteMode extends Module {
    public final Setting.Bool stopAnimated = add(new Setting.Bool("Stop animated textures", true));
    public final Setting.Bool noGlint = add(new Setting.Bool("No enchant glint", true));
    public final Setting.Bool noVignette = add(new Setting.Bool("No vignette", true));
    public final Setting.Bool noSky = add(new Setting.Bool("No vanilla sky", false));
    public final Setting.Bool flat = add(new Setting.Bool("Flat block textures", false));
    /** Paquete "Nova FPS" (lo instala el lanzador): bloques de un color en 4x4, atlas de texturas mucho mas chico. */
    public final Setting.Bool fpsPack = add(new Setting.Bool("Nova FPS texture pack", false));
    public static final String PACK_ID = "file/Nova FPS.zip";

    private boolean appliedFlat;

    public LiteMode() { super("Lite Mode", "Cut animations and details for more FPS", Category.UTILITY); }

    private static LiteMode on() {
        if (NovaClient.modules == null) return null;
        LiteMode m = NovaClient.modules.get(LiteMode.class);
        return m != null && m.enabled ? m : null;
    }

    public static boolean stopAnimated() { LiteMode m = on(); return m != null && m.stopAnimated.value; }
    public static boolean noGlint() { LiteMode m = on(); return m != null && m.noGlint.value; }
    public static boolean noVignette() { LiteMode m = on(); return m != null && m.noVignette.value; }
    public static boolean noSky() { LiteMode m = on(); return m != null && m.noSky.value; }

    private static Boolean earlyFlat;

    /**
     * Las texturas se cargan al abrir el juego, antes que los modulos: en ese momento se lee el archivo de ajustes.
     */
    public static boolean flatTextures() {
        if (NovaClient.modules != null) { LiteMode m = NovaClient.modules.get(LiteMode.class); return m != null && m.enabled && m.flat.value; }
        if (earlyFlat == null) {
            Config c = new Config(FabricLoader.getInstance().getConfigDir().resolve("orbita-nova.properties"));
            earlyFlat = "true".equals(c.get("Lite_Mode.enabled")) && "true".equals(c.get("Lite_Mode.Flat_block_textures"));
        }
        return earlyFlat;
    }

    @Override public void onEnable() { appliedFlat = flatTextures(); }

    @Override public void onTick() { // si cambian las texturas planas o el paquete, recarga las texturas del juego (una sola vez)
        boolean reload = false;
        if (flat.value != appliedFlat) { appliedFlat = flat.value; reload = true; }
        if (syncPack(fpsPack.value)) reload = true;
        if (reload) mc().reloadResourcePacks();
    }

    @Override public void onDisable() {
        boolean reload = appliedFlat;
        appliedFlat = false;
        if (syncPack(false)) reload = true;
        if (reload) mc().reloadResourcePacks();
    }

    private Boolean packApplied; // null = sin aplicar aun; recordamos el estado para no recargar en cada tick (1.21.11 reportaba mal si estaba puesto -> parpadeo)

    /** Prende o apaga el paquete Nova FPS una sola vez por cambio (Minecraft recarga las texturas solo). */
    private boolean syncPack(boolean want) {
        var mc = mc();
        if (mc.getOverlay() != null) return false; // ya esta recargando
        if (packApplied != null && packApplied == want) return false; // ya esta como queremos: no tocar
        if (packApplied == null && !want) { packApplied = false; return false; } // al arrancar sin pack: no hay nada que recargar
        var mgr = mc.getResourcePackRepository();
        if (want) {
            if (!mgr.getAvailableIds().contains(PACK_ID)) { mgr.reload(); if (!mgr.getAvailableIds().contains(PACK_ID)) { fpsPack.value = false; return false; } }
            mgr.addPack(PACK_ID);
        } else mgr.removePack(PACK_ID);
        mc.options.updateResourcePacks(mgr); // guarda en options.txt y recarga las texturas
        mc.options.save();
        packApplied = want;
        return false;
    }
}
