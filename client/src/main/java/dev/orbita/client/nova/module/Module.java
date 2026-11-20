package dev.orbita.client.nova.module;

import dev.orbita.client.nova.core.Setting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public abstract class Module {
    public final String name, description;
    public final Category category;
    public final List<Setting> settings = new ArrayList<>();
    public boolean enabled;
    public boolean expanded; // solo para la GUI
    /** Modulos que no se apagan (p. ej. los ajustes del cliente). */
    public boolean alwaysOn;

    protected Module(String name, String description, Category category) {
        this.name = name; this.description = description; this.category = category;
    }

    protected static Minecraft mc() { return Minecraft.getInstance(); }

    protected <T extends Setting> T add(T s) { settings.add(s); return s; }

    public void setEnabled(boolean e) {
        if (e == enabled) return;
        enabled = e;
        if (e) onEnable(); else onDisable();
        Notifications.toggled(this);
    }

    public void toggle() { setEnabled(!enabled); }

    /** Texto gris que sale al lado del nombre en la lista de modulos (por ejemplo el estilo elegido), o null. */
    public String info() { return null; }

    public void onEnable() { }
    public void onDisable() { }
    public void onTick() { }
    /** true = en modo ahorro no se dibuja ni se actualiza (efectos que cuestan FPS). */
    public boolean ecoSkip() { return false; }
    /** true = en ahorro Extra tampoco funciona (cosmeticos y efectos visuales). */
    public boolean extraSkip() { return category == Category.COSMETICS || category == Category.EFFECTS && !(this instanceof Combat.Listener); }
    public void onHud(GuiGraphics ctx) { }
    /** Antes de dibujar entidades (aqui se pinta el cielo). */
    public void onWorldBefore(dev.orbita.client.nova.gfx.WCtx ctx) { }
    /** Despues de dibujar entidades (cosmeticos). */
    public void onWorldAfter(dev.orbita.client.nova.gfx.WCtx ctx) { }
}
