package dev.orbita.client.hud.modules;

import dev.orbita.client.hud.HudModule;
import dev.orbita.client.ui.theme.OrbitaTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public final class ArmorHudModule extends HudModule {
    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND
    };
    private static final int ROW = 18;

    public ArmorHudModule() {
        super("armor", "Armadura", "Piezas equipadas y durabilidad", "ARM", Category.HUD, true, -1, -1);
        icon(net.minecraft.world.item.Items.DIAMOND_CHESTPLATE);
    }

    @Override
    public boolean hasContent(Minecraft mc) {
        for (EquipmentSlot slot : SLOTS) {
            if (!mc.player.getItemBySlot(slot).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void render(GuiGraphics g, Minecraft mc) {
        int rows = 0;
        int textW = 0;
        for (EquipmentSlot slot : SLOTS) {
            ItemStack stack = mc.player.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                rows++;
                textW = Math.max(textW, mc.font.width(label(stack)));
            }
        }
        width = 24 + textW + 6;
        height = rows * ROW + 2;
        if (background()) {
            g.fill(0, 0, width, height, 0x6F000000);
        }
        int ry = 1;
        for (EquipmentSlot slot : SLOTS) {
            ItemStack stack = mc.player.getItemBySlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            g.renderItem(stack, 3, ry + 1);
            g.drawString(mc.font, label(stack), 24, ry + 5, color(stack), shadow());
            ry += ROW;
        }
    }

    private static String label(ItemStack stack) {
        if (stack.isDamageableItem()) {
            return Integer.toString(stack.getMaxDamage() - stack.getDamageValue());
        }
        return stack.getCount() > 1 ? "x" + stack.getCount() : "";
    }

    private int color(ItemStack stack) {
        if (!stack.isDamageableItem()) {
            return textColor();
        }
        float left = 1f - stack.getDamageValue() / (float) stack.getMaxDamage();
        return left > 0.5f ? OrbitaTheme.SUCCESS : left > 0.2f ? OrbitaTheme.WARNING : OrbitaTheme.DANGER;
    }
}
