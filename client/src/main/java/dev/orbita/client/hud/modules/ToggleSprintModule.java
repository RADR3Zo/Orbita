package dev.orbita.client.hud.modules;

import dev.orbita.client.config.OrbitaConfig;
import dev.orbita.client.hud.TextHudModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;

/**
 * Toggle Sprint / Toggle Sneak usando las opciones nativas de Minecraft (sin trucos de input),
 * con el indicador clásico de los clientes PvP: "[Corriendo (Alternado)]".
 */
public final class ToggleSprintModule extends TextHudModule {
    private static boolean pending = true;
    private static ToggleSprintModule instance;

    public ToggleSprintModule() {
        super("togglesprint", "Toggle Sprint", "Correr (y agacharse) alternado con un toque", "SPR",
                Category.MECHANIC, true, 4, 196);
        icon(net.minecraft.world.item.Items.LEATHER_BOOTS);
        setShowIcon(false);
        setBrackets(true);
        setBackground(false);
        instance = this;
    }

    @Override
    protected void onToggle(boolean enabled) {
        pending = true;
    }

    /** Las opciones de Minecraft aún no existen al iniciar el mod: se aplican en el primer tick. */
    public static void requestApply() {
        pending = true;
    }

    public static void tick(Minecraft mc) {
        if (!pending || mc.options == null || instance == null) {
            return;
        }
        pending = false;
        boolean on = instance.isEnabled();
        mc.options.toggleSprint().set(on);
        mc.options.toggleCrouch().set(on && OrbitaConfig.get().toggleSneak);
        mc.options.save();
    }

    @Override
    public boolean hasContent(Minecraft mc) {
        return !text(mc).isEmpty();
    }

    @Override
    protected String text(Minecraft mc) {
        Options o = mc.options;
        if (o.toggleCrouch().get() && o.keyShift.isDown()) {
            return "Agachado (Alternado)";
        }
        if (o.toggleSprint().get() && o.keySprint.isDown()) {
            return "Corriendo (Alternado)";
        }
        if (mc.player != null && mc.player.isSprinting()) {
            return "Corriendo (Vanilla)";
        }
        return "";
    }
}
