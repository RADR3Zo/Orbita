package dev.orbita.client.nova.module;

import dev.orbita.client.nova.NovaClient;
import dev.orbita.client.nova.core.Lang;
import dev.orbita.client.nova.core.Setting;

/**
 * Modo ahorro de Nova (Medio / Extra).
 *
 * <p>El idioma ahora sigue al de Minecraft y el color de acento es el de Órbita; antes Nova tenía
 * sus propios ajustes de idioma, tema, fondo, menú de pausa y nombre del cliente, que en Órbita no se usaban.
 */
public final class ClientSettings extends Module {
    /** Antes era un si/no; se lee una vez para pasar al nuevo modo (oculto). */
    public final Setting.Bool eco = add(new Setting.Bool("Eco mode", false)).showIf(() -> false);
    public final Setting.Choice saving = add(new Setting.Choice("Saving mode", 0, "Disabled", "Mid", "Extra"));
    /** Opciones beta del modo ahorro (experimentales, mas FPS): resolucion del mundo al 80 %. Puede verse un poco borroso. */
    public final Setting.Bool betaBoost = add(new Setting.Bool("Beta boost (experimental)", false));
    /** Como estaban los modulos antes de prender el modo ahorro (oculto). */
    private final Setting.Pick ecoPrev = add(new Setting.Pick("EcoPrev", "", java.util.List::of)).showIf(() -> false);
    private Integer lastLevel;

    public ClientSettings() {
        super("Client Settings", "Saving mode", Category.SETTINGS);
        alwaysOn = true; enabled = true; expanded = true;
    }

    public void apply() {
        Lang.followMinecraft();
    }

    @Override public void onTick() {
        apply();
        if (lastLevel == null) { // al abrir
            if (eco.value) { eco.value = false; if (saving.index == 0) saving.index = 1; } // ajuste viejo
            if (!ecoPrev.value.isEmpty() && !ecoPrev.value.startsWith("v2")) restoreOld();
            lastLevel = ecoPrev.value.isEmpty() ? 0 : -1; // -1 = aplicado antes de cerrar, se vuelve a aplicar limpio
        }
        if (lastLevel != saving.index) {
            Notifications.mute = true;
            try { levelChanged(saving.index); } finally { Notifications.mute = false; }
            if (lastLevel != null && lastLevel >= 0) Notifications.push(Lang.t("Saving mode") + ": " + Lang.t(saving.get()), saving.index > 0 ? 0x34D399 : 0x9A98AE);
            lastLevel = saving.index;
        }
    }

    /** Que cambia el modo ahorro: modulo + ajuste (null = si esta prendido). */
    private record Lever(Module m, Setting s) { }

    private java.util.List<Lever> levers() {
        var mm = NovaClient.modules;
        FpsBoost fb = mm.get(FpsBoost.class);
        LiteMode lm = mm.get(LiteMode.class);
        Visual.NoParticles np = mm.get(Visual.NoParticles.class);
        java.util.List<Lever> l = new java.util.ArrayList<>();
        l.add(new Lever(fb, fb.preset)); l.add(new Lever(fb, fb.resolution)); l.add(new Lever(fb, null));
        l.add(new Lever(lm, lm.flat)); l.add(new Lever(lm, lm.fpsPack)); l.add(new Lever(lm, lm.noSky)); l.add(new Lever(lm, lm.stopAnimated)); l.add(new Lever(lm, lm.noGlint)); l.add(new Lever(lm, null));
        l.add(new Lever(np, np.all)); l.add(new Lever(np, null));
        l.add(new Lever(mm.get(Visual.NoWeather.class), null));
        l.add(new Lever(mm.get(Visual.NoClouds.class), null));
        return l;
    }

    /**
     * Modo ahorro. Medio: FPS Boost "Max FPS" + Modo ligero, y apaga el cielo propio, luciernagas, circulos, videos y particulas de Nova.
     * Extra: ademas FPS Boost "Patata", texturas planas, sin cielo, sin particulas, sin lluvia ni nubes, sin cosmeticos ni efectos.
     * Al bajar o apagar el modo, todo vuelve como estaba.
     */
    private void levelChanged(int level) {
        FpsBoost fb = NovaClient.modules.get(FpsBoost.class);
        java.util.List<Lever> ls = levers();
        if (ecoPrev.value.startsWith("v2")) { // primero deja todo como estaba
            String[] v = ecoPrev.value.substring(3).split("\u0001", -1);
            boolean fbWasOn = false;
            for (int i = 0; i < ls.size() && i < v.length; i++) {
                Lever lv = ls.get(i);
                if (lv.s != null) lv.s.load(v[i]);
                else if (lv.m == fb) { fbWasOn = Boolean.parseBoolean(v[i]); if (!fbWasOn) fb.setEnabled(false); }
                else lv.m.setEnabled(Boolean.parseBoolean(v[i]));
            }
            if (fbWasOn && fb.enabled) fb.onEnable(); // vuelve a su modo de antes
            ecoPrev.value = "";
        }
        if (level > 0) {
            StringBuilder sb = new StringBuilder("v2|");
            for (int i = 0; i < ls.size(); i++) {
                Lever lv = ls.get(i);
                if (i > 0) sb.append('\u0001');
                sb.append(lv.s != null ? lv.s.save() : String.valueOf(lv.m.enabled));
            }
            ecoPrev.value = sb.toString();
            LiteMode lm = NovaClient.modules.get(LiteMode.class);
            lm.stopAnimated.value = true; lm.noGlint.value = true;
            if (level == 2) {
                lm.fpsPack.value = true; lm.noSky.value = true; // paquete Nova FPS (o texturas planas si no esta)
                Visual.NoParticles np = NovaClient.modules.get(Visual.NoParticles.class);
                np.all.value = true; np.setEnabled(true);
                NovaClient.modules.get(Visual.NoWeather.class).setEnabled(true);
                NovaClient.modules.get(Visual.NoClouds.class).setEnabled(true);
            }
            fb.preset.index = level == 2 ? 2 : Math.max(1, fb.preset.index);
            if (fb.enabled) fb.onEnable(); else fb.setEnabled(true);
            lm.setEnabled(true);
        }
        NovaClient.modules.save();
    }

    /** Formato viejo ("fpsboost,preset,lite"): deja todo como estaba. */
    private void restoreOld() {
        FpsBoost fb = NovaClient.modules.get(FpsBoost.class);
        LiteMode lm = NovaClient.modules.get(LiteMode.class);
        String[] p = ecoPrev.value.split(",");
        try {
            if (p.length == 3) {
                fb.preset.index = Integer.parseInt(p[1]);
                if (p[0].equals("0")) fb.setEnabled(false); else fb.onEnable();
                if (p[2].equals("0")) lm.setEnabled(false);
            }
        } catch (RuntimeException ignored) { }
        ecoPrev.value = "";
    }

    /** 0 = apagado, 1 = medio, 2 = extra. */
    public static int level() {
        ClientSettings c = get();
        return c == null ? 0 : c.saving.index;
    }

    public static boolean extra() { return level() >= 2; }



    /** Opcion beta del modo ahorro: resolucion del mundo al 80 % (mas FPS en PC con tarjeta grafica). */
    public static boolean betaBoost() { ClientSettings c = get(); return c != null && c.betaBoost.value; }

    public static boolean eco() {
        if (NovaClient.modules == null) return false;
        ClientSettings c = NovaClient.modules.get(ClientSettings.class);
        return c != null && c.saving.index > 0;
    }

    public static ClientSettings get() { return NovaClient.modules == null ? null : NovaClient.modules.get(ClientSettings.class); }
}
