package dev.orbita.client.nova.module;

import dev.orbita.client.nova.NovaClient;
import dev.orbita.client.nova.core.Lang;
import dev.orbita.client.nova.core.Setting;
import dev.orbita.client.nova.gfx.Gfx;
import dev.orbita.client.nova.gui.Ui;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Cosas de PvP solo visuales: criticos, nombres al apuntar, cambiar tu nombre, recorrido de perlas y contador de gaps. */
public final class PvpExtras {
    private PvpExtras() { }

    // ------------------------------------------------------------------ criticos
    public static final class CritEffects extends Module implements Combat.Listener {
        public final Setting.Choice sound = add(new Setting.Choice("Crit sound", 6, "Off", "Vanilla", "Anvil", "Bell", "Pling", "XP", "Little bell"));
        public final Setting.Num volume = add(new Setting.Num("Volume", 0.8, 0.1, 1.0, 0.05));
        public final Setting.Bool everyHit = add(new Setting.Bool("Crit sound on every hit", false));
        public final Setting.Bool alwaysCrits = add(new Setting.Bool("Crit particles on every hit", true));
        public final Setting.Bool sharpness = add(new Setting.Bool("Sharpness particles", false));
        public final Setting.Num multiplier = add(new Setting.Num("Multiplier", 1, 1, 5, 1));

        public CritEffects() { super("Critical Hits", "Crit sounds and particles (visual only)", Category.EFFECTS); Combat.listen(this); }

        /** Igual que Minecraft decide un critico: cayendo, sin correr, sin agua ni escaleras, golpe cargado. */
        static boolean isCrit(Player p) {
            return p.getAttackStrengthScale(0.5f) > 0.9f && p.fallDistance > 0 && !p.onGround() && !p.onClimbable()
                    && !p.isInWater() && !p.hasEffect(MobEffects.BLINDNESS) && !p.isPassenger() && !p.isSprinting();
        }

        @Override public void onAttack(Entity target) {
            var mc = mc();
            if (!enabled || mc.player == null || !(target instanceof LivingEntity)) return;
            boolean crit = isCrit(mc.player);
            for (int i = 0; i < multiplier.i(); i++) {
                if (crit || alwaysCrits.value) mc.particleEngine.createTrackingEmitter(target, ParticleTypes.CRIT);
                if (sharpness.value) mc.particleEngine.createTrackingEmitter(target, ParticleTypes.ENCHANTED_HIT);
            }
            if ((crit || everyHit.value) && sound.index > 0) {
                float v = volume.f(), p = 0.95f + (float) Math.random() * 0.1f;
                SoundEvent s = switch (sound.index) {
                    case 1 -> SoundEvents.PLAYER_ATTACK_CRIT;
                    case 2 -> SoundEvents.ANVIL_LAND;
                    case 3 -> SoundEvents.BELL_BLOCK;
                    case 4 -> SoundEvents.NOTE_BLOCK_PLING.value();
                    case 6 -> dev.orbita.client.nova.module.Effects.NOVA_BELL;
                    default -> SoundEvents.EXPERIENCE_ORB_PICKUP;
                };
                if (sound.index == 2) v *= 0.35f;
                mc.getSoundManager().play(SimpleSoundInstance.forUI(s, p * (sound.index == 3 ? 1.4f : 1f), v));
            }
        }
    }

    // ------------------------------------------------------------------ contador de gaps
    /** Cuenta las manzanas doradas que comen los demas (y las tuyas). */
    public static final class GapTracker {
        static final Map<UUID, Integer> eaten = new HashMap<>();
        static final Map<UUID, String> names = new HashMap<>();
        private static final Map<UUID, Integer> using = new HashMap<>();

        static boolean isGap(ItemStack s) { return s.is(Items.GOLDEN_APPLE) || s.is(Items.ENCHANTED_GOLDEN_APPLE); }

        private static long last = Long.MIN_VALUE;

        public static void tick(Minecraft mc) {
            if (mc.level == null) { eaten.clear(); using.clear(); names.clear(); return; }
            if (mc.level.getGameTime() == last) return; // una vez por tick aunque lo llamen dos modulos
            last = mc.level.getGameTime();
            for (Player p : mc.level.players()) {
                UUID id = p.getUUID();
                if (p.isUsingItem() && isGap(p.getUseItem())) {
                    using.merge(id, 1, Integer::sum);
                } else {
                    Integer t = using.remove(id);
                    if (t != null && t >= 28) { // se la comio entera (32 ticks)
                        eaten.merge(id, 1, Integer::sum);
                        names.put(id, p.getName().getString());
                    }
                }
            }
        }

        public static int of(UUID id) { return eaten.getOrDefault(id, 0); }
        public static Map<UUID, Integer> eaten() { return eaten; }
        public static String name(UUID id) { return names.getOrDefault(id, "?"); }
    }

    // ------------------------------------------------------------------ nombres al apuntar
    public static final class Nametags extends Module {
        public final Setting.Bool onlyAim = add(new Setting.Bool("Only when aiming", true));
        public final Setting.Bool health = add(new Setting.Bool("Health", true));
        public final Setting.Bool distance = add(new Setting.Bool("Distance", true));
        public final Setting.Bool ping = add(new Setting.Bool("Ping", false));
        public final Setting.Bool gaps = add(new Setting.Bool("Gaps eaten", true));
        public final Setting.Bool hideVanilla = add(new Setting.Bool("Hide normal nametags", false));
        public final Setting.Num range = add(new Setting.Num("Range", 64, 8, 128, 1));
        public final Setting.Num size = add(new Setting.Num("Size", 1.0, 0.5, 2.0, 0.05));

        private static final Matrix4f VIEW = new Matrix4f(), PROJ = new Matrix4f();
        private static Vec3 cam = Vec3.ZERO;
        private static boolean ready;
        private final Map<UUID, Float> fade = new HashMap<>();

        public Nametags() { super("Nametags", "Player name, health and distance when you aim at them", Category.VISUAL); }

        public static boolean hideVanilla() {
            if (NovaClient.modules == null) return false;
            Nametags n = NovaClient.modules.get(Nametags.class);
            return n != null && n.enabled && n.hideVanilla.value;
        }

        @Override public void onTick() { GapTracker.tick(mc()); }

        @Override public void onWorldAfter(dev.orbita.client.nova.gfx.WCtx ctx) {
            VIEW.set(ctx.positionMatrix());
            PROJ.set(ctx.projectionMatrix());
            cam = ctx.camera().position();
            ready = true;
        }

        /** Pasa un punto del mundo a la pantalla (en pixeles de GUI). null si queda detras. */
        static float[] project(Vec3 p, int sw, int sh) {
            Vector4f v = new Vector4f((float) (p.x - cam.x), (float) (p.y - cam.y), (float) (p.z - cam.z), 1f);
            VIEW.transform(v);
            PROJ.transform(v);
            if (v.w <= 0.05f) return null;
            float nx = v.x / v.w, ny = v.y / v.w;
            return new float[]{(nx * 0.5f + 0.5f) * sw, (1f - (ny * 0.5f + 0.5f)) * sh};
        }

        private boolean aimed(Player me, Player p, float td) {
            Vec3 eye = me.getEyePosition(td), end = eye.add(me.getViewVector(td).scale(range.value));
            AABB b = p.getBoundingBox().move(p.getPosition(td).subtract(p.position())).inflate(0.35);
            if (b.clip(eye, end).isEmpty()) return false;
            return me.hasLineOfSight(p);
        }

        @Override public void onHud(GuiGraphics ctx) {
            var mc = mc();
            if (!ready || mc.level == null || mc.player == null || mc.options.hideGui) return;
            float td = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
            int sw = ctx.guiWidth(), sh = ctx.guiHeight();
            Font tr = mc.font;
            for (Player p : mc.level.players()) {
                if (p == mc.player) continue;
                boolean show = !p.isInvisible() && p.distanceToSqr(mc.player) < range.value * range.value
                        && (!onlyAim.value || aimed(mc.player, p, td));
                float a = fade.getOrDefault(p.getUUID(), 0f);
                a = Math.max(0f, Math.min(1f, a + (show ? 0.12f : -0.08f)));
                if (a <= 0f) { fade.remove(p.getUUID()); continue; }
                fade.put(p.getUUID(), a);
                Vec3 head = p.getPosition(td).add(0, p.getBbHeight() + 0.55, 0);
                float[] s = project(head, sw, sh);
                if (s == null) continue;
                drawTag(ctx, tr, p, s[0], s[1], a);
            }
            fade.keySet().removeIf(id -> mc.level.getPlayerByUUID(id) == null);
        }

        private void drawTag(GuiGraphics ctx, Font tr, Player p, float x, float y, float a) {
            var mc = mc();
            float hp = p.getHealth() + p.getAbsorptionAmount(), frac = Mth.clamp(p.getHealth() / p.getMaxHealth(), 0, 1);
            int hpCol = frac > 0.6f ? 0x55FF77 : frac > 0.3f ? 0xFFD24A : 0xFF5050;
            if (mc.player != null) { // color segun si le ganas en corazones (verde) o te gana (rojo); iguales = por nivel de vida
                float mine = mc.player.getHealth() + mc.player.getAbsorptionAmount();
                if (mine > hp + 0.01f) hpCol = 0x2FBF78;
                else if (hp > mine + 0.01f) hpCol = 0xD64545;
            }
            StringBuilder extra = new StringBuilder();
            if (distance.value) extra.append(String.format("%.0fm", Math.sqrt(p.distanceToSqr(mc.player))));
            if (ping.value && mc.getConnection() != null) {
                PlayerInfo e = mc.getConnection().getPlayerInfo(p.getUUID());
                if (e != null) extra.append(extra.length() > 0 ? "  " : "").append(e.getLatency()).append("ms");
            }
            int g = GapTracker.of(p.getUUID());
            if (gaps.value && g > 0) extra.append(extra.length() > 0 ? "  " : "").append(g).append(" gap").append(g > 1 ? "s" : "");
            String hpText = health.value ? String.format("%.1f", hp / 2f) + " ❤" : "";
            var name = p.getDisplayName();
            int nameW = tr.width(name), hpW = hpText.isEmpty() ? 0 : Ui.width(tr, hpText, true) + 8;
            int w = Math.max(nameW + hpW, Ui.width(tr, extra.toString(), false)) + 14;
            int h = extra.length() > 0 ? 28 : 17;
            float sc = size.f() * (0.85f + 0.15f * a);
            int al = (int) (255 * a);
            dev.orbita.client.nova.compat.V.push(ctx);
            dev.orbita.client.nova.compat.V.translate(ctx, x, y);
            dev.orbita.client.nova.compat.V.scale(ctx, sc, sc);
            int x0 = -w / 2, y0 = -h;
            Ui.glow(ctx, x0, y0, w, h, 7, dev.orbita.client.nova.core.Theme.rgb(), (int) (60 * a), 3);
            Ui.roundRect(ctx, x0, y0, w, h, 7, Ui.rgba(0x0B0B14, (int) (190 * a)));
            if (health.value) { // barrita de vida abajo
                int bw = w - 12;
                Ui.roundRect(ctx, x0 + 6, y0 + h - 4, bw, 2, 1, Ui.rgba(0xFFFFFF, (int) (40 * a)));
                Ui.roundRect(ctx, x0 + 6, y0 + h - 4, Math.max(2, (int) (bw * frac)), 2, 1, Ui.rgba(hpCol, al));
            }
            if (al > 8) {
                ctx.drawString(tr, name, x0 + 7, y0 + 4, Ui.rgba(0xFFFFFF, al), true);
                if (!hpText.isEmpty()) Ui.text(ctx, tr, hpText, x0 + 7 + nameW + 8, y0 + 4, Ui.rgba(hpCol, al), true);
                if (extra.length() > 0) Ui.text(ctx, tr, extra.toString(), x0 + 7, y0 + 15, Ui.rgba(0xB8BCCB, al), false);
            }
            dev.orbita.client.nova.compat.V.pop(ctx);
        }
    }

    // ------------------------------------------------------------------ cambiar nombre (solo en tu pantalla)
    public static final class NameChanger extends Module {
        public final Setting.Str fake = add(new Setting.Str("Fake name", "NovaPlayer", 32));
        public final Setting.Action reset = add(new Setting.Action("Reset name", "Reset", () -> fake.value = "")); // vuelve a tu nombre real

        public NameChanger() { super("Name Changer", "Changes your name only on your screen, only on servers: chat, tab, nametag...", Category.UTILITY); }

        private static NameChanger inst;
        private static String cachedReal, cachedFake;
        private static Pattern cachedPattern;

        /** Lo usa el mixin de texto: cambia tu nombre en todo lo que se dibuja (solo en servers). */
        public static String replace(String text) {
            if (text == null || text.isEmpty()) return text;
            if (inst == null) { if (NovaClient.modules == null) return text; inst = NovaClient.modules.get(NameChanger.class); if (inst == null) return text; }
            if (!inst.enabled || inst.fake.value.isBlank()) return text;
            Minecraft mc = Minecraft.getInstance();
            if (mc.isLocalServer()) return text; // solo en servers (en tu mundo singleplayer no cambia nada)
            var session = mc.getUser();
            if (session == null) return text;
            String real = session.getName();
            if (real == null || real.isEmpty() || !text.contains(real)) return text;
            if (!real.equals(cachedReal)) { cachedReal = real; cachedPattern = Pattern.compile("(?<![A-Za-z0-9_])" + Pattern.quote(real) + "(?![A-Za-z0-9_])"); }
            cachedFake = Matcher.quoteReplacement(inst.fake.value);
            return cachedPattern.matcher(text).replaceAll(cachedFake);
        }
    }

}
