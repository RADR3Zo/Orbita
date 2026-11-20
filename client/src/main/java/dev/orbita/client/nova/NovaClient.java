package dev.orbita.client.nova;

import com.mojang.blaze3d.platform.InputConstants;
import dev.orbita.client.nova.module.ModuleManager;
import dev.orbita.client.nova.module.cosmetic.CosmeticFeature;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import org.lwjgl.glfw.GLFW;

/**
 * Funciones traídas de Nova Client: cosméticos y emotes, cielos y video de fondo, modo ahorro y FPS, y extras
 * visuales de PvP. No es un mod aparte: lo arranca OrbitaClient. Sus opciones están en el panel de mods de Órbita.
 */
public final class NovaClient {
    public static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("Órbita/Nova");
    public static ModuleManager modules;
    public static KeyMapping emoteKey;
    /** No se usan en Órbita (Órbita tiene su propio zoom y panel); existen para el código de Nova que los nombra. */
    public static KeyMapping zoomKey, menuKey;

    private NovaClient() {
    }

    public static void init(KeyMapping.Category category) {
        emoteKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.orbita.emotes", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, category));
        zoomKey = new KeyMapping("key.orbita.nova_zoom_unused", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, category);
        menuKey = zoomKey;

        dev.orbita.client.nova.compat.V.init();
        ClientLifecycleEvents.CLIENT_STARTED.register(c -> modules = new ModuleManager());

        // cosméticos pegados al modelo del jugador (3a persona, inventario y menú)
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type, renderer, helper, ctx) -> {
            if (renderer instanceof AvatarRenderer pr) {
                helper.register(new CosmeticFeature(pr));
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(c -> {
            if (modules == null) {
                return;
            }
            while (emoteKey.consumeClick()) {
                if (c.screen == null && c.player != null) {
                    c.setScreen(new dev.orbita.client.nova.gui.EmoteScreen());
                }
            }
            modules.tick();
            if (c.level != null) {
                dev.orbita.client.nova.module.Combat.tick();
            }
            dev.orbita.client.nova.core.FpsRecorder.tick(c); // FPS de cada partida para la página Rendimiento del launcher
        });
        HudRenderCallback.EVENT.register((ctx, tickCounter) -> {
            if (modules != null) {
                modules.hud(ctx);
            }
        });
        dev.orbita.client.nova.compat.V.registerWorld(
                c -> {
                    if (modules != null) {
                        modules.worldBefore(c);
                    }
                },
                c -> {
                    if (modules != null) {
                        modules.worldAfter(c);
                    }
                });
        // a quién le pegas (solo para efectos visuales: no cambia el golpe)
        net.fabricmc.fabric.api.event.player.AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClientSide()) {
                dev.orbita.client.nova.module.Combat.attacked(entity);
            }
            return net.minecraft.world.InteractionResult.PASS;
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(c -> {
            if (modules != null) {
                modules.save();
            }
            dev.orbita.client.nova.core.FpsRecorder.stop();
        });
    }
}
