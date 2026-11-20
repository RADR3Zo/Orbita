package dev.orbita.client.nova.module;

import dev.orbita.client.nova.core.Setting;
import dev.orbita.client.nova.core.Theme;
import dev.orbita.client.nova.gui.Ui;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** Modulos que dibujan en pantalla. La posicion es un porcentaje de la pantalla (X, Y). */
public final class Hud {
    private Hud() { }

    static String[] pingLines() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) return new String[]{"- ms"};
        var e = mc.getConnection().getPlayerInfo(mc.player.getUUID());
        return new String[]{(e == null ? 0 : e.getLatency()) + " ms"};
    }

    static String[] coordLines() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return new String[]{"-"};
        return new String[]{"X: " + mc.player.getBlockX(), "Y: " + mc.player.getBlockY(), "Z: " + mc.player.getBlockZ()};
    }

    /** Cuantas flechas y perlas te quedan. */
    static String[] ammoLines() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return new String[]{"-"};
        var inv = mc.player.getInventory();
        int arrows = inv.countItem(net.minecraft.world.item.Items.ARROW) + inv.countItem(net.minecraft.world.item.Items.SPECTRAL_ARROW) + inv.countItem(net.minecraft.world.item.Items.TIPPED_ARROW);
        int pearls = inv.countItem(net.minecraft.world.item.Items.ENDER_PEARL);
        return new String[]{"Flechas " + arrows + "  Perlas " + pearls};
    }

    /** Hambre, saturacion y XP en numero. */
    static String[] foodLines() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return new String[]{"-"};
        var fm = mc.player.getFoodData();
        return new String[]{"Food " + fm.getFoodLevel() + "/20  Sat " + String.format("%.1f", fm.getSaturationLevel()), "XP " + mc.player.experienceLevel};
    }

    static String[] dirLines() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return new String[]{"-"};
        String d = mc.player.getDirection().getSerializedName();
        return new String[]{Character.toUpperCase(d.charAt(0)) + d.substring(1)};
    }

    static String[] clockLines() {
        return new String[]{LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))};
    }

    public abstract static class Base extends Module {
        public final Setting.Num x, y, scale;
        protected final Setting.Bool background;
        protected final Setting.Col color;
        public final int defaultX, defaultY;

        Base(String name, String desc, int dx, int dy, boolean defaultOn) {
            super(name, desc, Category.HUD);
            defaultX = dx; defaultY = dy;
            x = add(new Setting.Num("X", dx, 0, 100, 0.1));
            y = add(new Setting.Num("Y", dy, 0, 100, 0.1));
            scale = add(new Setting.Num("Size", 1.0, 0.5, 3.0, 0.05));
            background = add(new Setting.Bool("Background", true));
            color = add(new Setting.Col("Color", 0f, 1f));
            enabled = defaultOn;
        }

        protected abstract int width(Font tr);
        protected abstract int height(Font tr);
        protected abstract void draw(GuiGraphics ctx, Font tr, int px, int py);
        /** false = ahora no hay nada que mostrar (sin armadura, sin efectos...). */
        protected boolean hasContent() { return true; }

        protected int textColor() { return 0xFF000000 | (color.light >= 0.99f && color.hue == 0f ? 0xFFFFFF : color.rgb()); }

        /** Caja en pantalla: {x, y, ancho, alto} ya con el tamaño aplicado. */
        public float[] bounds(int screenW, int screenH, Font tr) {
            int w = hasContent() ? width(tr) : 64, h = hasContent() ? height(tr) : 20;
            float s = scale.f(), sw = w * s, sh = h * s;
            return new float[]{(float) ((screenW - sw) * x.value / 100.0), (float) ((screenH - sh) * y.value / 100.0), sw, sh};
        }

        /** Mueve la caja a (px, py) en pantalla y guarda la posicion como porcentaje. */
        public void moveTo(float px, float py, int screenW, int screenH, Font tr) {
            float[] b = bounds(screenW, screenH, tr);
            x.set(screenW - b[2] <= 0 ? 0 : px / (screenW - b[2]) * 100);
            y.set(screenH - b[3] <= 0 ? 0 : py / (screenH - b[3]) * 100);
        }

        /** Dibuja el elemento; en el editor, si no hay contenido, deja una caja con su nombre para poder moverlo. */
        public void render(GuiGraphics ctx, boolean editor) {
            Font tr = mc().font;
            float[] b = bounds(ctx.guiWidth(), ctx.guiHeight(), tr);
            float s = scale.f();
            dev.orbita.client.nova.compat.V.push(ctx);
            dev.orbita.client.nova.compat.V.translate(ctx, b[0], b[1]);
            dev.orbita.client.nova.compat.V.scale(ctx, s, s);
            if (!hasContent()) {
                if (editor) {
                    Ui.roundRect(ctx, 0, 0, 64, 20, 5, 0x50000000);
                    Ui.ctext(ctx, tr, dev.orbita.client.nova.core.Lang.t(name), 32, 6, 0xAAFFFFFF, false);
                }
            } else {
                int w = width(tr), h = height(tr);
                if (background.value) Ui.roundRect(ctx, 0, 0, w, h, 5, 0x70000000);
                draw(ctx, tr, 0, 0);
            }
            dev.orbita.client.nova.compat.V.pop(ctx);
        }

        @Override
        public void onHud(GuiGraphics ctx) {
            Minecraft mc = mc();
            if (mc.options.hideGui || mc.player == null || !hasContent()) return;
            // (en Órbita no se usa el editor de HUD de Nova)
            render(ctx, false);
        }
    }

    public static final class Text extends Base {
        private final Supplier<String[]> lines;

        public Text(String name, String desc, int dx, int dy, Supplier<String[]> lines, boolean on) {
            super(name, desc, dx, dy, on);
            this.lines = lines;
        }

        @Override protected int width(Font tr) {
            int w = 0;
            for (String s : lines.get()) w = Math.max(w, Ui.width(tr, s, false));
            return w + 8;
        }
        @Override protected int height(Font tr) { return lines.get().length * 10 + 4; }
        @Override protected void draw(GuiGraphics ctx, Font tr, int px, int py) {
            int i = 0;
            for (String s : lines.get()) Ui.text(ctx, tr, s, px + 5, py + 3 + 10 * i++, textColor(), false);
        }
    }

    public static final class Keystrokes extends Base {
        public Keystrokes() { super("Keystrokes", "Muestra WASD, espacio y clicks", 1, 55, true); }

        @Override protected int width(Font tr) { return 3 * 24 + 4; }
        @Override protected int height(Font tr) { return 98; }

        private void key(GuiGraphics ctx, Font tr, int x, int y, int w, int h, String label, boolean down) {
            Ui.roundRect(ctx, x, y, w, h, 6, down ? Theme.accent(220) : 0x70000000);
            int c = down ? Theme.onAccent() : textColor();
            Ui.text(ctx, tr, label, x + (w - Ui.width(tr, label, false)) / 2, y + (h - 8) / 2, c, false);
        }

        @Override protected void draw(GuiGraphics ctx, Font tr, int px, int py) {
            var o = mc().options;
            key(ctx, tr, px + 26, py + 2, 22, 22, "W", o.keyUp.isDown());
            key(ctx, tr, px + 2, py + 26, 22, 22, "A", o.keyLeft.isDown());
            key(ctx, tr, px + 26, py + 26, 22, 22, "S", o.keyDown.isDown());
            key(ctx, tr, px + 50, py + 26, 22, 22, "D", o.keyRight.isDown());
            key(ctx, tr, px + 2, py + 50, 34, 22, "LMB " + Cps.left(), o.keyAttack.isDown());
            key(ctx, tr, px + 38, py + 50, 34, 22, "RMB " + Cps.right(), o.keyUse.isDown());
            boolean jump = o.keyJump.isDown();
            key(ctx, tr, px + 2, py + 74, 70, 20, "", jump);
            ctx.fill(px + 25, py + 83, px + 49, py + 85, jump ? Theme.onAccent() : textColor()); // barra espaciadora
        }
    }

    public static final class Armor extends Base {
        private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

        public Armor() { super("Armor Status", "Armadura y durabilidad", 98, 40, true); }

        @Override protected boolean hasContent() { // sin armadura no se dibuja la caja vacia
            var p = mc().player;
            if (p == null) return false;
            for (EquipmentSlot slot : SLOTS) if (!p.getItemBySlot(slot).isEmpty()) return true;
            return false;
        }

        @Override protected int width(Font tr) { return 52; }
        @Override protected int height(Font tr) { return 4 * 18 + 2; }
        @Override protected void draw(GuiGraphics ctx, Font tr, int px, int py) {
            var p = mc().player;
            for (int i = 0; i < 4; i++) {
                ItemStack st = p.getItemBySlot(SLOTS[i]);
                int yy = py + 1 + i * 18;
                if (st.isEmpty()) continue;
                ctx.renderItem(st, px + 1, yy);
                if (st.isDamageableItem()) {
                    int left = st.getMaxDamage() - st.getDamageValue();
                    int pct = (int) (100L * left / st.getMaxDamage());
                    int col = pct > 50 ? 0xFF7CF08A : pct > 20 ? 0xFFFFE08A : 0xFFFF6A6A; // verde / amarillo / rojo
                    ctx.drawString(tr, Integer.toString(left), px + 20, yy + 4, col, true);
                }
            }
        }
    }

    /** Cuenta tus kills seguidas; se reinicia al morir o salir del mundo. */
    public static final class KillStreak extends Base implements Combat.Listener {
        private int streak = 0;
        private boolean wasDead = false;

        public KillStreak() { super("Kill Streak", "Tus kills seguidas", 1, 47, false); Combat.listen(this); }

        @Override public void onKill(net.minecraft.world.entity.LivingEntity t) { if (enabled) streak++; }
        @Override public void onTick() {
            var p = mc().player;
            if (mc().level == null || p == null) { streak = 0; wasDead = false; return; }
            boolean dead = p.isDeadOrDying() || p.getHealth() <= 0;
            if (dead && !wasDead) streak = 0; // al morir se reinicia la racha
            wasDead = dead;
        }

        private String text() { return "⚔ " + streak + (streak >= 2 ? " seguidas" : ""); }
        @Override protected boolean hasContent() { return true; }
        @Override protected int width(Font tr) { return Ui.width(tr, text(), false) + 8; }
        @Override protected int height(Font tr) { return 12; }
        @Override protected void draw(GuiGraphics ctx, Font tr, int px, int py) {
            int col = streak >= 10 ? 0xFFFF6A6A : streak >= 5 ? 0xFFFFD75A : textColor();
            Ui.text(ctx, tr, text(), px + 4, py + 2, col, false);
        }
    }

    /** La mira avisa (corchetes de color) cuando el rival esta a tu alcance de golpe. */
    public static final class ReachIndicator extends Module {
        private final Setting.Col color = add(new Setting.Col("Color", 0f, 0.85f)); // rojo

        public ReachIndicator() { super("Reach Indicator", "La mira avisa cuando el rival esta a tu alcance", Category.HUD); }

        @Override public void onHud(GuiGraphics ctx) {
            Minecraft mc = mc();
            if (mc.options.hideGui || mc.player == null || mc.level == null) return;
            if (!(mc.hitResult instanceof net.minecraft.world.phys.EntityHitResult ehr)) return;
            if (!(ehr.getEntity() instanceof net.minecraft.world.entity.LivingEntity)) return;
            int cx = ctx.guiWidth() / 2, cy = ctx.guiHeight() / 2;
            int col = 0xFF000000 | color.rgb();
            int d = 7, len = 4, th = 1; // corchetes en las 4 esquinas de la mira
            // arriba-izq
            ctx.fill(cx - d, cy - d, cx - d + len, cy - d + th, col); ctx.fill(cx - d, cy - d, cx - d + th, cy - d + len, col);
            // arriba-der
            ctx.fill(cx + d - len, cy - d, cx + d, cy - d + th, col); ctx.fill(cx + d - th, cy - d, cx + d, cy - d + len, col);
            // abajo-izq
            ctx.fill(cx - d, cy + d - th, cx - d + len, cy + d, col); ctx.fill(cx - d, cy + d - len, cx - d + th, cy + d, col);
            // abajo-der
            ctx.fill(cx + d - len, cy + d - th, cx + d, cy + d, col); ctx.fill(cx + d - th, cy + d - len, cx + d, cy + d, col);
        }
    }

    /** Letra de la musica sincronizada: el reproductor del launcher la escribe en nova-lyrics.json y aqui se muestra. */
    public static final class Lyrics extends Base {
        private static final String[] FONTS = {"greatvibes", "dancing", "sacramento", "damion", "allura", "pacifico", "parisienne", "kaushan"};
        public final Setting.Choice font = add(new Setting.Choice("Font", 0, "Great Vibes", "Dancing Script", "Sacramento", "Damion", "Allura", "Pacifico", "Parisienne", "Kaushan"));
        public final Setting.Bool big = add(new Setting.Bool("Big (center)", false)); // ademas del badge, la letra grande en el centro
        public final Setting.Bool front = add(new Setting.Bool("In front (3D)", false)); // las palabras flotando frente a ti en el mundo
        private static long lastRead;
        private static boolean playing;
        private static String line = "";
        // matrices del mundo para pasar un punto 3D a la pantalla (como los nametags)
        private static final org.joml.Matrix4f L_VIEW = new org.joml.Matrix4f(), L_PROJ = new org.joml.Matrix4f();
        private static net.minecraft.world.phys.Vec3 lcam = net.minecraft.world.phys.Vec3.ZERO;
        private static boolean lready;

        public Lyrics() { super("Lyrics", "Letra de la musica (reproductor del launcher)", 32, 2, true); }

        private static void poll() {
            long now = System.currentTimeMillis();
            if (now - lastRead < 250) return;
            lastRead = now;
            try {
                java.io.File f = new java.io.File(mc().gameDirectory, "nova-lyrics.json");
                if (!f.exists()) { playing = false; return; }
                var o = com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(f.toPath())).getAsJsonObject();
                playing = o.has("playing") && o.get("playing").getAsBoolean();
                line = o.has("line") && !o.get("line").isJsonNull() ? o.get("line").getAsString() : "";
            } catch (Throwable t) { playing = false; }
        }

        private net.minecraft.network.chat.Component styled() { return Ui.font(line, FONTS[Math.max(0, Math.min(font.index, FONTS.length - 1))]); }

        @Override protected boolean hasContent() { poll(); return playing && !line.isEmpty(); }
        @Override protected int width(Font tr) { return Math.max(60, (line.isEmpty() ? 0 : tr.width(styled())) + 36); }
        @Override protected int height(Font tr) { return 18; }
        @Override protected void draw(GuiGraphics ctx, Font tr, int px, int py) {
            int w = width(tr);
            Ui.roundRect(ctx, px, py, w, 18, 9, 0xC00D0C14);
            Ui.outline(ctx, px, py, w, 18, 9, Theme.accent(130));
            long t = System.currentTimeMillis(); // barritas de musica animadas
            for (int i = 0; i < 4; i++) {
                int bh = 3 + (int) (6 * (0.5 + 0.5 * Math.sin(t / 150.0 + i * 0.9)));
                int bx = px + 9 + i * 4;
                ctx.fill(bx, py + 14 - bh, bx + 2, py + 14, Theme.light());
            }
            ctx.drawString(tr, styled(), px + 30, py + 5, 0xFFFFFFFF, true);
        }

        @Override public void onHud(GuiGraphics ctx) {
            super.onHud(ctx); // el badge normal
            if (!big.value) return; // ademas, la letra grande en el centro (estilo karaoke)
            Minecraft mc = mc();
            if (mc.options.hideGui) return;
            poll();
            if (!playing || line.isEmpty()) return;
            Font tr = mc.font;
            net.minecraft.network.chat.Component txt = styled();
            int w = tr.width(txt);
            dev.orbita.client.nova.compat.V.push(ctx);
            dev.orbita.client.nova.compat.V.translate(ctx, ctx.guiWidth() / 2f, ctx.guiHeight() * 0.70f);
            dev.orbita.client.nova.compat.V.scale(ctx, 2.2f, 2.2f);
            ctx.drawString(tr, txt, -w / 2, 0, 0xFFFFFFFF, true);
            dev.orbita.client.nova.compat.V.pop(ctx);
            if (front.value) drawFront(ctx, mc, tr);
        }

        /** Guarda las matrices del mundo de este cuadro para poder proyectar puntos 3D a la pantalla. */
        @Override public void onWorldAfter(dev.orbita.client.nova.gfx.WCtx ctx) {
            if (!front.value) return;
            L_VIEW.set(ctx.positionMatrix());
            L_PROJ.set(ctx.projectionMatrix());
            lcam = ctx.camera().position();
            lready = true;
        }

        private static float[] project(net.minecraft.world.phys.Vec3 p, int sw, int sh) {
            org.joml.Vector4f v = new org.joml.Vector4f((float) (p.x - lcam.x), (float) (p.y - lcam.y), (float) (p.z - lcam.z), 1f);
            L_VIEW.transform(v);
            L_PROJ.transform(v);
            if (v.w <= 0.05f) return null; // detras de la camara
            float nx = v.x / v.w, ny = v.y / v.w;
            return new float[]{(nx * 0.5f + 0.5f) * sw, (1f - (ny * 0.5f + 0.5f)) * sh};
        }

        /** Dibuja cada palabra de la letra flotando en el mundo, adelante del jugador (como LyricsFlow). */
        private void drawFront(GuiGraphics ctx, Minecraft mc, Font tr) {
            if (!lready || mc.level == null || mc.player == null) return;
            String[] words = line.trim().split("\\s+");
            if (words.length == 0 || words[0].isEmpty()) return;
            float td = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
            net.minecraft.world.phys.Vec3 eye = mc.player.getEyePosition(td);
            double yaw = Math.toRadians(mc.player.getViewYRot(td));
            net.minecraft.world.phys.Vec3 fwd = new net.minecraft.world.phys.Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
            net.minecraft.world.phys.Vec3 right = new net.minecraft.world.phys.Vec3(Math.cos(yaw), 0, Math.sin(yaw));
            int sw = ctx.guiWidth(), sh = ctx.guiHeight();
            String fnt = FONTS[Math.max(0, Math.min(font.index, FONTS.length - 1))];
            long seed = line.hashCode();
            for (int i = 0; i < words.length; i++) {
                java.util.Random rnd = new java.util.Random(seed * 31L + i);
                double dist = 4 + rnd.nextDouble() * 5;      // 4..9 bloques adelante
                double side = (rnd.nextDouble() - 0.5) * 7;  // izquierda / derecha
                double up = -1.2 + rnd.nextDouble() * 2.6;   // altura
                net.minecraft.world.phys.Vec3 anchor = eye.add(fwd.scale(dist)).add(right.scale(side)).add(0, up, 0);
                float[] s = project(anchor, sw, sh);
                if (s == null) continue;
                float scale = (float) (10.0 / Math.max(2.0, anchor.distanceTo(lcam))); // perspectiva: cerca mas grande
                net.minecraft.network.chat.Component wtxt = Ui.font(words[i], fnt);
                int w = tr.width(wtxt);
                dev.orbita.client.nova.compat.V.push(ctx);
                dev.orbita.client.nova.compat.V.translate(ctx, s[0], s[1]);
                dev.orbita.client.nova.compat.V.scale(ctx, scale, scale);
                ctx.drawString(tr, wtxt, -w / 2, -4, 0xFFFFFFFF, true);
                dev.orbita.client.nova.compat.V.pop(ctx);
            }
        }
    }

    public static final class Potions extends Base {
        public Potions() { super("Potion Effects", "Efectos activos y su tiempo", 1, 70, true); }

        private List<String> lines() {
            List<String> l = new ArrayList<>();
            var p = mc().player;
            if (p == null) return l;
            Utility.Fullbright fb = dev.orbita.client.nova.NovaClient.modules.get(Utility.Fullbright.class);
            for (MobEffectInstance e : p.getActiveEffects()) {
                if (fb != null && fb.enabled && e.isInfiniteDuration() && e.getEffect() == net.minecraft.world.effect.MobEffects.NIGHT_VISION) continue; // la pone Fullbright
                int secs = e.isInfiniteDuration() ? -1 : e.getDuration() / 20;
                String t = secs < 0 ? "**" : String.format("%d:%02d", secs / 60, secs % 60);
                l.add(e.getEffect().value().getDisplayName().getString() + " " + (e.getAmplifier() + 1) + "  " + t);
            }
            return l;
        }

        @Override protected boolean hasContent() { return !lines().isEmpty(); }
        @Override protected int width(Font tr) {
            int w = 0;
            for (String s : lines()) w = Math.max(w, Ui.width(tr, s, false));
            return w + 8;
        }
        @Override protected int height(Font tr) { return lines().size() * 10 + 4; }
        @Override protected void draw(GuiGraphics ctx, Font tr, int px, int py) {
            int i = 0;
            for (String s : lines()) Ui.text(ctx, tr, s, px + 5, py + 3 + 10 * i++, textColor(), false);
        }
    }

    // ------------------------------------------------------------------ estilo Vape / Prestige (solo visual)

    /** Color del degradado en la posicion k (0..1) segun el estilo: tema, arcoiris o fijo. */
    static int gradient(int style, float k, int fixedRgb) {
        double t = (System.currentTimeMillis() % 4000) / 4000.0;
        return switch (style) {
            case 1 -> 0xFF000000 | (java.awt.Color.HSBtoRGB((float) ((k * 0.6 + t) % 1.0), 0.55f, 1f) & 0xFFFFFF);
            case 2 -> 0xFF000000 | fixedRgb;
            default -> {
                double w = 0.5 + 0.5 * Math.sin((k * 2.2 - t * 2) * Math.PI);
                yield 0xFF000000 | Ui.mix(Theme.accent(), Theme.light(), (float) w) & 0xFFFFFF;
            }
        };
    }

    /** Lista de modulos encendidos, ordenada por largo, con degradado y animacion al entrar y salir. */
    public static final class ModuleList extends Base {
        public final Setting.Choice style = add(new Setting.Choice("Style", 0, "Theme", "Rainbow", "Static"));
        public final Setting.Bool sideBar = add(new Setting.Bool("Side bar", true));
        public final Setting.Bool showInfo = add(new Setting.Bool("Show info", true));
        public final Setting.Bool cosmetics = add(new Setting.Bool("Show cosmetics", false));
        private final java.util.Map<Module, Float> slide = new java.util.HashMap<>();
        private long last = System.currentTimeMillis();

        public ModuleList() { super("Module List", "Active modules, Vape style (visual only)", 100, 0, true); background.value = true; }

        private record Line(Module m, String name, String info, int width, float p) { }

        private List<Line> lines(Font tr) {
            long now = System.currentTimeMillis();
            float step = Math.min(1f, (now - last) / 180f);
            last = now;
            List<Line> out = new ArrayList<>();
            if (dev.orbita.client.nova.NovaClient.modules == null) return out;
            for (Module m : dev.orbita.client.nova.NovaClient.modules.all) {
                boolean show = m.enabled && !m.alwaysOn && m.category != Category.HUD && m.category != Category.SETTINGS
                        && (cosmetics.value || m.category != Category.COSMETICS) && !(m instanceof Emotes);
                float p = slide.getOrDefault(m, 0f);
                p = show ? Math.min(1f, p + step) : Math.max(0f, p - step);
                slide.put(m, p);
                if (p <= 0) continue;
                String name = dev.orbita.client.nova.core.Lang.t(m.name);
                String info = showInfo.value ? m.info() : null;
                int w = Ui.width(tr, name, true) + (info != null ? 4 + Ui.width(tr, info, false) : 0);
                out.add(new Line(m, name, info, w, p));
            }
            out.sort((a, b) -> b.width - a.width);
            return out;
        }

        @Override protected boolean hasContent() { return !lines(mc().font).isEmpty(); }
        @Override protected int width(Font tr) { int w = 0; for (Line l : lines(tr)) w = Math.max(w, l.width); return w + 10; }
        @Override protected int height(Font tr) { float h = 0; for (Line l : lines(tr)) h += 11 * ease(l.p); return Math.max(1, Math.round(h)); }

        private static float ease(float p) { return 1 - (1 - p) * (1 - p); }

        @Override public void render(GuiGraphics ctx, boolean editor) { // dibujo propio: cada linea con su fondo
            Font tr = mc().font;
            List<Line> ls = lines(tr);
            if (ls.isEmpty()) { super.render(ctx, editor); return; }
            float[] b = bounds(ctx.guiWidth(), ctx.guiHeight(), tr);
            boolean right = x.value >= 50;
            float s = scale.f();
            int boxW = width(tr);
            dev.orbita.client.nova.compat.V.push(ctx);
            dev.orbita.client.nova.compat.V.translate(ctx, b[0], b[1]);
            dev.orbita.client.nova.compat.V.scale(ctx, s, s);
            float y = 0;
            int i = 0;
            for (Line l : ls) {
                float e = ease(l.p);
                int w = l.width + 10, h = 11;
                int lx = right ? boxW - w + Math.round((1 - e) * (w + 4)) : -Math.round((1 - e) * (w + 4));
                int ly = Math.round(y);
                int col = gradient(style.index, i / (float) Math.max(1, ls.size()), color.rgb());
                if (background.value) ctx.fill(lx, ly, lx + w, ly + h, Ui.alpha(0x90000000, e));
                if (sideBar.value) {
                    if (right) ctx.fill(lx + w - 2, ly, lx + w, ly + h, Ui.alpha(col, e));
                    else ctx.fill(lx, ly, lx + 2, ly + h, Ui.alpha(col, e));
                }
                int tx = right ? lx + 4 : lx + 5;
                Ui.text(ctx, tr, l.name, tx, ly + 2, Ui.alpha(col, e), true, true);
                if (l.info != null) Ui.text(ctx, tr, l.info, tx + Ui.width(tr, l.name, true) + 4, ly + 2, Ui.alpha(0xFFAAAAAA, e), false, true);
                y += h * e;
                i++;
            }
            dev.orbita.client.nova.compat.V.pop(ctx);
        }

        @Override protected void draw(GuiGraphics ctx, Font tr, int px, int py) { }
    }

    /** Marca de agua "Nova" con degradado, FPS y hora. */
    public static final class Watermark extends Base {
        public final Setting.Choice style = add(new Setting.Choice("Style", 0, "Theme", "Rainbow", "Static"));

        public final Setting.Bool showFps = add(new Setting.Bool("FPS", true));
        public final Setting.Bool showPing = add(new Setting.Bool("Ping", false));
        public final Setting.Bool showMemory = add(new Setting.Bool("Memory", false));
        public final Setting.Bool showServer = add(new Setting.Bool("Server", false));
        public final Setting.Bool showUser = add(new Setting.Bool("User", false));
        public final Setting.Bool showTime = add(new Setting.Bool("Clock", true));

        public Watermark() { super("Watermark", "Nova logo with FPS and time", 50, 0, true); }

        private String stats() {
            var mc = mc();
            StringBuilder b = new StringBuilder();
            if (showUser.value) b.append("  ").append(mc.getUser().getName());
            if (showFps.value) b.append("  ").append(mc.getFps()).append(" FPS");
            if (showPing.value) b.append("  ").append(pingLines()[0]);
            if (showMemory.value) {
                Runtime r = Runtime.getRuntime();
                b.append("  ").append((r.totalMemory() - r.freeMemory()) >> 20).append("/").append(r.maxMemory() >> 20).append(" MB");
            }
            if (showServer.value) b.append("  ").append(mc.getCurrentServer() != null ? mc.getCurrentServer().ip : dev.orbita.client.nova.core.Lang.t("Singleplayer"));
            if (showTime.value) b.append("  ").append(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
            return b.toString();
        }

        @Override protected int width(Font tr) { return 22 + Ui.width(tr, ClientSettings.shortName(), true) + Ui.width(tr, stats(), false) + 8; }
        @Override protected int height(Font tr) { return 16; }

        @Override protected void draw(GuiGraphics ctx, Font tr, int px, int py) {
            double t = System.currentTimeMillis() / 1000.0;
            Ui.logo(ctx, px + 10, py + 8, 5.5, t * 0.6, gradient(style.index, 0, color.rgb()));
            int x = px + 20;
            String word = ClientSettings.shortName();
            for (int i = 0; i < word.length(); i++) { // degradado letra por letra
                String ch = word.substring(i, i + 1);
                Ui.text(ctx, tr, ch, x, py + 4, gradient(style.index, i / 6f, color.rgb()), true, true);
                x += Ui.width(tr, ch, true);
            }
            Ui.text(ctx, tr, stats(), x, py + 4, 0xFFD6D4E6, false, true);
            ctx.fill(px + 3, py + 15, px + width(tr) - 3, py + 16, gradient(style.index, 0.5f, color.rgb()));
        }
    }

    // ------------------------------------------------------------------ mas elementos del HUD

    static String[] speedLines() {
        var p = Minecraft.getInstance().player;
        if (p == null) return new String[]{"- BPS"};
        double v = Math.sqrt(Math.pow(p.getX() - p.xo, 2) + Math.pow(p.getZ() - p.zo, 2)) * 20;
        return new String[]{String.format("%.1f BPS", v)};
    }

    static String[] tpsLines() {
        var mc = Minecraft.getInstance();
        if (mc.isLocalServer() && mc.getSingleplayerServer() != null) return new String[]{String.format("%.1f TPS", Math.min(20, 1000.0 / Math.max(50, mc.getSingleplayerServer().getCurrentSmoothedTickTime())))};
        return new String[]{String.format("%.1f TPS", Combat.tps())};
    }

    /** Ficha del rival: cabeza, nombre, vida con barra animada y armadura. */
    public static final class TargetHud extends Base {
        private float shownHealth = -1, flash;
        private LivingEntityRef last = new LivingEntityRef();
        private static final class LivingEntityRef { net.minecraft.world.entity.LivingEntity e; }

        public TargetHud() { super("Target HUD", "Info of who you are fighting", 50, 62, true); }

        @Override protected boolean hasContent() { return Combat.target() != null; }
        @Override protected int width(Font tr) { return 150; }
        @Override protected int height(Font tr) { return 44; }

        @Override protected void draw(GuiGraphics ctx, Font tr, int px, int py) {
            var t = Combat.target();
            if (t == null) return;
            if (last.e != t) { last.e = t; shownHealth = t.getHealth(); }
            float hp = t.getHealth(), max = Math.max(1, t.getMaxHealth()), abs = t.getAbsorptionAmount();
            if (hp < shownHealth - 0.01f) flash = 1;
            shownHealth += (hp - shownHealth) * 0.12f;
            flash *= 0.9f;
            // cabeza (jugadores: su skin; otros: inicial)
            if (t instanceof net.minecraft.client.player.AbstractClientPlayer pl) {
                net.minecraft.client.gui.components.PlayerFaceRenderer.draw(ctx, pl.getSkin(), px + 6, py + 6, 32);
                if (t.hurtTime > 0) ctx.fill(px + 6, py + 6, px + 38, py + 38, ((int) (t.hurtTime * 12) << 24) | 0xFF3030);
            } else {
                Ui.roundRect(ctx, px + 6, py + 6, 32, 32, 8, Theme.accent(90));
                Ui.ctext(ctx, tr, t.getName().getString().substring(0, 1), px + 22, py + 18, 0xFFFFFFFF, true);
            }
            Ui.text(ctx, tr, t.getName().getString(), px + 44, py + 7, 0xFFFFFFFF, true, false);
            String hpText = String.format("%.1f", hp / 2) + " \u2764" + (abs > 0 ? String.format("  +%.0f", abs / 2) : "");
            Ui.text(ctx, tr, hpText, px + 44, py + 18, 0xFFD6D4E6, false, false);
            // barra de vida: la parte que se pierde baja despacio
            int bx = px + 44, by = py + 31, bw = 100, bh = 6;
            Ui.roundRect(ctx, bx, by, bw, bh, 3, 0x50FFFFFF);
            int lost = Math.round(bw * Math.min(1, shownHealth / max)), now = Math.round(bw * Math.min(1, hp / max));
            if (lost > now) Ui.roundRect(ctx, bx, by, Math.max(6, lost), bh, 3, Ui.mix(0xFFFFE08A, 0xFFFFFFFF, flash));
            // color de la barra segun si le ganas en corazones (verde), pierdes (rojo) o van iguales (color del tema)
            int lc = Theme.light(), ac = Theme.accent();
            var me = mc().player;
            if (me != null) {
                float mine = me.getHealth() + me.getAbsorptionAmount(), his = hp + abs;
                if (mine > his + 0.01f) { lc = 0xFF9BF6A8; ac = 0xFF2FBF78; }       // vas ganando
                else if (his > mine + 0.01f) { lc = 0xFFFFA3A3; ac = 0xFFD64545; }  // vas perdiendo
                float dh = (mine - his) / 2f; // por cuantos corazones le ganas (+) o pierdes (-)
                if (Math.abs(dh) >= 0.05f) {
                    String mtxt = (dh > 0 ? "+" : "-") + String.format("%.1f", Math.abs(dh)) + " ❤";
                    Ui.rtext(ctx, tr, mtxt, px + 146, py + 18, dh > 0 ? 0xFF7CF08A : 0xFFFF6A6A, true);
                }
            }
            if (now > 0) Ui.roundRectV(ctx, bx, by, Math.max(6, now), bh, 3, lc, ac);
            // armadura pequeña a la derecha
            int ax = px + 150 - 4;
            for (var slot : new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET}) {
                ItemStack st = t.getItemBySlot(slot);
                if (st.isEmpty()) continue;
                ax -= 12;
                dev.orbita.client.nova.compat.V.push(ctx);
                dev.orbita.client.nova.compat.V.translate(ctx, ax, py + 5);
                dev.orbita.client.nova.compat.V.scale(ctx, 0.7f, 0.7f);
                ctx.renderItem(st, 0, 0);
                dev.orbita.client.nova.compat.V.pop(ctx);
            }
        }
    }

    /** Overlay con la armadura (y la mano) del rival y su durabilidad en numeros. */
    public static final class TargetArmor extends Base {
        private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND};

        public TargetArmor() { super("Target Armor", "Armadura del rival y su durabilidad", 50, 40, true); }

        private net.minecraft.world.entity.LivingEntity who() { return Combat.target(); }

        @Override protected boolean hasContent() {
            var t = who();
            if (t == null) return false;
            for (EquipmentSlot slot : SLOTS) if (!t.getItemBySlot(slot).isEmpty()) return true;
            return false;
        }

        // cuantas gaps (manzana de oro normal o encantada) lleva el rival en las manos
        private static int gaps(net.minecraft.world.entity.LivingEntity t) {
            int n = 0;
            for (var hand : new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND}) {
                ItemStack st = t.getItemBySlot(hand);
                if (st.is(net.minecraft.world.item.Items.GOLDEN_APPLE) || st.is(net.minecraft.world.item.Items.ENCHANTED_GOLDEN_APPLE)) n += st.getCount();
            }
            return n;
        }

        @Override protected int width(Font tr) { return 54; }
        @Override protected int height(Font tr) { var t = who(); return 5 * 18 + 2 + (t != null && gaps(t) > 0 ? 12 : 0); }

        @Override protected void draw(GuiGraphics ctx, Font tr, int px, int py) {
            var t = who();
            if (t == null) return;
            int row = 0;
            for (EquipmentSlot slot : SLOTS) {
                ItemStack st = t.getItemBySlot(slot);
                if (st.isEmpty()) continue;
                int yy = py + 1 + row * 18; row++;
                ctx.renderItem(st, px + 1, yy);
                dev.orbita.client.nova.compat.V.stackOverlay(ctx, tr, st, px + 1, yy); // cantidad (p. ej. flechas)
                if (st.isDamageableItem()) {
                    int left = st.getMaxDamage() - st.getDamageValue();
                    ctx.drawString(tr, Integer.toString(left), px + 20, yy + 4, 0xFFFFFFFF, true); // rival: siempre en blanco
                }
            }
            int g = gaps(t);
            if (g > 0) Ui.text(ctx, tr, "Gaps: " + g, px + 2, py + 5 * 18 + 2, 0xFFFFD75A, true, true);
        }
    }

    /** Cuanto falta para volver a usar perlas, chorus, escudo, etc. */
    public static final class Cooldowns extends Base {
        public Cooldowns() { super("Cooldowns", "Item cooldowns (pearl, chorus...)", 70, 75, true); }

        private List<ItemStack> items() {
            List<ItemStack> l = new ArrayList<>();
            var p = mc().player;
            if (p == null) return l;
            java.util.Set<net.minecraft.world.item.Item> seen = new java.util.HashSet<>();
            for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                ItemStack s = p.getInventory().getItem(i);
                if (!s.isEmpty() && seen.add(s.getItem()) && dev.orbita.client.nova.compat.V.coolingDown(p, s)) l.add(s);
            }
            return l;
        }

        @Override protected boolean hasContent() { return !items().isEmpty(); }
        @Override protected int width(Font tr) { return 96; }
        @Override protected int height(Font tr) { return items().size() * 20 + 4; }
        @Override protected void draw(GuiGraphics ctx, Font tr, int px, int py) {
            var p = mc().player;
            float pt = mc().getDeltaTracker().getGameTimeDeltaPartialTick(false);
            int y = py + 2;
            for (ItemStack s : items()) {
                float k = dev.orbita.client.nova.compat.V.cooldown(p, s, pt);
                ctx.renderItem(s, px + 3, y + 1);
                Ui.roundRect(ctx, px + 24, y + 7, 66, 5, 2, 0x50FFFFFF);
                if (k > 0) Ui.roundRect(ctx, px + 24, y + 7, Math.max(4, Math.round(66 * k)), 5, 2, Theme.accent());
                y += 20;
            }
        }
    }

    /** Cuantos totems, manzanas, perlas, flechas... te quedan. */
    public static final class Consumables extends Base {
        private static final net.minecraft.world.item.Item[] ITEMS = {
                net.minecraft.world.item.Items.TOTEM_OF_UNDYING, net.minecraft.world.item.Items.GOLDEN_APPLE, net.minecraft.world.item.Items.ENCHANTED_GOLDEN_APPLE,
                net.minecraft.world.item.Items.ENDER_PEARL, net.minecraft.world.item.Items.CHORUS_FRUIT, net.minecraft.world.item.Items.EXPERIENCE_BOTTLE,
                net.minecraft.world.item.Items.ARROW, net.minecraft.world.item.Items.SPECTRAL_ARROW, net.minecraft.world.item.Items.TIPPED_ARROW,
                net.minecraft.world.item.Items.WIND_CHARGE, net.minecraft.world.item.Items.SNOWBALL, net.minecraft.world.item.Items.EGG};

        public Consumables() { super("Consumables", "How many totems, apples, pearls... you have", 98, 76, false); }

        private List<ItemStack> counts() {
            List<ItemStack> l = new ArrayList<>();
            var p = mc().player;
            if (p == null) return l;
            for (var it : ITEMS) {
                int n = p.getInventory().countItem(it);
                if (n > 0) l.add(new ItemStack(it, n));
            }
            return l;
        }

        @Override protected boolean hasContent() { return !counts().isEmpty(); }
        @Override protected int width(Font tr) { return counts().size() * 20 + 4; }
        @Override protected int height(Font tr) { return 22; }
        @Override protected void draw(GuiGraphics ctx, Font tr, int px, int py) {
            int x = px + 2;
            for (ItemStack s : counts()) {
                ctx.renderItem(s, x + 1, py + 3);
                dev.orbita.client.nova.compat.V.stackOverlay(ctx, tr, s, x + 1, py + 3, Integer.toString(s.getCount()));
                x += 20;
            }
        }
    }

    /** Tu inventario (las 27 casillas) siempre a la vista. */
    public static final class InventoryHud extends Base {
        public InventoryHud() { super("Inventory HUD", "Your inventory on screen", 98, 98, false); }

        @Override protected int width(Font tr) { return 9 * 18 + 6; }
        @Override protected int height(Font tr) { return 3 * 18 + 6; }
        @Override protected void draw(GuiGraphics ctx, Font tr, int px, int py) {
            var p = mc().player;
            if (p == null) return;
            for (int i = 0; i < 27; i++) { // casillas de 18 px como el inventario normal, cada objeto centrado en la suya
                int x = px + 3 + (i % 9) * 18, y = py + 3 + (i / 9) * 18;
                ctx.fill(x, y, x + 18, y + 18, 0x22FFFFFF);
                ctx.fill(x + 1, y + 1, x + 17, y + 17, 0x30000000);
            }
            for (int i = 0; i < 27; i++) {
                ItemStack s = p.getInventory().getItem(9 + i);
                if (s.isEmpty()) continue;
                int x = px + 4 + (i % 9) * 18, y = py + 4 + (i / 9) * 18;
                ctx.renderItem(s, x, y);
                dev.orbita.client.nova.compat.V.stackOverlay(ctx, tr, s, x, y);
            }
        }
    }
}
