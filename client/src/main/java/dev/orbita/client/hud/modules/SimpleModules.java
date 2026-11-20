package dev.orbita.client.hud.modules;

import dev.orbita.client.feature.CombatTracker;
import dev.orbita.client.feature.CpsTracker;
import dev.orbita.client.feature.MotionTracker;
import dev.orbita.client.hud.HudModule;
import dev.orbita.client.hud.TextHudModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Function;

/** Módulos de una sola línea (estilo Lunar). Cada uno es solo una función que devuelve su texto. */
public final class SimpleModules {
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");

    private SimpleModules() {
    }

    private static final class Line extends TextHudModule {
        private final Function<Minecraft, String> text;

        Line(String id, String name, String desc, String glyph, boolean on, int x, int y, long refreshMs,
             Function<Minecraft, String> text) {
            super(id, name, desc, glyph, HudModule.Category.HUD, on, x, y, refreshMs);
            this.text = text;
        }

        @Override
        protected String text(Minecraft mc) {
            return text.apply(mc);
        }
    }

    public static HudModule fps() {
        return new Line("fps", "FPS", "Cuadros por segundo", "FPS", true, 4, 4, 250, mc -> mc.getFps() + " FPS").icon(Items.ITEM_FRAME);
    }

    public static HudModule cps() {
        return new Line("cps", "CPS", "Clics por segundo (izq. | der.)", "CPS", true, 4, 26, 50,
                mc -> CpsTracker.left() + " | " + CpsTracker.right() + " CPS").icon(Items.TARGET);
    }

    public static HudModule ping() {
        return new Line("ping", "Ping", "Latencia con el servidor", "MS", true, 4, 48, 1000, mc -> {
            if (mc.isLocalServer()) {
                return "0 ms";
            }
            ClientPacketListener c = mc.getConnection();
            PlayerInfo info = c == null ? null : c.getPlayerInfo(mc.player.getUUID());
            return (info == null ? 0 : info.getLatency()) + " ms";
        }).icon(Items.SCULK_SENSOR);
    }

    public static HudModule clock() {
        return new Line("clock", "Reloj", "Hora de tu ordenador", "12:00", false, -1, 140, 1000,
                mc -> LocalTime.now().format(CLOCK)).icon(Items.CLOCK);
    }

    public static HudModule memory() {
        return new Line("memory", "Memoria", "Uso de RAM del juego", "RAM", false, -1, 162, 1000, mc -> {
            Runtime rt = Runtime.getRuntime();
            long used = rt.totalMemory() - rt.freeMemory();
            return "RAM: " + (used * 100 / rt.maxMemory()) + "%";
        }).icon(Items.COMPARATOR);
    }

    public static HudModule server() {
        return new Line("server", "Servidor", "Dirección del servidor actual", "IP", false, -1, 184, 2000, mc -> {
            ServerData data = mc.getCurrentServer();
            if (data != null) {
                return data.ip;
            }
            return mc.isLocalServer() ? "Un jugador" : "—";
        }).icon(Items.BEACON);
    }

    public static HudModule speed() {
        return new Line("speed", "Velocidad", "Bloques por segundo", "m/s", false, -1, 206, 100,
                mc -> String.format(Locale.ROOT, "%.2f m/s", MotionTracker.speed())).icon(Items.SUGAR);
    }

    public static HudModule biome() {
        return new Line("biome", "Bioma", "Bioma en el que estás", "BIO", false, -1, 228, 1000, mc ->
                mc.level.getBiome(mc.player.blockPosition()).unwrapKey()
                        .map(k -> Component.translatable(k.identifier().toLanguageKey("biome")).getString())
                        .orElse("?")).icon(Items.OAK_SAPLING);
    }

    public static HudModule day() {
        return new Line("day", "Día", "Días transcurridos en el mundo", "DÍA", false, -1, 250, 2000,
                mc -> "Día " + (mc.level.getDayTime() / 24000L + 1)).icon(Items.DAYLIGHT_DETECTOR);
    }

    public static HudModule combo() {
        return new Line("combo", "Combo", "Golpes seguidos sin recibir daño", "x3", false, -1, 272, 100, mc -> {
            int c = CombatTracker.combo();
            return c == 0 ? "Sin combo" : c + " combo";
        }).icon(Items.DIAMOND_SWORD);
    }

    private static Object sessionLevel;
    private static long sessionStart;

    public static HudModule session() {
        return new Line("session", "Sesión", "Tiempo jugando en este mundo o servidor", "0:00", false, -1, 316, 1000, mc -> {
            if (mc.level != sessionLevel) {
                sessionLevel = mc.level;
                sessionStart = System.currentTimeMillis();
            }
            long s = (System.currentTimeMillis() - sessionStart) / 1000;
            return s >= 3600
                    ? String.format(Locale.ROOT, "Sesión %d:%02d:%02d", s / 3600, s / 60 % 60, s % 60)
                    : String.format(Locale.ROOT, "Sesión %d:%02d", s / 60, s % 60);
        }).icon(Items.AMETHYST_SHARD);
    }

    public static HudModule light() {
        return new Line("light", "Luz", "Luz de bloques donde estás (por debajo de 1 aparecen monstruos)", "LUZ", false, -1, 338, 250, mc -> {
            net.minecraft.core.BlockPos pos = mc.player.blockPosition();
            int block = mc.level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, pos);
            int sky = mc.level.getBrightness(net.minecraft.world.level.LightLayer.SKY, pos);
            return "Luz " + block + " · Cielo " + sky;
        }).icon(Items.TORCH);
    }

    public static HudModule target() {
        return new Line("target", "Mirando", "Bloque o criatura a la que apuntas", "BLOQUE", false, -1, 360, 100, mc -> {
            if (mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult b
                    && b.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                return mc.level.getBlockState(b.getBlockPos()).getBlock().getName().getString();
            }
            if (mc.hitResult instanceof net.minecraft.world.phys.EntityHitResult eh) {
                return eh.getEntity().getDisplayName().getString();
            }
            return "Mirando: —";
        }).icon(Items.SPYGLASS);
    }

    public static HudModule reach() {
        return new Line("reach", "Alcance", "Distancia de tu último golpe", "3.0", false, -1, 294, 200, mc -> {
            double r = CombatTracker.reach();
            return r < 0 ? "Alcance: —" : String.format(Locale.ROOT, "%.2f bloques", r);
        }).icon(Items.BOW);
    }
}
