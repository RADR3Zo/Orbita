package dev.orbita.client.nova.module;

import net.minecraft.sounds.SoundEvent;

/** Sonidos propios de Nova que usan otros módulos (por ejemplo, la campanita de los críticos). */
public final class Effects {
    private Effects() { }

    /** Campanita propia de Nova (assets/nova/sounds/crit_bell.ogg), aguda y corta. */
    public static final SoundEvent NOVA_BELL = SoundEvent.createVariableRangeEvent(net.minecraft.resources.Identifier.fromNamespaceAndPath("nova", "crit_bell"));
}
