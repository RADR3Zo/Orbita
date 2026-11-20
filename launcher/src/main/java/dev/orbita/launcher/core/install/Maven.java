package dev.orbita.launcher.core.install;

/** Convierte coordenadas Maven ("grupo:artefacto:versión[:clasificador][@ext]") en rutas. */
public final class Maven {
    private Maven() {
    }

    public static String path(String coords) {
        String ext = "jar";
        int at = coords.indexOf('@');
        if (at >= 0) {
            ext = coords.substring(at + 1);
            coords = coords.substring(0, at);
        }
        String[] p = coords.split(":");
        if (p.length < 3) {
            throw new IllegalArgumentException("Coordenada Maven inválida: " + coords);
        }
        String file = p[1] + "-" + p[2] + (p.length > 3 ? "-" + p[3] : "") + "." + ext;
        return p[0].replace('.', '/') + "/" + p[1] + "/" + p[2] + "/" + file;
    }

    /** Clave sin versión, para no meter dos versiones de la misma librería en el classpath. */
    public static String key(String coords) {
        int at = coords.indexOf('@');
        if (at >= 0) {
            coords = coords.substring(0, at);
        }
        String[] p = coords.split(":");
        return p[0] + ":" + p[1] + (p.length > 3 ? ":" + p[3] : "");
    }
}
