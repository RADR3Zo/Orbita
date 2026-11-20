package dev.orbita.client.hud.modules;

import dev.orbita.client.hud.HudModule;
import dev.orbita.client.ui.theme.OrbitaTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;

public final class CoordinatesModule extends HudModule {
    private static final String[] LABELS = {"X: ", "Y: ", "Z: "};
    private final String[] values = new String[]{"", "", ""};
    private String facing = "";
    private int lastX = Integer.MIN_VALUE, lastY, lastZ;
    private Direction lastDir;

    public CoordinatesModule() {
        super("coords", "Coordenadas", "Posición XYZ y dirección", "XYZ", Category.HUD, true, 4, 70);
        icon(net.minecraft.world.item.Items.FILLED_MAP);
    }

    @Override
    public void render(GuiGraphics g, Minecraft mc) {
        int bx = mc.player.getBlockX();
        int by = mc.player.getBlockY();
        int bz = mc.player.getBlockZ();
        Direction dir = mc.player.getDirection();
        if (bx != lastX || by != lastY || bz != lastZ || dir != lastDir) {
            lastX = bx;
            lastY = by;
            lastZ = bz;
            lastDir = dir;
            values[0] = Integer.toString(bx);
            values[1] = Integer.toString(by);
            values[2] = Integer.toString(bz);
            facing = switch (dir) {
                case NORTH -> "Norte (-Z)";
                case SOUTH -> "Sur (+Z)";
                case EAST -> "Este (+X)";
                case WEST -> "Oeste (-X)";
                default -> "";
            };
        }
        int textW = mc.font.width(facing);
        for (int i = 0; i < 3; i++) {
            textW = Math.max(textW, mc.font.width(LABELS[i] + values[i]));
        }
        int pad = background() ? 5 : 0;
        width = Math.max(textW + pad * 2, background() ? 56 : 0);
        height = 4 * 10 + pad * 2 - 1;
        if (background()) {
            g.fill(0, 0, width, height, 0x6F000000);
        }
        int color = textColor();
        for (int i = 0; i < 3; i++) {
            int ly = pad + i * 10;
            g.drawString(mc.font, LABELS[i], pad, ly, OrbitaTheme.withAlpha(color, 0.65f), shadow());
            g.drawString(mc.font, values[i], pad + mc.font.width(LABELS[i]), ly, color, shadow());
        }
        g.drawString(mc.font, facing, pad, pad + 30, OrbitaTheme.withAlpha(color, 0.65f), shadow());
    }
}
