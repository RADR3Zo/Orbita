package dev.orbita.client.nova.module;

import dev.orbita.client.nova.NovaClient;
import dev.orbita.client.nova.core.Setting;
import dev.orbita.client.nova.gfx.Fx;
import dev.orbita.client.nova.gfx.Gfx;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Efectos visuales y de sonido (todo es solo para tu pantalla, no cambia el juego). */
public final class Effects {
    private Effects() { }

    /** Campanita propia de Nova (assets/nova/sounds/crit_bell.ogg), aguda y corta. */
    public static final SoundEvent NOVA_BELL = SoundEvent.createVariableRangeEvent(net.minecraft.resources.Identifier.fromNamespaceAndPath("nova", "crit_bell"));

    static Fx.Shape shape(Setting.Choice c) { return Fx.Shape.values()[c.index]; }

    static int pick(Setting.Bool rainbow, Setting.Col color) {
        return rainbow.value ? java.awt.Color.HSBtoRGB(Fx.RNG.nextFloat(), 0.6f, 1f) & 0xFFFFFF : color.rgb();
    }

    static Vec3 center(Entity e) { return e.position().add(0, e.getBbHeight() * 0.55, 0); }

    // ------------------------------------------------------------------ golpes
    public static final class HitParticles extends Module implements Combat.Listener {
        public final Setting.Choice shape = add(new Setting.Choice("Shape", 1, Fx.SHAPES));
        public final Setting.Num count = add(new Setting.Num("Count", 12, 2, 40, 1));
        public final Setting.Num size = add(new Setting.Num("Size", 0.25, 0.08, 0.8, 0.01));
        public final Setting.Bool bubble = add(new Setting.Bool("Hit bubble", true));
        public final Setting.Bool rainbow = add(new Setting.Bool("Rainbow", false));
        public final Setting.Col color = add(new Setting.Col("Color", 0.78f, 0.6f)).showIf(() -> !rainbow.value);

        public HitParticles() { super("Hit Particles", "Particles when your hit lands", Category.EFFECTS); Combat.listen(this); }

        @Override public void onHit(LivingEntity t) {
            if (!enabled) return;
            Vec3 c = center(t);
            for (int i = 0; i < count.i(); i++) Fx.burst(shape(shape), c, 1, 0.18, size.f(), pick(rainbow, color), 18);
            if (bubble.value) { // anillo que crece donde golpeaste
                Fx.P p = Fx.spawn(Fx.Shape.RING, c, 0, 0, 0, 0.2f, pick(rainbow, color), 12);
                p.growth = 0.12f; p.drag = 1;
            }
        }
    }

    public static final class HitSounds extends Module implements Combat.Listener {
        public final Setting.Choice sound = add(new Setting.Choice("Sound", 0, "Bell", "Pling", "Bass", "Bubble", "Glass", "Chime", "XP", "Little bell"));
        public final Setting.Num volume = add(new Setting.Num("Volume", 0.8, 0.1, 1.0, 0.05));
        public final Setting.Num pitch = add(new Setting.Num("Pitch", 1.0, 0.5, 2.0, 0.05));

        public HitSounds() { super("Hit Sounds", "A sound when your hit lands", Category.EFFECTS); Combat.listen(this); }

        static void play(Holder<SoundEvent> s, float pitch, float vol) { mcStatic().getSoundManager().play(SimpleSoundInstance.forUI(s.value(), pitch, vol)); }
        static void play(SoundEvent s, float pitch, float vol) { mcStatic().getSoundManager().play(SimpleSoundInstance.forUI(s, pitch, vol)); }
        static net.minecraft.client.Minecraft mcStatic() { return net.minecraft.client.Minecraft.getInstance(); }

        @Override public void onHit(LivingEntity t) {
            if (!enabled) return;
            float p = pitch.f() * (0.95f + Fx.RNG.nextFloat() * 0.1f), v = volume.f();
            switch (sound.index) {
                case 0 -> play(SoundEvents.BELL_BLOCK, p * 1.4f, v);
                case 1 -> play(SoundEvents.NOTE_BLOCK_PLING, p, v);
                case 2 -> play(SoundEvents.NOTE_BLOCK_BASS, p, v);
                case 3 -> play(SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, p, v);
                case 4 -> play(SoundEvents.GLASS_BREAK, p * 1.5f, v * 0.6f);
                case 5 -> play(SoundEvents.AMETHYST_BLOCK_CHIME, p, v);
                case 7 -> play(NOVA_BELL, p, v);
                default -> play(SoundEvents.EXPERIENCE_ORB_PICKUP, p, v);
            }
        }
    }

    // ------------------------------------------------------------------ muertes y totems
    public static final class KillEffect extends Module implements Combat.Listener {
        public final Setting.Choice style = add(new Setting.Choice("Style", 2, "Lightning", "Burst", "Both"));
        public final Setting.Choice shape = add(new Setting.Choice("Shape", 3, Fx.SHAPES));
        public final Setting.Bool sound = add(new Setting.Bool("Sound", true));
        public final Setting.Col color = add(new Setting.Col("Color", 0.13f, 0.75f));

        public KillEffect() { super("Kill Effect", "Lightning and particles when you kill", Category.EFFECTS); Combat.listen(this); }

        @Override public void onKill(LivingEntity t) {
            if (!enabled || mc().level == null) return;
            Vec3 c = center(t);
            if (style.index != 1) { // rayo solo visual (no hace daño ni se ve en el servidor)
                LightningBolt l = new LightningBolt(EntityType.LIGHTNING_BOLT, mc().level);
                l.setVisualOnly(true);
                l.snapTo(t.getX(), t.getY(), t.getZ());
                mc().level.addEntity(l);
            }
            if (style.index != 0) {
                Fx.burst(shape(shape), c, 30, 0.3, 0.3f, color.rgb(), 30);
                Fx.P p = Fx.spawn(Fx.Shape.RING, t.position().add(0, 0.05, 0), 0, 0, 0, 0.3f, color.rgb(), 20);
                p.flat = true; p.growth = 0.25f; p.drag = 1;
            }
            if (sound.value) HitSounds.play(SoundEvents.PLAYER_LEVELUP, 1.6f, 0.6f);
        }
    }

    /** Super muerto: rayos varios segundos sobre quien matas + full particulas; y 1 trueno cuando muere cualquiera. */
    public static final class SuperDeath extends Module implements Combat.Listener {
        public final Setting.Num seconds = add(new Setting.Num("Seconds", 3, 1, 6, 0.5));
        public final Setting.Bool anyDeathSound = add(new Setting.Bool("Thunder on any death", true));
        public final Setting.Col color = add(new Setting.Col("Color", 0.08f, 0.9f));

        private long stormUntil = 0;
        private Vec3 stormPos = null;
        private int tickCount = 0;
        private final java.util.Set<Integer> deadSeen = new java.util.HashSet<>();

        public SuperDeath() { super("Super Death", "Rayos cuando matas; un trueno cuando muere alguien", Category.EFFECTS); Combat.listen(this); }

        @Override public void onKill(LivingEntity t) {
            if (!enabled || mc().level == null) return;
            stormPos = t.position();
            stormUntil = System.currentTimeMillis() + (long) (seconds.f() * 1000);
            bolt(stormPos);
            Fx.burst(Fx.Shape.SPARK, center(t), 70, 0.6, 0.45f, color.rgb(), 45); // full cosas
        }

        @Override public void onTick() {
            var world = mc().level;
            if (world == null) { deadSeen.clear(); stormPos = null; return; }
            tickCount++;
            if (stormPos != null && System.currentTimeMillis() < stormUntil) { // tormenta sobre quien mataste
                if (tickCount % 5 == 0) {
                    double ox = (Fx.RNG.nextDouble() - 0.5) * 3, oz = (Fx.RNG.nextDouble() - 0.5) * 3;
                    bolt(stormPos.add(ox, 0, oz));
                    Fx.burst(Fx.Shape.SPARK, stormPos.add(0, 1, 0), 24, 0.4, 0.35f, color.rgb(), 30);
                }
            } else { stormPos = null; }
            if (anyDeathSound.value) { // 1 trueno cuando muere cualquier jugador (lo mates o no)
                for (var p : world.players()) {
                    int id = p.getId();
                    boolean dead = p.isDeadOrDying() || p.getHealth() <= 0;
                    if (dead && !deadSeen.contains(id)) { deadSeen.add(id); HitSounds.play(SoundEvents.LIGHTNING_BOLT_THUNDER, 1f, 0.9f); }
                    else if (!dead) deadSeen.remove(id);
                }
            }
        }

        private void bolt(Vec3 pos) {
            LightningBolt l = new LightningBolt(EntityType.LIGHTNING_BOLT, mc().level);
            l.setVisualOnly(true);
            l.snapTo(pos.x, pos.y, pos.z);
            mc().level.addEntity(l);
        }
    }

    /** Domo/onda rojo en el suelo al matar, estilo LyricsFlow (hecho con el sistema de efectos de Nova). */
    public static final class DeathDome extends Module implements Combat.Listener {
        public final Setting.Col color = add(new Setting.Col("Color", 0f, 0.85f)); // rojo
        public final Setting.Bool sound = add(new Setting.Bool("Sound", true));

        public DeathDome() { super("Death Dome", "Onda y domo rojo en el suelo al matar", Category.EFFECTS); Combat.listen(this); }

        @Override public void onKill(LivingEntity t) {
            if (!enabled || mc().level == null) return;
            int rgb = color.rgb();
            Vec3 c = t.position();
            // dos ondas que se expanden en el suelo
            Fx.P r1 = Fx.spawn(Fx.Shape.RING, c.add(0, 0.06, 0), 0, 0, 0, 0.6f, rgb, 22); r1.flat = true; r1.growth = 0.95f; r1.drag = 1;
            Fx.P r2 = Fx.spawn(Fx.Shape.RING, c.add(0, 0.06, 0), 0, 0, 0, 0.3f, rgb, 28); r2.flat = true; r2.growth = 0.6f; r2.drag = 1;
            // domo: resplandor subiendo en media esfera + chispas rojas hacia arriba
            Fx.burst(Fx.Shape.GLOW, c.add(0, 0.5, 0), 44, 0.55, 0.5f, rgb, 32);
            Fx.burst(Fx.Shape.SPARK, c.add(0, 0.2, 0), 40, 0.7, 0.3f, rgb, 28);
            if (sound.value) HitSounds.play(SoundEvents.GENERIC_EXPLODE, 1.4f, 0.5f);
        }
    }

    public static final class TotemEffect extends Module implements Combat.Listener {
        public final Setting.Choice shape = add(new Setting.Choice("Shape", 1, Fx.SHAPES));
        public final Setting.Bool notify = add(new Setting.Bool("Notification", true));
        public final Setting.Col color = add(new Setting.Col("Color", 0.3f, 0.75f));
        private final java.util.Map<String, Integer> pops = new java.util.HashMap<>();

        public TotemEffect() { super("Totem Pops", "Counts totems that pop and shows an effect", Category.EFFECTS); Combat.listen(this); }

        @Override public void onTotem(LivingEntity who) {
            if (!enabled) return;
            Fx.burst(shape(shape), center(who), 26, 0.25, 0.28f, color.rgb(), 30);
            String name = who.getName().getString();
            int n = pops.merge(name, 1, Integer::sum);
            if (notify.value) Notifications.push(name + " · " + n + " totem" + (n == 1 ? "" : "s"), color.rgb());
        }

        @Override public void onTick() { if (mc().level == null) pops.clear(); }
    }

    // ------------------------------------------------------------------ salto y ambiente
    public static final class JumpCircles extends Module {
        @Override public boolean ecoSkip() { return true; }
        public final Setting.Num size = add(new Setting.Num("Size", 1.4, 0.5, 3.0, 0.1));
        public final Setting.Bool rainbow = add(new Setting.Bool("Rainbow", false));
        public final Setting.Col color = add(new Setting.Col("Color", 0.55f, 0.65f)).showIf(() -> !rainbow.value);
        private boolean wasGround = true;

        public JumpCircles() { super("Jump Circles", "Rings on the ground when you jump", Category.EFFECTS); }

        @Override public void onTick() {
            var p = mc().player;
            if (p == null) return;
            boolean g = p.onGround();
            if (wasGround && !g && p.getDeltaMovement().y > 0.2) {
                for (int i = 0; i < 2; i++) {
                    Fx.P r = Fx.spawn(Fx.Shape.RING, p.position().add(0, 0.03 + i * 0.01, 0), 0, 0, 0, 0.2f, pick(rainbow, color), 22 + i * 6);
                    r.flat = true; r.drag = 1; r.growth = size.f() / (18 + i * 8);
                }
            }
            wasGround = g;
        }
    }

    public static final class AmbientParticles extends Module {
        @Override public boolean ecoSkip() { return true; }
        public final Setting.Choice shape = add(new Setting.Choice("Shape", 0, Fx.SHAPES));
        public final Setting.Num count = add(new Setting.Num("Count", 2, 1, 8, 1));
        public final Setting.Num radius = add(new Setting.Num("Radius", 6, 2, 16, 1));
        public final Setting.Bool rainbow = add(new Setting.Bool("Rainbow", false));
        public final Setting.Col color = add(new Setting.Col("Color", 0.15f, 0.7f)).showIf(() -> !rainbow.value);

        public AmbientParticles() { super("Ambient Particles", "Fireflies and stars floating around you", Category.EFFECTS); }

        @Override public void onTick() {
            var p = mc().player;
            if (p == null) return;
            for (int i = 0; i < count.i(); i++) {
                double r = radius.value;
                Vec3 pos = p.position().add((Fx.RNG.nextDouble() - 0.5) * 2 * r, Fx.RNG.nextDouble() * 3 - 0.5, (Fx.RNG.nextDouble() - 0.5) * 2 * r);
                Fx.P f = Fx.spawn(shape(shape), pos, (Fx.RNG.nextDouble() - 0.5) * 0.02, 0.01 + Fx.RNG.nextDouble() * 0.015, (Fx.RNG.nextDouble() - 0.5) * 0.02,
                        0.12f + Fx.RNG.nextFloat() * 0.12f, pick(rainbow, color), 60 + Fx.RNG.nextInt(60));
                f.drag = 0.995f;
            }
        }
    }

    // ------------------------------------------------------------------ marcas en el mundo
    public static final class TargetMarker extends Module {
        @Override public boolean ecoSkip() { return true; }
        public final Setting.Choice style = add(new Setting.Choice("Style", 0, "Ring", "Spiral", "Ghosts"));
        public final Setting.Col color = add(new Setting.Col("Color", 0.95f, 0.6f));

        public TargetMarker() { super("Target Marker", "Glowing marker around who you fight", Category.EFFECTS); }

        @Override public void onWorldAfter(dev.orbita.client.nova.gfx.WCtx ctx) {
            LivingEntity t = Combat.target();
            if (t == null || t.isDeadOrDying()) return;
            float pt = ctx.tickDelta();
            Vec3 cam = ctx.camera().position(), pos = t.getPosition(pt);
            Matrix4f m = new Matrix4f().translate((float) (pos.x - cam.x), (float) (pos.y - cam.y), (float) (pos.z - cam.z));
            if (ctx.matrixStack() != null) m = new Matrix4f(ctx.matrixStack().last().pose()).mul(m);
            double time = (System.currentTimeMillis() % 100000) / 50.0;
            float w = t.getBbWidth() * 0.8f + 0.2f, h = t.getBbHeight();
            int rgb = color.rgb();
            Gfx.begin();
            switch (style.index) {
                case 1 -> { // espiral que sube y baja
                    for (int k = 0; k < 2; k++) {
                        double prevA = 0; float py = 0;
                        for (int i = 0; i <= 40; i++) {
                            double a = time * 0.12 + k * Math.PI + i * 0.16;
                            float y = (float) ((Math.sin(time * 0.05) * 0.5 + 0.5) * h * 0.2 + i / 40f * h * 0.8);
                            if (i > 0) Gfx.segXZ(m, (float) Math.cos(prevA) * w, (float) Math.sin(prevA) * w, (float) Math.cos(a) * w, (float) Math.sin(a) * w, (py + y) / 2, 0.025f, Gfx.argb(rgb, (int) (40 + 200 * i / 40f)));
                            prevA = a; py = y;
                        }
                    }
                }
                case 2 -> { // "fantasmas": orbes que giran alrededor
                    for (int i = 0; i < 3; i++) {
                        double a = time * 0.15 + i * Math.PI * 2 / 3;
                        float y = h * 0.5f + (float) Math.sin(time * 0.1 + i) * h * 0.35f;
                        float x = (float) Math.cos(a) * w, z = (float) Math.sin(a) * w;
                        Gfx.ringXZ(m, y, 0.08f, 0.06f, 12, Gfx.argb(rgb, 230));
                        Gfx.ringXZ(m, y, 0.16f, 0.06f, 12, Gfx.argb(rgb, 60));
                        Gfx.segXZ(m, x - 0.01f, z, x + 0.01f, z, y, 0.1f, Gfx.argb(rgb, 200));
                    }
                }
                default -> { // anillo que sube y baja
                    float y = (float) (Math.sin(time * 0.08) * 0.5 + 0.5) * h;
                    Gfx.ringXZ(m, y, w, 0.035f, 40, Gfx.argb(rgb, 230));
                    Gfx.ringXZ(m, y, w, 0.12f, 40, Gfx.argb(rgb, 50));
                }
            }
            Gfx.end();
        }
    }

    public static final class Hitboxes extends Module {
        public final Setting.Bool fill = add(new Setting.Bool("Fill", true));
        public final Setting.Bool onlyLiving = add(new Setting.Bool("Only creatures", true));
        public final Setting.Col color = add(new Setting.Col("Color", 0.55f, 0.7f));

        public Hitboxes() { super("Hitboxes", "Colored boxes around entities", Category.EFFECTS); }

        @Override public void onWorldAfter(dev.orbita.client.nova.gfx.WCtx ctx) {
            var mc = mc();
            if (mc.level == null || ctx.consumers() == null) return;
            float pt = ctx.tickDelta();
            Vec3 cam = ctx.camera().position();
            var ms = ctx.matrixStack() != null ? ctx.matrixStack() : new com.mojang.blaze3d.vertex.PoseStack();
            int rgb = color.rgb();
            float r = ((rgb >> 16) & 255) / 255f, g = ((rgb >> 8) & 255) / 255f, b = (rgb & 255) / 255f;
            var lines = ctx.consumers().getBuffer(net.minecraft.client.renderer.rendertype.RenderTypes.lines());
            java.util.List<AABB> boxes = new java.util.ArrayList<>();
            for (Entity e : mc.level.entitiesForRendering()) {
                if (e == mc.player && mc.options.getCameraType().isFirstPerson()) continue;
                if (onlyLiving.value && !(e instanceof LivingEntity)) continue;
                if (e.distanceToSqr(cam) > 48 * 48 || e.isInvisible()) continue;
                Vec3 lp = e.getPosition(pt);
                AABB box = e.getBoundingBox().move(lp.subtract(e.position())).move(-cam.x, -cam.y, -cam.z);
                dev.orbita.client.nova.compat.V.drawBox(ms, lines, box, r, g, b, 1f);
                boxes.add(box);
            }
            if (fill.value && !boxes.isEmpty()) {
                Matrix4f m = ms.last().pose();
                Gfx.begin();
                for (AABB x : boxes) Gfx.box(m, (float) x.minX, (float) x.minY, (float) x.minZ, (float) x.maxX, (float) x.maxY, (float) x.maxZ, Gfx.argb(rgb, 28));
                Gfx.end();
            }
        }
    }

    public static final class BlockHighlight extends Module {
        public final Setting.Bool fill = add(new Setting.Bool("Fill", true));
        public final Setting.Col color = add(new Setting.Col("Color", 0.78f, 0.6f));

        public BlockHighlight() { super("Block Highlight", "Colored outline on the block you look at", Category.EFFECTS); }

        public static BlockHighlight on() {
            if (NovaClient.modules == null) return null;
            BlockHighlight b = NovaClient.modules.get(BlockHighlight.class);
            return b != null && b.enabled && !ClientSettings.extra() ? b : null;
        }

        @Override public void onWorldAfter(dev.orbita.client.nova.gfx.WCtx ctx) {
            var mc = mc();
            if (!fill.value || mc.level == null || !(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
            var state = mc.level.getBlockState(hit.getBlockPos());
            if (state.isAir()) return;
            var shape = state.getShape(mc.level, hit.getBlockPos());
            if (shape.isEmpty()) return;
            Vec3 cam = ctx.camera().position();
            AABB b = shape.bounds().move(hit.getBlockPos()).move(-cam.x, -cam.y, -cam.z).inflate(0.002);
            Matrix4f m = ctx.matrixStack() != null ? ctx.matrixStack().last().pose() : new Matrix4f();
            float pulse = 0.75f + 0.25f * (float) Math.sin(System.currentTimeMillis() / 300.0);
            Gfx.begin();
            Gfx.box(m, (float) b.minX, (float) b.minY, (float) b.minZ, (float) b.maxX, (float) b.maxY, (float) b.maxZ, Gfx.argb(color.rgb(), (int) (45 * pulse)));
            Gfx.end();
        }
    }

    // ------------------------------------------------------------------ mano y pantalla
    public static final class HandTweaks extends Module {
        public final Setting.Num x = add(new Setting.Num("X", 0, -1, 1, 0.02));
        public final Setting.Num y = add(new Setting.Num("Y", 0, -1, 1, 0.02));
        public final Setting.Num z = add(new Setting.Num("Z", 0, -1, 1, 0.02));
        public final Setting.Num scale = add(new Setting.Num("Size", 1.0, 0.3, 1.5, 0.05));

        public HandTweaks() { super("Hand Tweaks", "Move and resize the item in your hand", Category.EFFECTS); }

        public static HandTweaks on() {
            if (NovaClient.modules == null) return null;
            HandTweaks h = NovaClient.modules.get(HandTweaks.class);
            return h != null && h.enabled ? h : null;
        }
    }

    public static final class AspectRatio extends Module {
        public final Setting.Choice ratio = add(new Setting.Choice("Ratio", 1, "Normal", "4:3", "5:4", "16:10", "Custom"));
        public final Setting.Num custom = add(new Setting.Num("Stretch", 1.0, 0.5, 1.5, 0.01)).showIf(() -> ratio.index == 4);

        public AspectRatio() { super("Aspect Ratio", "Stretched screen like 4:3", Category.EFFECTS); }

        /** Factor que multiplica la proporcion de la pantalla (menos de 1 = estirado a lo ancho). */
        public static float factor(float aspect) {
            if (NovaClient.modules == null) return 1;
            AspectRatio a = NovaClient.modules.get(AspectRatio.class);
            if (a == null || !a.enabled) return 1;
            return switch (a.ratio.index) {
                case 1 -> (4f / 3f) / aspect;
                case 2 -> (5f / 4f) / aspect;
                case 3 -> (16f / 10f) / aspect;
                case 4 -> a.custom.f();
                default -> 1;
            };
        }
    }
}
