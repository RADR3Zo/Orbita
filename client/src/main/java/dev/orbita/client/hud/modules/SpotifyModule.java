package dev.orbita.client.hud.modules;

import dev.orbita.client.config.OrbitaConfig;
import dev.orbita.client.feature.CoverArt;
import dev.orbita.client.feature.SpotifyWatcher;
import dev.orbita.client.hud.HudModule;
import dev.orbita.client.ui.render.UiDraw;
import dev.orbita.client.ui.theme.OrbitaFonts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Items;

/** "Escuchando ahora" de Spotify: portada de la canción, título, artista y un ecualizador animado. */
public final class SpotifyModule extends HudModule {
    private static final int W = 176;
    private static final int H = 32;
    private static final int COVER = 24;
    private static final int GREEN = 0xFF1ED760;

    private String lastSong = "";
    private String lastArtist = "";
    private Component songText = Component.empty();
    private Component artistText = Component.empty();

    public SpotifyModule() {
        super("spotify", "Spotify", "Canción que suena ahora en Spotify (Windows)", "♪", Category.HUD, true, 4, 212);
        icon(Items.MUSIC_DISC_CAT);
    }

    @Override
    protected void onToggle(boolean enabled) {
        SpotifyWatcher.setEnabled(enabled);
    }

    @Override
    public boolean hasTextStyle() {
        return false;
    }

    @Override
    public boolean hasContent(Minecraft mc) {
        return SpotifyWatcher.current().running();
    }

    @Override
    public void renderPlaceholder(GuiGraphics g, Minecraft mc) {
        draw(g, mc, "", "", "Abre Spotify", "y reproduce una canción", false);
    }

    @Override
    public void render(GuiGraphics g, Minecraft mc) {
        SpotifyWatcher.NowPlaying np = SpotifyWatcher.current();
        if (np.song().isEmpty()) {
            draw(g, mc, "", "", "Spotify", "En pausa", false);
        } else {
            draw(g, mc, np.artist(), np.song(), np.song(),
                    np.playing() ? np.artist() : "En pausa · " + np.artist(), np.playing());
        }
    }

    /** Estilo píldora: portada pequeña, línea de la letra (palabra actual resaltada) o canción, y ecualizador. */
    private void drawPill(GuiGraphics g, Minecraft mc, String artist, String song, String fallback, boolean playing) {
        Font font = mc.font;
        dev.orbita.client.hud.lyrics.LyricsClock.State st = dev.orbita.client.hud.lyrics.LyricsClock.current();
        int pillW = 230;
        int pillH = 16;
        width = pillW;
        height = pillH;
        UiDraw.roundedRect(g, 0, 0, pillW, pillH, 8, background() ? 0xB0000000 : 0x60000000);
        Identifier cover = song.isEmpty() ? null : CoverArt.get(artist, song);
        if (cover == null && !song.isEmpty() && OrbitaConfig.get().spotifyCovers) {
            CoverArt.request(artist, song);
        }
        if (cover != null) {
            g.blit(RenderPipelines.GUI_TEXTURED, cover, 3, 2, 0f, 0f, 12, 12, CoverArt.SIZE, CoverArt.SIZE, CoverArt.SIZE, CoverArt.SIZE);
        } else {
            UiDraw.circle(g, 9, 8, 6, 0xFF1DB954);
        }
        int textX = 20;
        if (!song.isEmpty() && SpotifyWatcher.isSynced()) {
            // Tiempo transcurrido de la canción, como "1:07".
            long sec = Math.max(0, SpotifyWatcher.positionMs()) / 1000;
            String time = (sec / 60) + ":" + String.format(java.util.Locale.ROOT, "%02d", sec % 60);
            g.drawString(font, time, textX, 4, 0xFF9AA0B5, false);
            textX += font.width(time) + 6;
        }
        int textW = pillW - textX - 20;
        g.enableScissor(textX, 0, textX + textW, pillH);
        if (st != null) {
            // Palabras ya cantadas en blanco, la actual en verde, las siguientes atenuadas.
            int current = st.wordIndex();
            float x = textX;
            float total = font.width(st.text());
            float scale = total > textW ? Math.max(0.6f, textW / total) : 1f;
            g.pose().pushMatrix();
            g.pose().translate(textX, 4);
            g.pose().scale(scale, scale);
            float cx = 0;
            for (int i = 0; i < st.words().length; i++) {
                String w = st.words()[i] + " ";
                int color = i < current ? 0xFFFFFFFF : i == current ? GREEN : 0xFF7A8094;
                g.drawString(font, w, Math.round(cx), 0, color, false);
                cx += font.width(w);
            }
            g.pose().popMatrix();
        } else {
            marquee(g, font, OrbitaFonts.menu(fallback), textX, 4, textW, playing ? 0xFFFFFFFF : 0xFF8A8FA3);
        }
        g.disableScissor();
        float time = (System.currentTimeMillis() % 100_000L) / 1000f;
        int bx = pillW - 15;
        for (int i = 0; i < 3; i++) {
            float h = playing ? 2 + 6 * (0.5f + 0.5f * Mth.sin(time * (7f + i * 2.3f) + i * 1.7f)) : 2;
            g.fill(bx + i * 3, Math.round(12 - h), bx + i * 3 + 2, 12, playing ? GREEN : 0xFF5F6478);
        }
    }

    private void draw(GuiGraphics g, Minecraft mc, String artist, String song, String title, String subtitle, boolean playing) {
        if (OrbitaConfig.get().spotifyPill) {
            drawPill(g, mc, artist, song, song.isEmpty() ? title + " · " + subtitle : song + " · " + artist, playing);
            return;
        }
        if (!title.equals(lastSong)) {
            lastSong = title;
            songText = OrbitaFonts.bold(title);
        }
        if (!subtitle.equals(lastArtist)) {
            lastArtist = subtitle;
            artistText = OrbitaFonts.menu(subtitle);
        }
        width = W;
        height = H;
        if (background()) {
            UiDraw.roundedRect(g, 0, 0, W, H, 5, 0x8C000000);
        }

        // Portada (o un cuadro verde con una nota si aún no hay)
        int cx = 4;
        int cy = (H - COVER) / 2;
        Identifier cover = song.isEmpty() ? null : CoverArt.get(artist, song);
        if (cover == null && !song.isEmpty() && OrbitaConfig.get().spotifyCovers) {
            CoverArt.request(artist, song);
        }
        UiDraw.roundedRect(g, cx - 1, cy - 1, COVER + 2, COVER + 2, 3, 0x40FFFFFF);
        if (cover != null) {
            g.blit(RenderPipelines.GUI_TEXTURED, cover, cx, cy, 0f, 0f, COVER, COVER,
                    CoverArt.SIZE, CoverArt.SIZE, CoverArt.SIZE, CoverArt.SIZE);
            if (!playing) {
                g.fill(cx, cy, cx + COVER, cy + COVER, 0x80000000);
            }
        } else {
            UiDraw.roundedRect(g, cx, cy, COVER, COVER, 3, playing ? 0xFF1DB954 : 0xFF2B2F3D);
            g.drawString(mc.font, "♪", cx + (COVER - mc.font.width("♪")) / 2, cy + 8, 0xFFFFFFFF, false);
        }
        if (!playing && cover != null) {
            // Símbolo de pausa sobre la portada
            g.fill(cx + 8, cy + 7, cx + 11, cy + 17, 0xFFFFFFFF);
            g.fill(cx + 13, cy + 7, cx + 16, cy + 17, 0xFFFFFFFF);
        }

        Font font = mc.font;
        int textX = cx + COVER + 7;
        int textW = W - textX - 20;
        marquee(g, font, songText, textX, 6, textW, 0xFFFFFFFF);
        marquee(g, font, artistText, textX, 18, textW, playing ? 0xFFB3B8C9 : 0xFF7A8094);

        // Ecualizador
        float time = (System.currentTimeMillis() % 100_000L) / 1000f;
        int bx = W - 15;
        for (int i = 0; i < 3; i++) {
            float h = playing ? 3 + 8 * (0.5f + 0.5f * Mth.sin(time * (7f + i * 2.3f) + i * 1.7f)) : 2;
            int top = Math.round(23 - h);
            g.fill(bx + i * 4, top, bx + i * 4 + 2, 23, playing ? GREEN : 0xFF5F6478);
        }
    }

    /** Texto que se desplaza suavemente si no cabe en el ancho disponible. */
    private static void marquee(GuiGraphics g, Font font, Component text, int x, int y, int w, int color) {
        int tw = font.width(text);
        if (tw <= w) {
            g.drawString(font, text, x, y, color, false);
            return;
        }
        float cycle = (tw - w) + 60f;
        float t = ((System.currentTimeMillis() - SpotifyWatcher.changedAt()) / 1000f * 22f) % (cycle * 2);
        float offset = t < cycle ? Math.max(0, t - 30) : Math.max(0, cycle * 2 - t - 30);
        offset = Math.min(offset, tw - w);
        g.enableScissor(x, y - 1, x + w, y + 10);
        g.pose().pushMatrix();
        g.pose().translate(x - offset, y);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popMatrix();
        g.disableScissor();
    }
}
