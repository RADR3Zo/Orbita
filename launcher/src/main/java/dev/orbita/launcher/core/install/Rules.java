package dev.orbita.launcher.core.install;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Locale;
import java.util.Map;

/** Evalúa las "rules" de los JSON de versión de Mojang (sistema operativo, arquitectura, features). */
public final class Rules {
    private static final String OS_NAME = detectOs();
    private static final String ARCH = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);

    private Rules() {
    }

    public static boolean allowed(JsonArray rules) {
        return allowed(rules, Map.of());
    }

    public static boolean allowed(JsonArray rules, Map<String, Boolean> features) {
        if (rules == null || rules.isEmpty()) {
            return true;
        }
        boolean result = false;
        for (JsonElement e : rules) {
            JsonObject rule = e.getAsJsonObject();
            if (matches(rule, features)) {
                result = "allow".equals(rule.get("action").getAsString());
            }
        }
        return result;
    }

    private static boolean matches(JsonObject rule, Map<String, Boolean> features) {
        if (rule.has("os")) {
            JsonObject os = rule.getAsJsonObject("os");
            if (os.has("name") && !OS_NAME.equals(os.get("name").getAsString())) {
                return false;
            }
            if (os.has("arch")) {
                String arch = os.get("arch").getAsString();
                if ("x86".equals(arch) && !(ARCH.equals("x86") || ARCH.equals("i386") || ARCH.equals("i686"))) {
                    return false;
                }
            }
        }
        if (rule.has("features")) {
            for (Map.Entry<String, JsonElement> f : rule.getAsJsonObject("features").entrySet()) {
                boolean have = features.getOrDefault(f.getKey(), false);
                if (have != f.getValue().getAsBoolean()) {
                    return false;
                }
            }
        }
        return true;
    }

    private static String detectOs() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            return "windows";
        }
        if (os.contains("mac")) {
            return "osx";
        }
        return "linux";
    }
}
