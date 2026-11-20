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

    public static final class GapCounter extends Hud.Base {
        public final Setting.Bool mine = add(new Setting.Bool("Your gaps", true));
        public final Setting.Bool others = add(new Setting.Bool("Gaps eaten by others", true));
        public final Setting.Action reset = add(new Setting.Action("Reset counter", "Reset", GapTracker.eaten::clear));

        public GapCounter() { super("Gap Counter", "Golden apples you have and how many others ate", 98, 70, false); }

        @Override public void onTick() { GapTracker.tick(mc()); }

        private List<String> lines() {
            List<String> l = new ArrayList<>();
            var mc = mc();
            if (mc.player == null) return l;
            if (mine.value) {
                int g = 0, eg = 0;
                var inv = mc.player.getInventory();
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    ItemStack s = inv.getItem(i);
                    if (s.is(Items.GOLDEN_APPLE)) g += s.getCount();
                    else if (s.is(Items.ENCHANTED_GOLDEN_APPLE)) eg += s.getCount();
                }
                l.add(Lang.t("Gaps") + ": " + g + (eg > 0 ? "  (+" + eg + " god)" : ""));
            }
            if (others.value) {
                GapTracker.eaten.entrySet().stream()
                        .filter(e -> !e.getKey().equals(mc.player.getUUID()))
                        .sorted((a, b) -> b.getValue() - a.getValue()).limit(6)
                        .forEach(e -> l.add(GapTracker.names.getOrDefault(e.getKey(), "?") + "  x" + e.getValue()));
            }
            return l;
        }

        @Override protected int width(Font tr) {
            int w = 60;
            for (String s : lines()) w = Math.max(w, Ui.width(tr, s, false) + 22);
            return w;
        }
        @Override protected int height(Font tr) { return Math.max(1, lines().size()) * 11 + 4; }
        @Override protected void draw(GuiGraphics ctx, Font tr, int px, int py) {
            int y = py + 3;
            boolean first = true;
            for (String s : lines()) {
                if (first) { dev.orbita.client.nova.compat.V.push(ctx); dev.orbita.client.nova.compat.V.translate(ctx, px + 2, y - 2); dev.orbita.client.nova.compat.V.scale(ctx, 0.75f, 0.75f); ctx.renderItem(new ItemStack(Items.GOLDEN_APPLE), 0, 0); dev.orbita.client.nova.compat.V.pop(ctx); }
                Ui.text(ctx, tr, s, px + (first ? 18 : 6), y, first ? textColor() : 0xFFFFD27A, false);
                y += 11; first = false;
            }
        }
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

    // ------------------------------------------------------------------ recorrido de perlas y flechas
    public static final class Trajectories extends Module {
        public final Setting.Bool pearls = add(new Setting.Bool("Ender pearls", true));
        public final Setting.Bool throwables = add(new Setting.Bool("Snowballs, eggs, potions", true));
        public final Setting.Bool bows = add(new Setting.Bool("Bows and tridents", true));
        public final Setting.Bool landing = add(new Setting.Bool("Landing marker", true));
        public final Setting.Col color = add(new Setting.Col("Color", 0.78f, 0.6f));

        public Trajectories() { super("Trajectories", "Shows where your pearl, arrow or potion will land", Category.VISUAL); }

        private record Kind(double speed, double gravity, double pitchOffset, boolean arrow) { }

        private Kind kind(Player p, ItemStack s) {
            Item i = s.getItem();
            if (i == Items.ENDER_PEARL) return pearls.value ? new Kind(1.5, 0.03, 0, false) : null;
            if (!throwables.value && !(i instanceof BowItem) && !(i instanceof CrossbowItem) && i != Items.TRIDENT) return null;
            if (i == Items.SNOWBALL || i == Items.EGG) return new Kind(1.5, 0.03, 0, false);
            if (i == Items.SPLASH_POTION || i == Items.LINGERING_POTION) return new Kind(0.5, 0.05, -20, false);
            if (i == Items.EXPERIENCE_BOTTLE) return new Kind(0.7, 0.07, -20, false);
            if (!bows.value) return null;
            if (i instanceof BowItem) {
                if (!p.isUsingItem()) return null;
                float pull = BowItem.getPowerForTime(p.getTicksUsingItem());
                return pull < 0.1f ? null : new Kind(pull * 3.0, 0.05, 0, true);
            }
            if (i instanceof CrossbowItem && CrossbowItem.isCharged(s)) return new Kind(3.15, 0.05, 0, true);
            if (i == Items.TRIDENT && p.isUsingItem()) return new Kind(2.5, 0.05, 0, true);
            return null;
        }

        @Override public void onWorldAfter(dev.orbita.client.nova.gfx.WCtx ctx) {
            var mc = mc();
            Player p = mc.player;
            if (mc.level == null || p == null) return;
            Kind k = null; InteractionHand hand = InteractionHand.MAIN_HAND;
            for (InteractionHand h : InteractionHand.values()) { k = kind(p, p.getItemInHand(h)); if (k != null) { hand = h; break; } }
            if (k == null) return;
            float td = ctx.tickDelta();
            float yaw = p.getViewYRot(td), pitch = p.getViewXRot(td) + (float) k.pitchOffset;
            double rx = -Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch));
            double ry = -Math.sin(Math.toRadians(pitch));
            double rz = Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch));
            Vec3 pos = p.getPosition(td).add(0, p.getEyeHeight() - 0.1, 0);
            Vec3 vel = new Vec3(rx, ry, rz).normalize().scale(k.speed);
            Vec3 mv = p.getDeltaMovement();
            vel = vel.add(mv.x, p.onGround() ? 0 : mv.y, mv.z);

            List<Vec3> pts = new ArrayList<>();
            pts.add(pos);
            HitResult hit = null;
            Entity hitEntity = null;
            for (int step = 0; step < 300; step++) {
                Vec3 next = pos.add(vel);
                BlockHitResult bh = mc.level.clip(new ClipContext(pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
                if (bh.getType() != HitResult.Type.MISS) next = bh.getLocation();
                // entidades en el camino
                AABB sweep = new AABB(pos, next).inflate(1.0);
                double best = Double.MAX_VALUE;
                for (Entity e : mc.level.getEntities(p, sweep, e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator())) {
                    var r = e.getBoundingBox().inflate(0.3).clip(pos, next);
                    if (r.isPresent() && r.get().distanceToSqr(pos) < best) { best = r.get().distanceToSqr(pos); hitEntity = e; next = r.get(); }
                }
                pts.add(next);
                if (hitEntity != null) break;
                if (bh.getType() != HitResult.Type.MISS) { hit = bh; break; }
                pos = next;
                boolean water = mc.level.getFluidState(net.minecraft.core.BlockPos.containing(pos)).is(net.minecraft.tags.FluidTags.WATER);
                vel = vel.scale(water ? (k.arrow ? 0.6 : 0.8) : 0.99).add(0, -k.gravity, 0);
                if (pos.y < mc.level.getMinY() - 8) break;
            }

            Vec3 cam = ctx.camera().position();
            var ms = ctx.matrixStack() != null ? ctx.matrixStack() : new com.mojang.blaze3d.vertex.PoseStack();
            Matrix4f m = ms.last().pose();
            int rgb = color.rgb();
            // el primer punto sale de la mano para que se vea en primera persona
            Vec3 look = new Vec3(rx, ry, rz).normalize();
            Vec3 side = look.cross(new Vec3(0, 1, 0)).normalize().scale(hand == InteractionHand.MAIN_HAND ? 0.25 : -0.25);
            Gfx.begin();
            for (int i = 0; i + 1 < pts.size(); i++) {
                float f0 = Math.min(1f, i / 6f), f1 = Math.min(1f, (i + 1) / 6f);
                Vec3 a = pts.get(i).add(side.scale(1 - f0)).add(0, -0.12 * (1 - f0), 0).subtract(cam);
                Vec3 b = pts.get(i + 1).add(side.scale(1 - f1)).add(0, -0.12 * (1 - f1), 0).subtract(cam);
                float t = i / (float) pts.size();
                int c = Gfx.argb(Gfx.mixRgb(rgb, 0xFFFFFF, t * 0.4f), (int) (200 * Math.min(1f, i / 3f + 0.2f)));
                ribbon(m, a, b, 0.035f, c);
            }
            Vec3 end = pts.get(pts.size() - 1).subtract(cam);
            if (landing.value) {
                int c = Gfx.argb(hitEntity != null ? 0xFF4A4A : rgb, 170);
                if (hitEntity != null) {
                    AABB b = hitEntity.getBoundingBox().move(hitEntity.getPosition(td).subtract(hitEntity.position())).move(-cam.x, -cam.y, -cam.z);
                    Gfx.box(m, (float) b.minX, (float) b.minY, (float) b.minZ, (float) b.maxX, (float) b.maxY, (float) b.maxZ, Gfx.argb(0xFF4A4A, 50));
                } else if (hit instanceof BlockHitResult bh) {
                    Direction d = bh.getDirection();
                    float pulse = 0.35f + 0.05f * (float) Math.sin(System.currentTimeMillis() / 180.0);
                    ms.pushPose();
                    ms.translate(end.x + d.getStepX() * 0.01, end.y + d.getStepY() * 0.01, end.z + d.getStepZ() * 0.01);
                    ms.mulPose(d.getRotation());
                    Matrix4f mm = ms.last().pose();
                    Gfx.ringXZ(mm, 0, pulse, 0.04f, 32, c);
                    Gfx.ringXZ(mm, 0, pulse * 0.45f, 0.03f, 24, Gfx.argb(0xFFFFFF, 140));
                    ms.popPose();
                }
            }
            Gfx.end();
        }

        /** Linea gruesa que siempre mira a la camara (la camara esta en 0,0,0). */
        private static void ribbon(Matrix4f m, Vec3 a, Vec3 b, float w, int c) {
            Vec3 dir = b.subtract(a);
            Vec3 side = dir.cross(a.add(b).scale(0.5));
            if (side.lengthSqr() < 1e-9) return;
            side = side.normalize().scale(w);
            Gfx.quad(m, v(a.add(side)), c, v(a.subtract(side)), c, v(b.subtract(side)), c, v(b.add(side)), c);
        }

        private static Vector3f v(Vec3 p) { return new Vector3f((float) p.x, (float) p.y, (float) p.z); }
    }
}
