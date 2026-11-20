package dev.orbita.client.nova.core;

import java.awt.Color;
import java.util.function.BooleanSupplier;

/** Ajustes de un modulo. No depende de Minecraft. */
public abstract class Setting {
    public final String name;
    private BooleanSupplier visible = () -> true;

    protected Setting(String name) { this.name = name; }

    @SuppressWarnings("unchecked")
    public <T extends Setting> T showIf(BooleanSupplier s) { visible = s; return (T) this; }

    public boolean isVisible() { return visible.getAsBoolean(); }

    public abstract String save();
    public abstract void load(String s);

    public static final class Bool extends Setting {
        public boolean value;
        public Bool(String name, boolean def) { super(name); value = def; }
        public void toggle() { value = !value; }
        public String save() { return Boolean.toString(value); }
        public void load(String s) { value = Boolean.parseBoolean(s); }
    }

    public static final class Num extends Setting {
        public double value;
        public final double min, max, step;
        public Num(String name, double def, double min, double max, double step) {
            super(name); this.min = min; this.max = max; this.step = step; set(def);
        }
        public void set(double v) {
            if (step > 0) v = Math.round(v / step) * step;
            value = Math.max(min, Math.min(max, v));
        }
        public void setFraction(double f) { set(min + (max - min) * Math.max(0, Math.min(1, f))); }
        public double fraction() { return (value - min) / (max - min); }
        public float f() { return (float) value; }
        public int i() { return (int) Math.round(value); }
        public String save() { return Double.toString(value); }
        public void load(String s) { try { set(Double.parseDouble(s)); } catch (NumberFormatException ignored) { } }
    }

    public static final class Choice extends Setting {
        public final String[] options;
        public int index;
        public Choice(String name, int def, String... options) { super(name); this.options = options; index = def; }
        public String get() { return options[index]; }
        public void next() { index = (index + 1) % options.length; }
        public void prev() { index = (index + options.length - 1) % options.length; }
        public String save() { return Integer.toString(index); }
        public void load(String s) {
            try { int i = Integer.parseInt(s); if (i >= 0 && i < options.length) index = i; } catch (NumberFormatException ignored) { }
        }
    }

    /** Color en HSB: tono + luminosidad. */
    public static final class Col extends Setting {
        public float hue, light;
        public Col(String name, float hue, float light) { super(name); this.hue = hue; this.light = light; }
        public int rgb() { return Color.HSBtoRGB(hue, 1f - Math.max(0, light - 0.5f) * 1.2f, Math.min(1f, 0.35f + light * 0.9f)) & 0xFFFFFF; }
        public String save() { return hue + "," + light; }
        public void load(String s) {
            try { String[] p = s.split(","); hue = Float.parseFloat(p[0]); light = Float.parseFloat(p[1]); }
            catch (Exception ignored) { }
        }
    }

    /** Opciones dinamicas (p. ej. archivos de una carpeta). value = "" significa ninguna. */
    public static final class Pick extends Setting {
        public String value = "";
        public final java.util.function.Supplier<java.util.List<String>> options;
        public final String hint;
        /** Texto de la primera opcion (value = ""). */
        public String noneLabel = "None";
        public Pick(String name, String hint, java.util.function.Supplier<java.util.List<String>> options) { super(name); this.hint = hint; this.options = options; }
        public Pick none(String label) { noneLabel = label; return this; }
        public String save() { return value; }
        public void load(String s) { value = s == null ? "" : s; }
    }

    /** Texto que escribe el jugador (p. ej. un nombre). */
    public static final class Str extends Setting {
        public String value;
        public final int max;
        public Str(String name, String def, int max) { super(name); value = def; this.max = max; }
        public String save() { return value; }
        public void load(String s) { value = s == null ? "" : s.length() > max ? s.substring(0, max) : s; }
    }

    /** Una tecla que el jugador elige (guarda el codigo GLFW; -1 = ninguna). El nombre legible lo pone la GUI. */
    public static final class Key extends Setting {
        public int code;
        public Key(String name, int def) { super(name); code = def; }
        public String save() { return Integer.toString(code); }
        public void load(String s) { try { code = Integer.parseInt(s); } catch (NumberFormatException ignored) { } }
    }

    /** Boton que ejecuta una accion (no guarda nada). */
    public static final class Action extends Setting {
        public final String button;
        public final Runnable run;
        public Action(String name, String button, Runnable run) { super(name); this.button = button; this.run = run; }
        public String save() { return ""; }
        public void load(String s) { }
    }
}
