package dev.orbita.client;

import dev.orbita.client.hud.HudManager;
import dev.orbita.client.hud.HudModule;
import dev.orbita.client.ui.screen.ControlPanelScreen;
import dev.orbita.client.ui.screen.OrbitaMainMenu;
import dev.orbita.client.ui.screen.ProfileScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Solo para desarrollo (-Dorbita.devshots=true): recorre las pantallas de Órbita, entra a un mundo de
 * prueba y guarda capturas en screenshots/orbita-*.png. No hace nada en uso normal.
 */
public final class DevShots {
    private static final boolean ENABLED = Boolean.getBoolean("orbita.devshots");

    private record Step(String shot, Consumer<Minecraft> action, Predicate<Minecraft> ready, int settle) {
    }

    private static final List<Step> STEPS = List.of(
            new Step("menu", mc -> { }, mc -> mc.screen instanceof OrbitaMainMenu, 100),
            new Step("iconos", mc -> mc.setScreen(new net.minecraft.client.gui.screens.Screen(net.minecraft.network.chat.Component.literal("iconos")) {
                private final String[] keys = {"fps", "cps", "ping", "coords", "direction", "keystrokes", "togglesprint", "potions",
                        "armor", "spotify", "lyrics", "clock", "memory", "server", "speed", "biome", "day", "combo", "reach", "session",
                        "light", "target", "gaps", "zoom", "freelook", "profile", "Wings", "Cape", "Halo", "Crown", "Hat", "Bandana",
                        "Pet", "Coins", "Trail", "AuraRings", "GroundSigil", "Emotes", "SkyModule", "VideoBackground", "CritEffects",
                        "Nametags", "NameChanger", "UnlockFps", "FpsBoost", "SmartFps", "LiteMode", "NoParticles", "NoClouds",
                        "NoWeather", "ClientSettings"};

                @Override
                public void render(net.minecraft.client.gui.GuiGraphics g, int mx, int my, float pt) {
                    g.fill(0, 0, width, height, 0xFF141625);
                    int cols = 9, cw = width / cols, chh = 52;
                    for (int i = 0; i < keys.length; i++) {
                        int x = (i % cols) * cw, y = 6 + (i / cols) * chh;
                        net.minecraft.resources.Identifier id = dev.orbita.client.ui.render.ModIcons.get(keys[i]);
                        dev.orbita.client.ui.render.UiDraw.circle(g, x + cw / 2, y + 16, 15, 0xFF2A2550);
                        if (id != null) {
                            g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, id, x + cw / 2 - 12, y + 4, 0f, 0f, 24, 24, 64, 64, 64, 64, 0xFFFFFFFF);
                        }
                        g.drawCenteredString(font, keys[i], x + cw / 2, y + 36, id != null ? 0xFFCCCCDD : 0xFFFF6666);
                    }
                }
            }), mc -> true, 20),
            new Step("cuentas", mc -> {
                dev.orbita.client.account.Accounts.add(dev.orbita.client.account.Accounts.Acc.offline("Amigo_Uno"));
                dev.orbita.client.account.Accounts.add(dev.orbita.client.account.Accounts.Acc.offline("Prueba_Dos"));
                mc.setScreen(new dev.orbita.client.ui.screen.AccountsScreen(mc.screen));
            }, mc -> true, 30),
            new Step("cuentas-cambio", mc -> {
                var list = dev.orbita.client.account.Accounts.list();
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).name().equals("Prueba_Dos")) {
                        int idx = i;
                        dev.orbita.client.account.SessionSwitcher.switchTo(idx, s -> { },
                                err -> OrbitaClient.LOG.info("DevShots cuenta: ahora {} ({})", mc.getUser().getName(), err == null ? "ok" : err));
                    }
                }
            }, mc -> true, 40),
            new Step("menu-giro", mc -> mc.setScreen(new OrbitaMainMenu()), mc -> true, 60),
            new Step("menu-zoom", mc -> {
                for (int i = 0; i < 3; i++) {
                    mc.screen.mouseScrolled(100, 100, 0, -1);
                }
            }, mc -> true, 40),
            new Step("menu-cohete", mc -> ((OrbitaMainMenu) mc.screen).devThrowRocket(), mc -> true, 14),
            new Step("perfil", mc -> mc.setScreen(new ProfileScreen(mc.screen)), mc -> true, 40),
            new Step("mods", mc -> mc.setScreen(new ControlPanelScreen(mc.screen)), mc -> true, 40),
            new Step("hud", mc -> {
                mc.setScreen(null);
                CreateWorldScreen.testWorld(mc, () -> mc.setScreen(new OrbitaMainMenu()));
            }, mc -> {
                pressCreate(mc);
                return mc.player != null && mc.screen == null && mc.level != null;
            }, 200),
            new Step("medicion", mc -> {
                frameCount = 0;
                measuring = true;
            }, mc -> true, 600),
            new Step("hud-extra", mc -> {
                report();
                Set<String> extra = Set.of("direction", "clock", "memory", "speed", "biome", "day", "server");
                for (HudModule m : HudManager.get().modules()) {
                    if (extra.contains(m.id())) {
                        m.setEnabled(true);
                    }
                }
            }, mc -> true, 40),
            new Step("freelook", mc -> dev.orbita.client.feature.FreelookFeature.devForce(mc, true, 150f), mc -> true, 30),
            new Step("editor", mc -> {
                dev.orbita.client.feature.FreelookFeature.devForce(mc, false, 0f);
                mc.setScreen(new dev.orbita.client.ui.screen.HudEditorScreen(null));
            }, mc -> true, 30),
            new Step("spotify", mc -> {
                mc.setScreen(null);
                // Letra de prueba inventada (no es la letra real de ninguna canción).
                dev.orbita.client.feature.LyricsService.put("Daft Punk", "One More Time",
                        new dev.orbita.client.feature.LyricsService.Lyrics(
                                dev.orbita.client.feature.LyricsService.parseLrc(
                                        "[00:00.50]Línea de prueba uno\n[00:02.00]Línea de prueba dos, un poco más larga\n"
                                                + "[00:03.50]Línea de prueba tres\n[00:05.00]Cuarta línea de prueba\n"
                                                + "[00:06.50]Quinta línea\n[00:08.00]Sexta línea de prueba"),
                                true, false));
                dev.orbita.client.config.OrbitaConfig.get().spotifyPill = true;
                HudManager.get().modules().stream().filter(m -> m.id().equals("lyrics")).findFirst()
                        .ifPresent(m -> m.setEnabled(true));
                dev.orbita.client.feature.SpotifyWatcher.devFake(
                        new dev.orbita.client.feature.SpotifyWatcher.NowPlaying(true, true, "One More Time", "Daft Punk"));
            }, mc -> true, 70),
            new Step("spotify-opciones", mc -> mc.setScreen(new ControlPanelScreen(null, HudManager.get().modules().stream()
                    .filter(m -> m.id().equals("spotify")).findFirst().orElseThrow())), mc -> true, 30),
            new Step("freelook-opciones", mc -> {
                dev.orbita.client.feature.SpotifyWatcher.devFake(null);
                mc.setScreen(new ControlPanelScreen(null));
            }, mc -> true, 10),
            new Step("mods-juego", mc -> mc.setScreen(new ControlPanelScreen(null)), mc -> true, 40),
            new Step("medicion-mods", mc -> {
                DevShots.frameCount = 0;
                DevShots.lastPanelFrame = 0;
                DevShots.measuringPanel = true;
            }, mc -> {
                // Desplaza el panel cada segundo para medir también el cambio de página.
                if (mc.screen != null && DevShots.ticks % 20 == 0) {
                    mc.screen.mouseScrolled(mc.getWindow().getGuiScaledWidth() / 2.0, mc.getWindow().getGuiScaledHeight() / 2.0,
                            0, (DevShots.ticks / 60) % 2 == 0 ? -1 : 1);
                }
                return true;
            }, 300),
            new Step("opciones", mc -> {
                if (DevShots.measuringPanel) {
                    DevShots.measuringPanel = false;
                    report("medicion-mods");
                    reportParts();
                }
                mc.setScreen(new ControlPanelScreen(null, HudManager.get().modules().get(0)));
            }, mc -> true, 40),
            new Step("nova-opciones", mc -> {
                ControlPanelScreen.optionsForNova = dev.orbita.client.nova.NovaClient.modules.get(dev.orbita.client.nova.module.cosmetic.Wings.class);
                mc.setScreen(new ControlPanelScreen(null));
            }, mc -> true, 40),
            new Step("nova-ahorro", mc -> {
                ControlPanelScreen.optionsForNova = dev.orbita.client.nova.NovaClient.modules.get(dev.orbita.client.nova.module.ClientSettings.class);
                mc.setScreen(new ControlPanelScreen(null));
            }, mc -> true, 40),
            new Step("nova-mundo", mc -> {
                mc.setScreen(null);
                var mm = dev.orbita.client.nova.NovaClient.modules;
                for (Class<? extends dev.orbita.client.nova.module.Module> c : List.of(
                        dev.orbita.client.nova.module.cosmetic.Wings.class, dev.orbita.client.nova.module.cosmetic.Halo.class,
                        dev.orbita.client.nova.module.cosmetic.AuraRings.class, dev.orbita.client.nova.module.cosmetic.Trail.class,
                        dev.orbita.client.nova.module.cosmetic.Pet.class, dev.orbita.client.nova.module.SkyModule.class)) {
                    mm.get(c).setEnabled(true);
                }
                mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
            }, mc -> true, 80),
            new Step("fin", mc -> {
                mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
                dev.orbita.client.nova.NovaClient.modules.all.stream()
                        .filter(m -> m.category == dev.orbita.client.nova.module.Category.COSMETICS
                                || m instanceof dev.orbita.client.nova.module.SkyModule)
                        .forEach(m -> m.setEnabled(false));
            },
                    mc -> true, 40));

    /** Tiempos de fotograma (ns) medidos durante el paso "medicion". */
    private static final long[] FRAMES = new long[20000];
    private static int frameCount;
    private static long lastFrame;
    private static boolean measuring;
    private static boolean measuringPanel;

    public static boolean enabled() {
        return ENABLED;
    }

    /** Llamado en cada fotograma desde el panel de mods (solo en modo desarrollo). */
    public static void panelFrame(long renderNanos) {
        if (!measuringPanel) {
            return;
        }
        if (frameCount < FRAMES.length) {
            // Se guarda el tiempo de CPU que tarda en dibujarse el panel (el juego limita los FPS sin actividad).
            FRAMES[frameCount++] = renderNanos * 1000;
        }
    }

    private static long lastPanelFrame;
    private static final long[] PARTS = new long[8];
    private static final String[] PART_NAMES = {"fondo", "tarjetas-forma", "tarjetas-icono", "tarjetas-texto", "encima", "particulas", "", ""};

    /** Acumula tiempo por parte del panel (solo mientras se mide). */
    public static void part(int index, long nanos) {
        if (measuringPanel) {
            PARTS[index] += nanos;
        }
    }

    private static void reportParts() {
        StringBuilder sb = new StringBuilder("DevShots partes del panel (us por fotograma):");
        for (int i = 0; i < PARTS.length; i++) {
            if (!PART_NAMES[i].isEmpty()) {
                sb.append(' ').append(PART_NAMES[i]).append('=').append(frameCount == 0 ? 0 : PARTS[i] / 1000 / frameCount);
            }
        }
        OrbitaClient.LOG.info(sb.toString());
    }

    /** Llamado en cada fotograma desde el HUD (solo en modo desarrollo). */
    public static void frame() {
        long now = System.nanoTime();
        if (measuring && lastFrame != 0 && frameCount < FRAMES.length) {
            FRAMES[frameCount++] = now - lastFrame;
        }
        lastFrame = now;
    }

    private static void report() {
        measuring = false;
        report("medicion");
    }

    private static void report(String label) {
        if (frameCount == 0) {
            return;
        }
        long[] sorted = java.util.Arrays.copyOf(FRAMES, frameCount);
        java.util.Arrays.sort(sorted);
        double total = 0;
        int over33 = 0;
        int over50 = 0;
        for (int i = 0; i < frameCount; i++) {
            total += sorted[i];
            if (sorted[i] > 33_000_000L) over33++;
            if (sorted[i] > 50_000_000L) over50++;
        }
        double avgMs = total / frameCount / 1e6;
        double p99 = sorted[(int) (frameCount * 0.99)] / 1e6;
        double max = sorted[frameCount - 1] / 1e6;
        OrbitaClient.LOG.info(String.format(java.util.Locale.ROOT,
                "DevShots %s: %d fotogramas, media %.2f ms (%.0f FPS), p99 %.2f ms, max %.2f ms, >33ms: %d, >50ms: %d",
                label, frameCount, avgMs, 1000 / avgMs, p99, max, over33, over50)
                + (label.equals("medicion-mods") ? "  (valores en microsegundos de dibujo del panel, no ms)" : ""));
    }

    private static int index;
    private static int ticks;
    private static boolean started;

    private DevShots() {
    }

    /** Pulsa "Crear mundo" en la pantalla de creación que abre testWorld. */
    private static void pressCreate(Minecraft mc) {
        if (!(mc.screen instanceof CreateWorldScreen screen)) {
            return;
        }
        for (var child : screen.children()) {
            if (child instanceof net.minecraft.client.gui.components.Button b
                    && b.getMessage().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tc
                    && tc.getKey().equals("selectWorld.create") && b.active) {
                b.onPress(new net.minecraft.client.input.MouseButtonInfo(0, 0));
                return;
            }
        }
    }

    public static void tick(Minecraft mc) {
        if (!ENABLED || index >= STEPS.size()) {
            return;
        }
        Step step = STEPS.get(index);
        if (!started) {
            step.action().accept(mc);
            started = true;
            ticks = 0;
            return;
        }
        if (!step.ready().test(mc)) {
            ticks = 0;
            return;
        }
        if (++ticks == step.settle()) {
            Screenshot.grab(mc.gameDirectory, "orbita-" + step.shot() + ".png", mc.getMainRenderTarget(), 1,
                    msg -> OrbitaClient.LOG.info("DevShots: {}", msg.getString()));
        } else if (ticks == step.settle() + 10) {
            index++;
            started = false;
        }
    }
}
