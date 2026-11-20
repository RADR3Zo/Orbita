package dev.orbita.client.hud.modules;

import dev.orbita.client.config.OrbitaConfig;
import dev.orbita.client.feature.LyricsService;
import dev.orbita.client.feature.SpotifyWatcher;
import dev.orbita.client.hud.HudModule;
import dev.orbita.client.ui.anim.Animation;
import dev.orbita.client.ui.render.UiDraw;
import dev.orbita.client.ui.theme.OrbitaFonts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Letras de la canción que suena en Spotify, sincronizadas: la línea actual resaltada y la siguiente atenuada.
 * Va a juego con el widget de Spotify (fondo oscuro redondeado y acento verde).
 */
public final class LyricsModule extends HudModule {
    private static final int W = 260;
    private static final int GREEN = 0xFF1ED760;

    private String songKey = "";
    private int lastIndex = -2;
    private long indexChangedAt;
    private Component current = Component.empty();
    private Component previous = Component.empty();
    private Component next = Component.empty();

    public LyricsModule() {
        super("lyrics", "Letras", "Letra sincronizada de la canción de Spotify", "♫", Category.HUD, true, -2, 26);
        icon(Items.WRITABLE_BOOK);
    }

    @Override
    public int screenX(int guiWidth) {
        return x == -2 ? (guiWidth - scaledWidth()) / 2 : super.screenX(guiWidth);
    }

    @Override
    protected void onToggle(boolean enabled) {
        LyricsService.setEnabled(enabled);
        SpotifyWatcher.setFastPoll(enabled);
        if (enabled) {
            SpotifyWatcher.NowPlaying np = SpotifyWatcher.current();
            LyricsService.request(np.artist(), np.song());
        }
    }

    @Override
    public boolean hasTextStyle() {
        return false;
    }

    private final dev.orbita.client.hud.lyrics.LyricsRenderer world = new dev.orbita.client.hud.lyrics.LyricsRenderer();

    /** En los modos Cielo y Delante las letras se dibujan a pantalla completa (en 3D), no en una caja. */
    @Override
    public boolean freeform() {
        return !"HUD".equals(OrbitaConfig.get().lyricsWhere);
    }

    @Override
    public void renderFreeform(GuiGraphics g, Minecraft mc, int width, int height) {
        world.render(g, mc, width, height);
    }

    @Override
    public boolean hasContent(Minecraft mc) {
        SpotifyWatcher.NowPlaying np = SpotifyWatcher.current();
        if (!np.running() || np.song().isEmpty()) {
            return false;
        }
        LyricsService.State st = LyricsService.state(np.artist(), np.song());
        if (st == LyricsService.State.NONE) {
            LyricsService.request(np.artist(), np.song());
        }
        if (freeform()) {
            return true; // la tarjeta "Sonando ahora" se ve aunque la canción no tenga letra
        }
        return st == LyricsService.State.FOUND || st == LyricsService.State.LOADING;
    }

    @Override
    public void renderPlaceholder(GuiGraphics g, Minecraft mc) {
        box(g, 30);
        center(g, mc.font, OrbitaFonts.bold("Letras"), 6, 0xFFFFFFFF, 1f);
        center(g, mc.font, OrbitaFonts.menu("Pon una canción en Spotify"), 18, 0xFF8A8FA3, 1f);
    }

    @Override
    public void render(GuiGraphics g, Minecraft mc) {
        SpotifyWatcher.NowPlaying np = SpotifyWatcher.current();
        Font font = mc.font;
        LyricsService.Lyrics lyrics = LyricsService.get(np.artist(), np.song());
        if (lyrics == null) {
            box(g, 22);
            center(g, font, OrbitaFonts.menu("Buscando la letra…"), 7, 0xFF8A8FA3, 1f);
            return;
        }
        if (lyrics.instrumental()) {
            box(g, 22);
            center(g, font, OrbitaFonts.menu("♪  Instrumental  ♪"), 7, 0xFFB3B8C9, 1f);
            return;
        }
        if (!lyrics.synced()) {
            box(g, 22);
            center(g, font, OrbitaFonts.menu("Esta canción solo tiene letra sin sincronizar"), 7, 0xFF8A8FA3, 1f);
            return;
        }
        if (!SpotifyWatcher.isSynced()) {
            box(g, 30);
            center(g, font, OrbitaFonts.bold("Letra lista"), 6, 0xFFFFFFFF, 1f);
            center(g, font, OrbitaFonts.menu("Se sincroniza al empezar la próxima canción"), 18, 0xFF8A8FA3, 1f);
            return;
        }

        String key = np.artist() + "|" + np.song();
        long pos = SpotifyWatcher.positionMs() + OrbitaConfig.get().lyricsOffsetMs;
        List<LyricsService.Line> lines = lyrics.lines();
        int index = lyrics.indexAt(pos);
        if (!key.equals(songKey) || index != lastIndex) {
            previous = key.equals(songKey) ? current : Component.empty();
            songKey = key;
            lastIndex = index;
            indexChangedAt = System.currentTimeMillis();
            current = line(lines, index);
            next = line(lines, index + 1);
        }
        boolean showNext = OrbitaConfig.get().lyricsShowNext;
        int h = showNext ? 36 : 24;
        box(g, h);

        float t = Animation.easeOutCubic((System.currentTimeMillis() - indexChangedAt) / 260f);
        int slide = Math.round((1f - t) * 8f);
        if (t < 1f && previous != Component.empty()) {
            center(g, font, previous, 6 - Math.round(t * 8f), withAlpha(0xFFFFFFFF, 1f - t), 1f);
        }
        center(g, font, current, 6 + slide, withAlpha(np.playing() ? 0xFFFFFFFF : 0xFFB3B8C9, t), 1.05f);
        if (showNext) {
            center(g, font, next, 21 + slide, withAlpha(0xFF7A8094, t), 0.9f);
        }

        // Progreso dentro de la línea actual
        if (index >= 0 && index + 1 < lines.size()) {
            long start = lines.get(index).timeMs();
            long end = lines.get(index + 1).timeMs();
            float p = end > start ? Math.min(1f, (pos - start) / (float) (end - start)) : 1f;
            int barW = Math.round((W - 24) * p);
            g.fill(12, h - 3, W - 12, h - 2, 0x30FFFFFF);
            g.fill(12, h - 3, 12 + barW, h - 2, GREEN);
        }
    }

    private static Component line(List<LyricsService.Line> lines, int i) {
        if (i < 0) {
            return OrbitaFonts.menu("♪");
        }
        if (i >= lines.size()) {
            return Component.empty();
        }
        String text = lines.get(i).text();
        return text.isEmpty() ? OrbitaFonts.menu("♪") : OrbitaFonts.bold(text);
    }

    private void box(GuiGraphics g, int h) {
        width = W;
        height = h;
        if (background()) {
            UiDraw.roundedRect(g, 0, 0, W, h, 5, 0x8C000000);
            g.fill(6, 0, W - 6, 1, 0x601ED760);
        }
    }

    /** Texto centrado; si no cabe, se reduce hasta caber. */
    private void center(GuiGraphics g, Font font, Component text, int y, int color, float scale) {
        int tw = font.width(text);
        float s = Math.min(scale, (W - 16) / (float) Math.max(1, tw));
        g.pose().pushMatrix();
        g.pose().translate(W / 2f - tw * s / 2f, y);
        g.pose().scale(s, s);
        g.drawString(font, text, 0, 0, color, shadow());
        g.pose().popMatrix();
    }

    private static int withAlpha(int argb, float a) {
        int alpha = Math.round(((argb >>> 24) & 0xFF) * Math.max(0f, Math.min(1f, a)));
        return (alpha << 24) | (argb & 0x00FFFFFF);
    }
}
