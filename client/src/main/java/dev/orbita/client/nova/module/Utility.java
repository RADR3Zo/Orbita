package dev.orbita.client.nova.module;

import com.mojang.blaze3d.platform.InputConstants;
import dev.orbita.client.nova.NovaClient;
import dev.orbita.client.nova.core.Setting;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.lwjgl.glfw.GLFW;

public final class Utility {
    private Utility() { }

    public static final class ToggleSprint extends Module {
        public ToggleSprint() { super("Toggle Sprint", "Corres sin mantener la tecla", Category.UTILITY); }

        @Override public void onTick() {
            var mc = mc();
            if (mc.player != null && mc.screen == null && mc.options.keyUp.isDown()) {
                mc.options.keySprint.setDown(true);
            }
        }
    }

    public static final class Zoom extends Module {
        public final Setting.Num factor = add(new Setting.Num("Zoom", 4, 1.5, 10, 0.5));
        public final Setting.Bool smooth = add(new Setting.Bool("Smooth", true));
        private float current = 1f;

        public Zoom() { super("Zoom", "Mantén C para acercar la vista", Category.UTILITY); enabled = true; }
        @Override public String info() { return factor.value + "x"; }

        /**
         * Se lee la tecla directamente: en Minecraft la C ya es "guardar barra de objetos" y solo una de las dos
         * acciones recibe la tecla, asi que KeyBinding.isPressed() podia no enterarse nunca.
         */
        public static boolean held() {
            var mc = Minecraft.getInstance();
            if (mc.screen != null || mc.getWindow() == null) return false;
            InputConstants.Key key = KeyBindingHelper.getBoundKeyOf(NovaClient.zoomKey);
            if (key.equals(InputConstants.UNKNOWN)) return false;
            long win = mc.getWindow().handle();
            if (key.getType() == InputConstants.Type.MOUSE) return GLFW.glfwGetMouseButton(win, key.getValue()) == GLFW.GLFW_PRESS;
            return InputConstants.isKeyDown(mc.getWindow(), key.getValue());
        }

        @Override public void onTick() {
            float target = held() ? 1f / factor.f() : 1f;
            current = smooth.value ? current + (target - current) * 0.35f : target;
        }

        /** Multiplicador de FOV (1 = sin zoom). */
        public float fovMultiplier() { return held() || current < 0.999f ? current : 1f; }
    }

    public static final class Fullbright extends Module {
        public Fullbright() { super("Fullbright", "Ve todo con luz maxima", Category.UTILITY); }

        @Override public void onTick() {
            var p = mc().player;
            if (p == null) return;
            var cur = p.getEffect(MobEffects.NIGHT_VISION);
            if (cur == null || !cur.isInfiniteDuration()) {
                p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, MobEffectInstance.INFINITE_DURATION, 0, false, false, false));
            }
        }

        @Override public void onDisable() {
            var p = mc().player;
            if (p != null) p.removeEffect(MobEffects.NIGHT_VISION);
        }
    }

    public static final class TimeChanger extends Module {
        public final Setting.Num time = add(new Setting.Num("Time", 6000, 0, 24000, 500));

        public TimeChanger() { super("Time Changer", "Cambia la hora (solo visual)", Category.UTILITY); }

        @Override public void onTick() {
            var w = Minecraft.getInstance().level;
            if (w != null) w.getLevelData().setDayTime((long) time.value); // via Properties: existe en todas las 1.21.x
        }
    }

    /**
     * Buscador de libros encantados: escribes el nombre del encantamiento y, al abrir un cofre/inventario,
     * los libros (o items) que coinciden se pintan de verde. Solo visual, en tu pantalla.
     */
    public static final class BookFinder extends Module {
        public static final int SLOTS = 30; // hasta 30 encantamientos a la vez
        public final Setting.Str[] slots;
        public final Setting.Bool[] on; // cada casilla se puede apagar sin borrar su texto
        public final Setting.Action clear;

        public BookFinder() {
            super("Book Finder", "Marca en verde los libros encantados: un encantamiento por casilla, hasta 30 a la vez (puedes apagar casillas sin borrarlas)", Category.UTILITY);
            slots = new Setting.Str[SLOTS];
            on = new Setting.Bool[SLOTS];
            for (int i = 0; i < SLOTS; i++) {
                slots[i] = add(new Setting.Str("Book " + (i + 1), "", 40));
                on[i] = add(new Setting.Bool("Use book " + (i + 1), true)); // apagarlo = no se busca, pero el texto queda
            }
            clear = add(new Setting.Action("Clear all", "Clear", () -> { for (Setting.Str s : slots) s.value = ""; }));
        }

        private static BookFinder inst;
        public static BookFinder get() {
            if (inst == null && NovaClient.modules != null) inst = NovaClient.modules.get(BookFinder.class);
            return inst;
        }

        /** Lista de nombres a buscar, cada uno normalizado (minusculas, sin tildes); vacia si no hay nada que buscar. */
        public java.util.List<String> terms() {
            java.util.List<String> out = new java.util.ArrayList<>();
            for (int i = 0; i < SLOTS; i++) { if (!on[i].value) continue; for (String part : slots[i].value.split(",")) { String t = norm(part.trim()); if (!t.isEmpty()) out.add(t); } }
            return out;
        }

        static String norm(String s) {
            if (s == null) return "";
            s = java.text.Normalizer.normalize(s.toLowerCase(java.util.Locale.ROOT), java.text.Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
            return s;
        }

        /** True si el item encaja con ALGUNO de los nombres buscados: por su nombre o por sus encantamientos. */
        public static boolean matches(net.minecraft.world.item.ItemStack stack, java.util.List<String> terms) {
            try {
                if (stack == null || stack.isEmpty() || terms.isEmpty()) return false;
                String name = norm(stack.getHoverName().getString());
                for (String q : terms) if (name.contains(q)) return true;
                var stored = stack.get(net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS);
                var normal = stack.get(net.minecraft.core.component.DataComponents.ENCHANTMENTS);
                for (var comp : java.util.Arrays.asList(stored, normal)) {
                    if (comp == null) continue;
                    for (var e : comp.keySet()) {
                        String en = norm(e.value().description().getString());
                        for (String q : terms) if (en.contains(q)) return true;
                    }
                }
            } catch (Throwable ignored) { /* si la version lee distinto los componentes, no resalta pero no rompe */ }
            return false;
        }
    }

    /** Teclas para mandar mensajes de chat rapidos (la tecla se asigna en Opciones > Controles > Nova Client). */
    public static final class ChatMacros extends Module {
        public final Setting.Str m1 = add(new Setting.Str("Mensaje 1", "gg", 200));
        public final Setting.Str m2 = add(new Setting.Str("Mensaje 2", "glhf", 200));
        public final Setting.Str m3 = add(new Setting.Str("Mensaje 3", "", 200));
        public final Setting.Str m4 = add(new Setting.Str("Mensaje 4", "", 200));

        public ChatMacros() { super("Chat Macros", "Teclas para mensajes rapidos (asigna la tecla en Opciones > Controles)", Category.UTILITY); }

        private Setting.Str slot(int i) { return i == 0 ? m1 : i == 1 ? m2 : i == 2 ? m3 : m4; }

        /** Llamado desde NovaClient al pulsar la tecla del macro i (0..3). Manda chat, o un comando si empieza por "/". */
        public static void fire(int i) {
            var mods = NovaClient.modules;
            if (mods == null) return;
            ChatMacros m = mods.get(ChatMacros.class);
            if (m == null || !m.enabled || i < 0 || i > 3) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.screen != null) return;
            var nh = mc.getConnection();
            if (nh == null) return;
            String txt = m.slot(i).value.trim();
            if (txt.isEmpty()) return;
            if (txt.startsWith("/")) nh.sendCommand(txt.substring(1));
            else nh.sendChat(txt);
        }
    }
}
