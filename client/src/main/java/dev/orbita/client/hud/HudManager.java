package dev.orbita.client.hud;

import dev.orbita.client.config.OrbitaConfig;
import dev.orbita.client.ui.screen.HudEditorScreen;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class HudManager {
    private static final HudManager INSTANCE = new HudManager();
    private final List<HudModule> modules = new ArrayList<>();

    public static HudManager get() {
        return INSTANCE;
    }

    public void register(HudModule module) {
        modules.add(module);
    }

    public List<HudModule> modules() {
        return Collections.unmodifiableList(modules);
    }

    public void loadFromConfig() {
        OrbitaConfig cfg = OrbitaConfig.get();
        for (HudModule m : modules) {
            OrbitaConfig.ModuleState s = cfg.modules.get(m.id());
            if (s != null) {
                m.setPosition(s.x, s.y);
                m.setScale(s.scale <= 0 ? 1f : s.scale);
                m.setBackground(s.background);
                m.setBrackets(s.brackets);
                m.setShadow(s.shadow);
                m.setColorIndex(s.color);
                if (s.icon != null) {
                    m.setShowIcon(s.icon);
                }
                m.setEnabled(s.enabled);
            } else if (m.isEnabled()) {
                m.onToggle(true);
            }
        }
    }

    /** Activa una vez los módulos que en versiones anteriores venían apagados por error. */
    public void applyMigrations() {
        OrbitaConfig cfg = OrbitaConfig.get();
        if (!cfg.lyricsMigrated) {
            cfg.lyricsMigrated = true;
            for (HudModule m : modules) {
                if (m.id().equals("lyrics")) {
                    m.setEnabled(true);
                }
            }
            save();
        }
        if (!cfg.lyricsScatterMigrated) {
            cfg.lyricsScatterMigrated = true;
            cfg.lyricsWhere = "SCATTER";
            save();
        }
    }

    public void save() {
        OrbitaConfig cfg = OrbitaConfig.get();
        for (HudModule m : modules) {
            OrbitaConfig.ModuleState s = new OrbitaConfig.ModuleState();
            s.enabled = m.isEnabled();
            s.x = m.x();
            s.y = m.y();
            s.scale = m.scale();
            s.background = m.background();
            s.brackets = m.brackets();
            s.shadow = m.shadow();
            s.color = m.colorIndex();
            s.icon = m.showIcon();
            cfg.modules.put(m.id(), s);
        }
        OrbitaConfig.save();
    }

    public void render(GuiGraphics g, DeltaTracker delta) {
        if (dev.orbita.client.DevShots.enabled()) {
            dev.orbita.client.DevShots.frame();
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.debugEntries.isOverlayVisible()) {
            return;
        }
        boolean editing = mc.screen instanceof HudEditorScreen;
        int gw = g.guiWidth();
        int gh = g.guiHeight();

        for (int i = 0, n = modules.size(); i < n; i++) {
            HudModule m = modules.get(i);
            if (!m.isEnabled() || !m.hasHud()) {
                continue;
            }
            boolean content = m.hasContent(mc);
            if (m.freeform()) {
                if (content) {
                    m.renderFreeform(g, mc, gw, gh);
                }
                continue;
            }
            if (!content && !editing) {
                continue;
            }
            int x = m.screenX(gw);
            int y = m.screenY(gh);
            g.pose().pushMatrix();
            g.pose().translate(x, y);
            g.pose().scale(m.scale(), m.scale());
            if (content) {
                m.render(g, mc);
            } else {
                m.renderPlaceholder(g, mc);
            }
            g.pose().popMatrix();
            m.setLastPosition(m.screenX(gw), m.screenY(gh));
        }
    }
}
