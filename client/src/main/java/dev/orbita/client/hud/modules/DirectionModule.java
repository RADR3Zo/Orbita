package dev.orbita.client.hud.modules;

import dev.orbita.client.hud.HudModule;
import dev.orbita.client.ui.theme.OrbitaTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/** Brújula horizontal (Direction HUD): marcas cada 15° y puntos cardinales. */
public final class DirectionModule extends HudModule {
    private static final String[] CARDINALS = {"N", "NE", "E", "SE", "S", "SO", "O", "NO"};
    private static final int W = 150;
    private static final float PX_PER_DEG = 1.1f;

    public DirectionModule() {
        super("direction", "Brújula", "Rumbo con puntos cardinales", "N", Category.HUD, false, -2, 4);
        icon(net.minecraft.world.item.Items.COMPASS);
    }

    @Override
    public int screenX(int guiWidth) {
        return x == -2 ? (guiWidth - scaledWidth()) / 2 : super.screenX(guiWidth);
    }

    @Override
    public void render(GuiGraphics g, Minecraft mc) {
        width = W;
        height = 18;
        if (background()) {
            g.fill(0, 0, width, height, 0x6F000000);
        }
        // Yaw de Minecraft: 0 = sur. Rumbo: 0 = norte, en sentido horario.
        float heading = Mth.positiveModulo(mc.player.getYRot() + 180f, 360f);
        int color = textColor();
        float half = W / 2f;
        int start = (int) Math.floor((heading - half / PX_PER_DEG) / 15f) * 15;
        for (int deg = start; deg <= heading + half / PX_PER_DEG; deg += 15) {
            float dx = (deg - heading) * PX_PER_DEG;
            if (Math.abs(dx) > half - 4) {
                continue;
            }
            int px = Math.round(half + dx);
            float alpha = 1f - Math.abs(dx) / half;
            int d = Math.floorMod(deg, 360);
            if (d % 45 == 0) {
                String label = CARDINALS[d / 45];
                boolean major = d % 90 == 0;
                int c = OrbitaTheme.withAlpha(major ? color : OrbitaTheme.TEXT_MUTED, alpha);
                g.drawString(mc.font, label, px - mc.font.width(label) / 2, 5, c, shadow());
            } else {
                g.fill(px, 7, px + 1, 11, OrbitaTheme.withAlpha(0xFFFFFFFF, alpha * 0.6f));
            }
        }
        g.fill(Math.round(half), 0, Math.round(half) + 1, 3, OrbitaTheme.ACCENT);
        g.fill(Math.round(half), height - 3, Math.round(half) + 1, height, OrbitaTheme.ACCENT);
    }
}
