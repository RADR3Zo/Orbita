package dev.orbita.client.hud.modules;

import dev.orbita.client.hud.HudModule;
import dev.orbita.client.hud.TextHudModule;
import dev.orbita.client.nova.module.PvpExtras;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Map;
import java.util.UUID;

/** Contador de manzanas doradas (traído de Nova): las tuyas y cuántas se comieron los demás jugadores. */
public final class GapCounterModule extends TextHudModule {
    public GapCounterModule() {
        super("gaps", "Manzanas doradas", "Tus manzanas doradas y las que se comen los rivales", "GAP",
                HudModule.Category.HUD, false, -1, 382, 250);
        icon(Items.GOLDEN_APPLE);
    }

    @Override
    protected String text(Minecraft mc) {
        if (mc.player == null) {
            return "Manzanas: 0";
        }
        PvpExtras.GapTracker.tick(mc);
        int g = 0, eg = 0;
        var inv = mc.player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(Items.GOLDEN_APPLE)) {
                g += s.getCount();
            } else if (s.is(Items.ENCHANTED_GOLDEN_APPLE)) {
                eg += s.getCount();
            }
        }
        StringBuilder sb = new StringBuilder("Manzanas: ").append(g);
        if (eg > 0) {
            sb.append(" (+").append(eg).append(" dios)");
        }
        // el rival que más se ha comido
        UUID me = mc.player.getUUID();
        Map.Entry<UUID, Integer> top = null;
        for (Map.Entry<UUID, Integer> e : PvpExtras.GapTracker.eaten().entrySet()) {
            if (!e.getKey().equals(me) && (top == null || e.getValue() > top.getValue())) {
                top = e;
            }
        }
        if (top != null) {
            sb.append(" · ").append(PvpExtras.GapTracker.name(top.getKey())).append(" x").append(top.getValue());
        }
        return sb.toString();
    }
}
