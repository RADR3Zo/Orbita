package dev.orbita.client.nova.module;

import dev.orbita.client.nova.NovaClient;
import dev.orbita.client.nova.core.Setting;
import dev.orbita.client.nova.video.VideoPlayer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.util.Util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Video de fondo en el menu principal, el menu de Nova, el inventario y otras pantallas; tambien se puede usar
 * como cielo (estilo "Video" del Sky Changer). Coloca tus .mp4 o .gif en  .minecraft/nova/videos
 */
public final class VideoBackground extends Module {
    private static final int[] WIDTHS = {480, 640, 960};

    public final Setting.Pick file = add(new Setting.Pick("Video", "Put .mp4 / .gif files in the nova/videos folder", VideoBackground::listFiles));
    public final Setting.Action openFolder = add(new Setting.Action("Videos folder", "Open", VideoBackground::openFolder));
    public final Setting.Choice quality = add(new Setting.Choice("Quality", 1, "Low", "Medium", "High"));
    public final Setting.Num opacity = add(new Setting.Num("Opacity", 0.9, 0.2, 1.0, 0.05));
    public final Setting.Num dim = add(new Setting.Num("Dim", 0.25, 0.0, 0.8, 0.05));
    public final Setting.Bool mainMenu = add(new Setting.Bool("Main menu", true));
    public final Setting.Bool novaMenu = add(new Setting.Bool("Nova menu", true));
    public final Setting.Bool inventory = add(new Setting.Bool("Inventory", true));
    /** Video propio del inventario; vacio = el mismo que el de los menus. */
    public final Setting.Pick invFile = add(new Setting.Pick("Inventory video", "Put .mp4 / .gif files in the nova/videos folder", VideoBackground::listFiles)
            .none("Same as menu")).showIf(() -> inventory.value);
    public final Setting.Bool otherMenus = add(new Setting.Bool("Other menus", false));

    private static List<String> cache = new ArrayList<>();
    private static long cacheAt;

    public VideoBackground() { super("Video Background", "Video behind menus, inventory and sky", Category.VISUAL); }

    // ------------------------------------------------------------------ archivos
    public static Path folder() {
        Path p = FabricLoader.getInstance().getGameDir().resolve("nova").resolve("videos");
        try { Files.createDirectories(p); } catch (IOException ignored) { }
        return p;
    }

    private static void openFolder() { Util.getPlatform().openFile(folder().toFile()); }

    private static List<String> listFiles() {
        long now = System.currentTimeMillis();
        if (now - cacheAt < 2000) return cache;
        cacheAt = now;
        List<String> out = new ArrayList<>();
        try (Stream<Path> s = Files.list(folder())) {
            s.map(p -> p.getFileName().toString()).filter(n -> {
                String l = n.toLowerCase(Locale.ROOT);
                return l.endsWith(".mp4") || l.endsWith(".mov") || l.endsWith(".m4v") || l.endsWith(".gif");
            }).sorted().forEach(out::add);
        } catch (IOException ignored) { }
        cache = out;
        return out;
    }

    // ------------------------------------------------------------------ ciclo de vida
    private static void sync(VideoPlayer player, String name, int width) {
        Path p = name.isEmpty() ? null : folder().resolve(name);
        if (p == null || !Files.isRegularFile(p)) { player.close(); return; }
        player.open(p, width);
    }

    private void sync() {
        int w = WIDTHS[quality.index];
        if (ClientSettings.eco()) { VideoPlayer.MENU.close(); VideoPlayer.INVENTORY.close(); return; }
        sync(VideoPlayer.MENU, file.value, w);
        // el inventario usa su propio video solo si es distinto del de los menus
        boolean own = inventory.value && !invFile.value.isEmpty() && !invFile.value.equals(file.value);
        if (own) sync(VideoPlayer.INVENTORY, invFile.value, w); else VideoPlayer.INVENTORY.close();
    }

    @Override public void onEnable() { sync(); }
    @Override public void onTick() { sync(); }
    @Override public void onDisable() { VideoPlayer.MENU.close(); VideoPlayer.INVENTORY.close(); }

    // ------------------------------------------------------------------ dibujo
    public static VideoBackground get() { return NovaClient.modules == null ? null : NovaClient.modules.get(VideoBackground.class); }

    /** Dibuja el video a pantalla completa (recortando el centro). @return false si no hay video listo. */
    private boolean draw(GuiGraphics ctx, int w, int h, VideoPlayer player, float dimMul) {
        if (!enabled || ClientSettings.eco() || !player.pump()) return false;
        int tw = player.width(), th = player.height();
        if (tw <= 0) return false;
        double sar = w / (double) h, tar = tw / (double) th;
        int rw = tw, rh = th, u = 0, v = 0;
        if (sar > tar) { rh = (int) (tw / sar); v = (th - rh) / 2; } else { rw = (int) (th * sar); u = (tw - rw) / 2; }
        int color = ((int) (255 * opacity.value) << 24) | 0xFFFFFF;
        dev.orbita.client.nova.compat.V.tex(ctx, player.TEX, 0, 0, (float) u, (float) v, w, h, rw, rh, tw, th, color);
        if (dim.value * dimMul > 0) ctx.fill(0, 0, w, h, ((int) (255 * dim.value * dimMul) << 24));
        return true;
    }

    public static boolean drawMainMenu(GuiGraphics ctx, int w, int h) {
        VideoBackground m = get();
        return m != null && m.mainMenu.value && !m.file.value.isEmpty() && m.draw(ctx, w, h, VideoPlayer.MENU, 1f);
    }

    /** En el menu de Nova el video se ve mas (menos oscurecido); el panel se vuelve de cristal. */
    public static boolean drawNovaMenu(GuiGraphics ctx, int w, int h) {
        VideoBackground m = get();
        return m != null && m.novaMenu.value && !m.file.value.isEmpty() && m.draw(ctx, w, h, VideoPlayer.MENU, 0.4f);
    }

    /** Lo llama ScreenMixin para el inventario/contenedores y el resto de pantallas de Minecraft. */
    public static void drawForScreen(Screen s, GuiGraphics ctx) {
        VideoBackground m = get();
        if (m == null || !m.enabled) return;
        boolean inv = s instanceof AbstractContainerScreen<?>;
        if (inv && m.inventory.value) {
            boolean own = !m.invFile.value.isEmpty() && !m.invFile.value.equals(m.file.value);
            if (own) m.draw(ctx, s.width, s.height, VideoPlayer.INVENTORY, 1f);
            else if (!m.file.value.isEmpty()) m.draw(ctx, s.width, s.height, VideoPlayer.MENU, 1f);
        } else if (!inv && m.otherMenus.value && !m.file.value.isEmpty()) m.draw(ctx, s.width, s.height, VideoPlayer.MENU, 1f);
    }
}
