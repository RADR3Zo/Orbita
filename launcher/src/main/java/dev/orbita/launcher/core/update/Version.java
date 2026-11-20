package dev.orbita.launcher.core.update;

import java.util.Arrays;

/**
 * Número de versión tipo "1.5.4" (también acepta "v1.5.4" y sufijos como "1.6.0-beta").
 * Se comparan los números uno a uno: 1.10.0 es más nueva que 1.9.9. Una versión con sufijo
 * ("1.6.0-beta") es más vieja que la misma sin sufijo ("1.6.0").
 */
public record Version(int[] parts, String suffix) implements Comparable<Version> {

    public static Version parse(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Versión vacía");
        }
        String s = text.trim();
        if (s.startsWith("v") || s.startsWith("V")) {
            s = s.substring(1);
        }
        String suffix = "";
        int dash = s.indexOf('-');
        if (dash >= 0) {
            suffix = s.substring(dash + 1);
            s = s.substring(0, dash);
        }
        String[] pieces = s.split("\\.");
        int[] parts = new int[pieces.length];
        for (int i = 0; i < pieces.length; i++) {
            try {
                parts[i] = Integer.parseInt(pieces[i]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Versión inválida: " + text);
            }
        }
        return new Version(parts, suffix);
    }

    /** Como {@link #parse}, pero devuelve null en vez de lanzar error (por ejemplo, la versión "dev"). */
    public static Version tryParse(String text) {
        try {
            return parse(text);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public int compareTo(Version o) {
        int n = Math.max(parts.length, o.parts.length);
        for (int i = 0; i < n; i++) {
            int a = i < parts.length ? parts[i] : 0;
            int b = i < o.parts.length ? o.parts[i] : 0;
            if (a != b) {
                return Integer.compare(a, b);
            }
        }
        if (suffix.isEmpty() != o.suffix.isEmpty()) {
            return suffix.isEmpty() ? 1 : -1;
        }
        return suffix.compareTo(o.suffix);
    }

    public boolean isNewerThan(Version o) {
        return compareTo(o) > 0;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Version v && compareTo(v) == 0;
    }

    @Override
    public int hashCode() {
        // 1.5 y 1.5.0 son iguales, así que los ceros del final no cuentan
        int end = parts.length;
        while (end > 0 && parts[end - 1] == 0) {
            end--;
        }
        return Arrays.hashCode(Arrays.copyOf(parts, end)) * 31 + suffix.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                b.append('.');
            }
            b.append(parts[i]);
        }
        return suffix.isEmpty() ? b.toString() : b + "-" + suffix;
    }
}
