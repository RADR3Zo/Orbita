package dev.orbita.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.orbita.client.config.OrbitaConfig;
import dev.orbita.client.feature.CombatTracker;
import dev.orbita.client.feature.FreelookFeature;
import dev.orbita.client.feature.SpotifyWatcher;
import dev.orbita.client.feature.MotionTracker;
import dev.orbita.client.feature.ZoomFeature;
import dev.orbita.client.hud.HudManager;
import dev.orbita.client.hud.modules.ArmorHudModule;
import dev.orbita.client.hud.modules.CoordinatesModule;
import dev.orbita.client.hud.modules.DirectionModule;
import dev.orbita.client.hud.modules.KeystrokesModule;
import dev.orbita.client.hud.modules.PotionHudModule;
import dev.orbita.client.hud.modules.SimpleModules;
import dev.orbita.client.hud.modules.SpotifyModule;
import dev.orbita.client.hud.modules.ToggleSprintModule;
import dev.orbita.client.ui.screen.ControlPanelScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OrbitaClient implements ClientModInitializer {
    public static final String MOD_ID = "orbita";
    public static final Logger LOG = LoggerFactory.getLogger("Órbita");

    private static KeyMapping panelKey;
    private static KeyMapping zoomKey;
    private static KeyMapping freelookKey;
    private static KeyMapping spotifyPlayKey;
    private static KeyMapping spotifyNextKey;
    private static KeyMapping spotifyPrevKey;

    @Override
    public void onInitializeClient() {
        OrbitaConfig.load();

        HudManager hud = HudManager.get();
        hud.register(SimpleModules.fps());
        hud.register(SimpleModules.cps());
        hud.register(SimpleModules.ping());
        hud.register(new CoordinatesModule());
        hud.register(new DirectionModule());
        hud.register(new KeystrokesModule());
        hud.register(new ToggleSprintModule());
        hud.register(new PotionHudModule());
        hud.register(new ArmorHudModule());
        hud.register(new SpotifyModule());
        hud.register(new dev.orbita.client.hud.modules.LyricsModule());
        hud.register(SimpleModules.clock());
        hud.register(SimpleModules.memory());
        hud.register(SimpleModules.server());
        hud.register(SimpleModules.speed());
        hud.register(SimpleModules.biome());
        hud.register(SimpleModules.day());
        hud.register(SimpleModules.combo());
        hud.register(SimpleModules.reach());
        hud.register(SimpleModules.session());
        hud.register(SimpleModules.light());
        hud.register(SimpleModules.target());
        hud.register(new dev.orbita.client.hud.modules.GapCounterModule());
        hud.loadFromConfig();
        hud.applyMigrations();
        // Los módulos que ya estaban activados no pasan por onToggle al cargar: se sincronizan aquí.
        boolean spotifyOn = hud.modules().stream().anyMatch(m -> m.id().equals("spotify") && m.isEnabled());
        boolean lyricsOn = hud.modules().stream().anyMatch(m -> m.id().equals("lyrics") && m.isEnabled());
        dev.orbita.client.feature.LyricsService.setEnabled(lyricsOn);
        SpotifyWatcher.setEnabled(spotifyOn);
        SpotifyWatcher.setFastPoll(lyricsOn);

        // Un único elemento de HUD dibuja todos los módulos activos, por debajo del chat.
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, id("hud"), hud::render);

        KeyMapping.Category category = KeyMapping.Category.register(id("orbita"));
        panelKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.orbita.panel", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, category));
        zoomKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.orbita.zoom", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, category));
        freelookKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.orbita.freelook", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, category));
        spotifyPrevKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.orbita.spotify_previous", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F7, category));
        spotifyPlayKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.orbita.spotify_play", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F8, category));
        spotifyNextKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.orbita.spotify_next", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F9, category));
        SpotifyWatcher.start();
        // Funciones traídas de Nova (cosméticos, emotes, cielos, video de fondo, modo ahorro y extras de PvP)
        dev.orbita.client.nova.NovaClient.init(category);

        AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (level.isClientSide()) {
                CombatTracker.onAttack(player, entity);
            }
            return InteractionResult.PASS;
        });

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (panelKey.consumeClick()) {
                if (mc.screen == null) {
                    mc.setScreen(new ControlPanelScreen(null));
                }
            }
            ZoomFeature.tick(mc, zoomKey.isDown() && OrbitaConfig.get().zoomEnabled);
            boolean freelookClick = false;
            while (freelookKey.consumeClick()) {
                freelookClick = true;
            }
            FreelookFeature.tick(mc, freelookKey.isDown(), freelookClick);
            while (spotifyPlayKey.consumeClick()) {
                SpotifyWatcher.send(SpotifyWatcher.Command.PLAY_PAUSE);
            }
            while (spotifyNextKey.consumeClick()) {
                SpotifyWatcher.send(SpotifyWatcher.Command.NEXT);
            }
            while (spotifyPrevKey.consumeClick()) {
                SpotifyWatcher.send(SpotifyWatcher.Command.PREVIOUS);
            }
            CombatTracker.tick(mc);
            ToggleSprintModule.tick(mc);
            DevShots.tick(mc);
            MotionTracker.tick(mc);
        });

        LOG.info("Órbita {} inicializado", version());
    }

    public static KeyMapping zoomKey() {
        return zoomKey;
    }

    public static KeyMapping freelookKey() {
        return freelookKey;
    }

    public static KeyMapping spotifyPlayKey() {
        return spotifyPlayKey;
    }

    public static KeyMapping spotifyNextKey() {
        return spotifyNextKey;
    }

    public static KeyMapping spotifyPrevKey() {
        return spotifyPrevKey;
    }

    public static String version() {
        return FabricLoader.getInstance().getModContainer(MOD_ID)
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("dev");
    }

    public static String minecraftVersion() {
        return FabricLoader.getInstance().getModContainer("minecraft")
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("");
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
