package dev.orbita.client.hud.lyrics;

import dev.orbita.client.config.OrbitaConfig;
import dev.orbita.client.feature.ZoomFeature;
import dev.orbita.client.ui.anim.Animation;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Letras "en 3D": cada línea se coloca en el cielo, en la dirección en la que mirabas cuando empezó, y se
 * proyecta a la pantalla con la perspectiva de la cámara (si giras, se queda donde apareció). En el modo
 * "Delante" flota frente a ti y te sigue. Las palabras aparecen una a una con un pequeño salto.
 */
public final class LyricsRenderer {
    private static final float SKY_DISTANCE = 22f;
    /** Cuánto tiempo siguen visibles (atenuándose) las líneas anteriores alrededor de la actual. */
    private static final long LINGER_MS = 3200;
    private static final int MAX_LINGERING = 3;
    /** Posiciones alrededor de la vista: giro horizontal (grados), altura y distancia, en ciclo. */
    private static final float[] AROUND_YAW = {0f, -32f, 28f, -16f, 34f, -36f, 14f, -24f};
    private static final float[] AROUND_HEIGHT = {8f, 13f, 4.5f, 11.5f, 6f, 14f, 3.5f, 10f};
    private static final float[] AROUND_DIST = {22f, 24f, 21f, 25f, 22f, 26f, 20f, 23f};

    private record Placed(LyricsClock.State state, Vec3 anchor, long shownAt, long endedAt) {
        Placed ended(long at) {
            return new Placed(state, anchor, shownAt, at);
        }
    }

    private Placed current;
    private final java.util.ArrayDeque<Placed> past = new java.util.ArrayDeque<>();

    public void render(GuiGraphics g, Minecraft mc, int width, int height) {
        LyricsClock.State st = LyricsClock.current();
        OrbitaConfig cfg = OrbitaConfig.get();
        long now = System.currentTimeMillis();
        Camera cam = mc.gameRenderer.getMainCamera();
        if (!cam.isInitialized()) {
            return;
        }
        renderNowPlaying(g, mc, cam, width, height, now);
        if ("SCATTER".equals(cfg.lyricsWhere)) {
            renderScatter(g, mc, cam, st, width, height, now);
            return;
        }

        boolean newLine = st != null && (current == null || current.state().index() != st.index()
                || !current.state().key().equals(st.key()));
        if (newLine) {
            if (current != null) {
                past.addFirst(current.ended(now));
            }
            // Al cambiar de canción, las líneas de la anterior se van.
            if (current != null && !current.state().key().equals(st.key())) {
                past.clear();
            }
            current = new Placed(st, aroundAnchor(cam, st.index()), now, 0);
        } else if (st != null) {
            current = new Placed(st, current.anchor(), current.shownAt(), 0);
        } else if (current != null) {
            past.addFirst(current.ended(now));
            current = null;
        }
        while (past.size() > MAX_LINGERING || (!past.isEmpty() && now - past.peekLast().endedAt() > LINGER_MS)) {
            past.removeLast();
        }

        LyricsStyle style = LyricsStyle.byName(cfg.lyricsStyle);
        LyricsStyle.LyricsFont fontChoice = LyricsStyle.LyricsFont.byName(cfg.lyricsFont);
        FontDescription font = (fontChoice == LyricsStyle.LyricsFont.AUTO ? style.font : fontChoice).description();
        boolean sky = "SKY".equals(cfg.lyricsWhere);

        // Las anteriores se dibujan primero (detrás), cada vez más tenues y subiendo despacio.
        java.util.Iterator<Placed> it = past.descendingIterator();
        while (it.hasNext()) {
            Placed p = it.next();
            float age = (now - p.endedAt()) / (float) LINGER_MS;
            float alpha = sky ? 0.55f * (1f - age) : Math.max(0f, 1f - age * 4f);
            if (alpha > 0.01f) {
                draw(g, mc, p, cam, width, height, sky, style, font, alpha, -age * (sky ? 10f : 18f), true);
            }
        }
        if (current != null) {
            float in = Animation.easeOutCubic((now - current.shownAt()) / 250f);
            draw(g, mc, current, cam, width, height, sky, style, font, in, 0f, false);
        }
    }

    // ------------------------------------------------------------------ modo Dispersas

    /** Una palabra suelta colocada en el mundo en el instante en que se canta. */
    private record Sprite(String word, Vec3 anchor, long bornAt, float size, float tilt, float tone) {
    }

    private static final long SPRITE_LIFE_MS = 2800;
    private static final int MAX_SPRITES = 14;
    private final java.util.ArrayList<Sprite> sprites = new java.util.ArrayList<>();
    private final java.util.HashSet<String> spawned = new java.util.HashSet<>();
    private String spawnedKey = "";

    /**
     * Cada palabra aparece justo cuando se canta, en un punto al azar de lo que estás mirando en ese instante,
     * con tamaño e inclinación variados. Queda fija en el mundo (si giras, se mueve como un objeto real) y se
     * desvanece a los pocos segundos.
     */
    private void renderScatter(GuiGraphics g, Minecraft mc, Camera cam, LyricsClock.State st, int width, int height,
                               long now) {
        OrbitaConfig cfg = OrbitaConfig.get();
        if (st != null) {
            if (!st.key().equals(spawnedKey)) {
                spawnedKey = st.key();
                spawned.clear();
                sprites.clear();
            }
            long pos = st.position();
            for (int i = 0; i < st.words().length; i++) {
                long start = st.wordStarts()[i];
                String id = st.index() + ":" + i;
                // Solo palabras que empiezan ahora (si adelantas la canción no aparece un montón de golpe).
                if (pos >= start && pos - start < 700 && spawned.add(id)) {
                    sprites.add(spawn(cam, st.words()[i], st.index() * 31L + i, now));
                }
            }
        }
        sprites.removeIf(s -> now - s.bornAt() > SPRITE_LIFE_MS);
        while (sprites.size() > MAX_SPRITES) {
            sprites.remove(0);
        }
        if (sprites.isEmpty()) {
            return;
        }
        LyricsStyle style = LyricsStyle.byName(cfg.lyricsStyle);
        LyricsStyle.LyricsFont fontChoice = LyricsStyle.LyricsFont.byName(cfg.lyricsFont);
        FontDescription font = (fontChoice == LyricsStyle.LyricsFont.AUTO ? style.font : fontChoice).description();

        // Proyección y orden de lejos a cerca, para que las cercanas queden delante.
        java.util.ArrayList<float[]> order = new java.util.ArrayList<>();
        for (int i = 0; i < sprites.size(); i++) {
            float[] s = project(mc, cam, sprites.get(i).anchor(), width, height);
            if (s != null) {
                order.add(new float[]{s[0], s[1], s[2], i});
            }
        }
        order.sort((a, b) -> Float.compare(b[2], a[2]));
        for (float[] o : order) {
            Sprite sp = sprites.get((int) o[3]);
            float age = now - sp.bornAt();
            float in = Animation.easeOutCubic(age / 160f);
            float out = age > SPRITE_LIFE_MS - 700 ? (SPRITE_LIFE_MS - age) / 700f : 1f;
            float alpha = Math.max(0f, Math.min(in, out));
            float pop = 1f + 0.18f * (1f - in);
            float scale = cfg.lyricsSize * 0.55f * sp.size() * Math.min(2.5f, 13f / Math.max(3f, o[2])) * pop;
            String word = sp.word();
            Component c = Component.literal(word).withStyle(s -> s.withFont(font));
            float w = mc.font.width(c);
            g.pose().pushMatrix();
            g.pose().translate(o[0], o[1] - (1f - in) * 6f);
            g.pose().rotate(sp.tilt());
            g.pose().scale(scale, scale);
            float t0 = sp.tone();
            drawWord(g, mc.font, word, font, -w / 2f, -mc.font.lineHeight / 2f, 1f, style, t0, Math.min(1f, t0 + 0.35f), alpha);
            g.pose().popMatrix();
        }
    }

    /** Coloca una palabra en un punto al azar del campo de visión actual, lejos de las que ya hay. */
    private Sprite spawn(Camera cam, String word, long seed, long now) {
        // SplittableRandom mezcla bien semillas consecutivas (java.util.Random daría posiciones casi iguales).
        java.util.SplittableRandom r = new java.util.SplittableRandom(seed * 0x9E3779B97F4A7C15L ^ spawnedKey.hashCode());
        Vec3 best = null;
        double bestScore = -1;
        float hFov = 44f; // mitad del ángulo horizontal donde pueden salir
        for (int attempt = 0; attempt < 8; attempt++) {
            float dYaw = (float) (r.nextDouble() * 2 - 1) * hFov;
            // Repartidas por encima y alrededor de la mira, dejando libre el centro justo.
            float dPitch = (float) (-22 + r.nextDouble() * 26);
            float yaw = (cam.yRot() + dYaw) * Mth.DEG_TO_RAD;
            float pitch = (Mth.clamp(cam.xRot(), -45f, 45f) + dPitch) * Mth.DEG_TO_RAD;
            float dist = 9f + (float) r.nextDouble() * 9f;
            Vec3 dir = new Vec3(-Mth.sin(yaw) * Mth.cos(pitch), -Mth.sin(pitch), Mth.cos(yaw) * Mth.cos(pitch));
            Vec3 p = cam.position().add(dir.scale(dist));
            // Separación medida en ángulo (lo que se ve en pantalla), no en bloques.
            double minD = Double.MAX_VALUE;
            for (Sprite s : sprites) {
                if (now - s.bornAt() < SPRITE_LIFE_MS - 400) {
                    Vec3 a = s.anchor().subtract(cam.position()).normalize();
                    minD = Math.min(minD, 1 - a.dot(dir));
                }
            }
            if (minD > bestScore) {
                bestScore = minD;
                best = p;
            }
        }
        float size = (0.6f + (float) r.nextDouble() * 0.6f) * (word.length() >= 7 ? 0.9f : word.length() <= 2 ? 0.75f : 1f);
        float tilt = (float) (r.nextDouble() * 2 - 1) * 0.2f;
        return new Sprite(word, best, now, size, tilt, (float) r.nextDouble() * 0.65f);
    }

    // ------------------------------------------------------------------ tarjeta "Sonando ahora"

    private String nowPlayingKey = "";
    private long nowPlayingAt;
    private Vec3 nowPlayingAnchor;
    private static final long NOW_PLAYING_MS = 4200;

    /** Al empezar una canción, una tarjeta flota unos segundos en el mundo con el título y el artista. */
    private void renderNowPlaying(GuiGraphics g, Minecraft mc, Camera cam, int width, int height, long now) {
        if ("HUD".equals(OrbitaConfig.get().lyricsWhere) || !OrbitaConfig.get().lyricsNowPlaying) {
            return;
        }
        dev.orbita.client.feature.SpotifyWatcher.NowPlaying np = dev.orbita.client.feature.SpotifyWatcher.current();
        if (!np.running() || np.song().isEmpty()) {
            return;
        }
        String key = np.artist() + "|" + np.song();
        if (!key.equals(nowPlayingKey)) {
            nowPlayingKey = key;
            nowPlayingAt = now;
            float yaw = cam.yRot() * Mth.DEG_TO_RAD;
            // Un poco por debajo de la mira: las palabras salen por encima y no la tapan.
            float pitch = Mth.clamp(cam.xRot() + 13f, -60f, 60f) * Mth.DEG_TO_RAD;
            nowPlayingAnchor = cam.position().add(new Vec3(-Mth.sin(yaw) * Mth.cos(pitch), -Mth.sin(pitch),
                    Mth.cos(yaw) * Mth.cos(pitch)).scale(10));
        }
        long age = now - nowPlayingAt;
        if (age > NOW_PLAYING_MS || nowPlayingAnchor == null) {
            return;
        }
        float[] s = project(mc, cam, nowPlayingAnchor, width, height);
        if (s == null) {
            return;
        }
        float in = Animation.easeOutCubic(age / 300f);
        float out = age > NOW_PLAYING_MS - 600 ? (NOW_PLAYING_MS - age) / 600f : 1f;
        float alpha = Math.min(in, out);
        float scale = Math.min(2.2f, 10f / Math.max(3f, s[2])) * 1.5f;
        LyricsStyle style = LyricsStyle.byName(OrbitaConfig.get().lyricsStyle);
        Font f = mc.font;
        Component label = dev.orbita.client.ui.theme.OrbitaFonts.menu("SONANDO AHORA");
        Component title = Component.literal(np.song()).withStyle(st -> st.withFont(dev.orbita.client.ui.theme.OrbitaFonts.MENU_BOLD));
        Component artist = dev.orbita.client.ui.theme.OrbitaFonts.menu(np.artist());
        g.pose().pushMatrix();
        g.pose().translate(s[0], s[1] + (1f - in) * 8f);
        g.pose().scale(scale, scale);
        g.drawString(f, label, -f.width(label) / 2, -16, withAlpha(0xFF9AA0B5, alpha), false);
        g.pose().pushMatrix();
        g.pose().scale(1.4f, 1.4f);
        g.drawString(f, title, -f.width(title) / 2, -4, withAlpha(lerp(style.from, style.to, 0.3f), alpha), true);
        g.pose().popMatrix();
        g.drawString(f, artist, -f.width(artist) / 2, 10, withAlpha(0xFFC8CCDA, alpha), false);
        g.pose().popMatrix();
    }

    /** Punto del cielo alrededor de la vista (alternando lados y alturas) donde se queda la línea. */
    private static Vec3 aroundAnchor(Camera cam, int lineIndex) {
        int i = Math.floorMod(lineIndex, AROUND_YAW.length);
        float yaw = (cam.yRot() + AROUND_YAW[i]) * Mth.DEG_TO_RAD;
        Vec3 forward = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
        return cam.position().add(forward.scale(AROUND_DIST[i])).add(0, AROUND_HEIGHT[i], 0);
    }

    private void draw(GuiGraphics g, Minecraft mc, Placed p, Camera cam, int width, int height, boolean sky,
                      LyricsStyle style, FontDescription font, float alpha, float rise, boolean leaving) {
        OrbitaConfig cfg = OrbitaConfig.get();
        float size = cfg.lyricsSize;
        float x;
        float y;
        float scale;
        float tilt;
        if (sky) {
            float[] s = project(mc, cam, p.anchor(), width, height);
            if (s == null) {
                return; // detrás de la cámara: la línea se quedó donde apareció
            }
            x = s[0];
            y = s[1];
            scale = size * Math.min(3f, SKY_DISTANCE / Math.max(4f, s[2]));
            // Inclinación suave según el lado de la pantalla: da sensación de perspectiva.
            tilt = -0.07f + (x - width / 2f) / Math.max(1, width) * -0.18f;
        } else {
            x = width / 2f;
            y = height * 0.30f;
            scale = size;
            tilt = -0.05f + Mth.sin((System.currentTimeMillis() % 20000L) / 20000f * Mth.TWO_PI) * 0.015f;
        }
        drawLine(g, mc.font, p.state(), x, y + rise, scale, tilt, style, font, alpha, cfg.lyricsWordByWord && !leaving);
    }

    /** Proyecta un punto del mundo a coordenadas de pantalla GUI. Devuelve {x, y, distancia} o null. */
    private static float[] project(Minecraft mc, Camera cam, Vec3 point, int width, int height) {
        Vec3 rel = point.subtract(cam.position());
        float yaw = cam.yRot() * Mth.DEG_TO_RAD;
        float pitch = cam.xRot() * Mth.DEG_TO_RAD;
        double fx = -Mth.sin(yaw) * Mth.cos(pitch);
        double fy = -Mth.sin(pitch);
        double fz = Mth.cos(yaw) * Mth.cos(pitch);
        double rx = -Mth.cos(yaw);
        double rz = -Mth.sin(yaw);
        // up = right × forward
        double ux = -rz * fy;
        double uy = rz * fx - rx * fz;
        double uz = rx * fy;
        double z = rel.x * fx + rel.y * fy + rel.z * fz;
        if (z < 0.5) {
            return null;
        }
        double xr = rel.x * rx + rel.z * rz;
        double yr = rel.x * ux + rel.y * uy + rel.z * uz;
        double fov = mc.options.fov().get() * ZoomFeature.fovMultiplier();
        double tanHalf = Math.tan(Math.toRadians(fov) / 2);
        double aspect = width / (double) height;
        float sx = (float) (width / 2.0 + (xr / z) / (tanHalf * aspect) * width / 2.0);
        float sy = (float) (height / 2.0 - (yr / z) / tanHalf * height / 2.0);
        if (sx < -width || sx > width * 2 || sy < -height || sy > height * 2) {
            return null;
        }
        return new float[]{sx, sy, (float) z};
    }

    /** Dibuja la línea centrada en (cx, cy): degradado por letra, contorno y aparición palabra a palabra. */
    private static void drawLine(GuiGraphics g, Font mcFont, LyricsClock.State st, float cx, float cy, float scale,
                                 float tilt, LyricsStyle style, FontDescription font, float alpha, boolean wordByWord) {
        String[] words = st.words();
        Component space = Component.literal(" ").withStyle(s -> s.withFont(font));
        float spaceW = mcFont.width(space);
        float total = -spaceW;
        float[] widths = new float[words.length];
        Component[] comps = new Component[words.length];
        for (int i = 0; i < words.length; i++) {
            String w = words[i];
            comps[i] = Component.literal(w).withStyle(s -> s.withFont(font));
            widths[i] = mcFont.width(comps[i]);
            total += widths[i] + spaceW;
        }
        float maxW = 420f;
        float s = total * scale > maxW ? maxW / total : scale;

        g.pose().pushMatrix();
        g.pose().translate(cx, cy);
        g.pose().rotate(tilt);
        g.pose().scale(s, s);
        float x = -total / 2f;
        int current = st.wordIndex();
        long pos = st.position();
        for (int i = 0; i < words.length; i++) {
            float a = alpha;
            float lift = 0f;
            float pop = 1f;
            if (wordByWord) {
                float t = Math.max(0f, Math.min(1f, (pos - st.wordStarts()[i]) / 180f));
                if (t <= 0f) {
                    x += widths[i] + spaceW;
                    continue;
                }
                float e = Animation.easeOutCubic(t);
                a *= e;
                lift = (1f - e) * 6f;
                pop = i == current ? 1f + 0.06f * (1f - Math.min(1f, (pos - st.wordStarts()[i]) / 400f)) : 1f;
            }
            float t0 = (x + total / 2f) / Math.max(1f, total);
            float t1 = (x + widths[i] + total / 2f) / Math.max(1f, total);
            drawWord(g, mcFont, words[i], font, x, -mcFont.lineHeight / 2f - lift, pop, style, t0, t1, a);
            x += widths[i] + spaceW;
        }
        g.pose().popMatrix();
    }

    private static void drawWord(GuiGraphics g, Font mcFont, String word, FontDescription font, float x, float y,
                                 float pop, LyricsStyle style, float t0, float t1, float alpha) {
        int outline = withAlpha(style.outline, alpha * 0.85f);
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        if (pop != 1f) {
            g.pose().scale(pop, pop);
        }
        float cx = 0;
        int n = word.length();
        for (int i = 0; i < n; i++) {
            String ch = String.valueOf(word.charAt(i));
            Component c = Component.literal(ch).withStyle(s -> s.withFont(font));
            int w = mcFont.width(c);
            float t = n == 1 ? (t0 + t1) / 2 : t0 + (t1 - t0) * i / (n - 1f);
            int color = withAlpha(lerp(style.from, style.to, t), alpha);
            int ix = Math.round(cx);
            // Aura: halo suave del color del estilo alrededor de cada letra.
            int glow = withAlpha(style.to, alpha * 0.16f);
            g.drawString(mcFont, c, ix - 2, 1, glow, false);
            g.drawString(mcFont, c, ix + 2, 1, glow, false);
            g.drawString(mcFont, c, ix, -2, glow, false);
            g.drawString(mcFont, c, ix, 3, glow, false);
            g.drawString(mcFont, c, ix - 1, 0, outline, false);
            g.drawString(mcFont, c, ix + 1, 0, outline, false);
            g.drawString(mcFont, c, ix, -1, outline, false);
            g.drawString(mcFont, c, ix, 1, outline, false);
            g.drawString(mcFont, c, ix, 0, color, false);
            cx += w;
        }
        g.pose().popMatrix();
    }

    private static int lerp(int a, int b, float t) {
        float k = Math.max(0f, Math.min(1f, t));
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return 0xFF000000 | (Math.round(ar + (br - ar) * k) << 16) | (Math.round(ag + (bg - ag) * k) << 8)
                | Math.round(ab + (bb - ab) * k);
    }

    private static int withAlpha(int argb, float a) {
        int alpha = Math.round(((argb >>> 24) & 0xFF) * Math.max(0f, Math.min(1f, a)));
        return (alpha << 24) | (argb & 0x00FFFFFF);
    }
}
