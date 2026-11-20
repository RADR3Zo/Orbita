package dev.orbita.client.hud.modules;

import dev.orbita.client.hud.HudModule;
import dev.orbita.client.ui.theme.OrbitaTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;

import java.util.Collection;

public final class PotionHudModule extends HudModule {
    private static final String[] ROMAN = {"", "", " II", " III", " IV", " V", " VI", " VII", " VIII", " IX", " X"};
    private static final int ROW = 22;

    public PotionHudModule() {
        super("potions", "Pociones", "Efectos activos y su duración", "POC", Category.HUD, true, -1, 4);
        icon(net.minecraft.world.item.Items.POTION);
    }

    @Override
    public boolean hasContent(Minecraft mc) {
        return !mc.player.getActiveEffects().isEmpty();
    }

    @Override
    public void render(GuiGraphics g, Minecraft mc) {
        Collection<MobEffectInstance> effects = mc.player.getActiveEffects();
        float tickRate = mc.level == null ? 20f : mc.level.tickRateManager().tickrate();
        int textW = 0;
        for (MobEffectInstance e : effects) {
            textW = Math.max(textW, mc.font.width(name(e)));
            textW = Math.max(textW, mc.font.width(MobEffectUtil.formatDuration(e, 1.0f, tickRate)));
        }
        width = textW + 32;
        height = effects.size() * ROW + 2;
        if (background()) {
            g.fill(0, 0, width, height, 0x6F000000);
        }
        int color = textColor();
        int ry = 2;
        for (MobEffectInstance e : effects) {
            g.blitSprite(RenderPipelines.GUI_TEXTURED, Gui.getMobEffectSprite(e.getEffect()), 4, ry + 1, 18, 18);
            g.drawString(mc.font, name(e), 26, ry + 2, color, shadow());
            int dur = !e.isInfiniteDuration() && e.endsWithin(200) ? OrbitaTheme.WARNING : OrbitaTheme.withAlpha(color, 0.65f);
            g.drawString(mc.font, MobEffectUtil.formatDuration(e, 1.0f, tickRate), 26, ry + 11, dur, shadow());
            ry += ROW;
        }
    }

    private static String name(MobEffectInstance e) {
        int level = e.getAmplifier() + 1;
        String suffix = level < ROMAN.length ? ROMAN[level] : " " + level;
        return e.getEffect().value().getDisplayName().getString() + suffix;
    }
}
