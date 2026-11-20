package dev.orbita.client.nova.module;

import dev.orbita.client.nova.NovaClient;
import dev.orbita.client.nova.core.Setting;

/** Animaciones del personaje (solo visual): quitar el paso de las piernas y elegir la velocidad del golpe del brazo. */
public final class Animations extends Module {
    public final Setting.Bool noLegs = add(new Setting.Bool("No leg walking", true));
    public final Setting.Bool everyone = add(new Setting.Bool("Also other players", false));
    public final Setting.Num swingSpeed = add(new Setting.Num("Arm swing speed", 1.0, 0.25, 3.0, 0.05));

    public Animations() { super("Animations", "Leg walking and arm swing speed (visual)", Category.VISUAL); }

    public static Animations get() {
        if (NovaClient.modules == null) return null;
        Animations a = NovaClient.modules.get(Animations.class);
        return a != null && a.enabled ? a : null;
    }
}
