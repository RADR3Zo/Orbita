package dev.orbita.launcher.core.update;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdaterTest {
    /** Respuesta recortada de https://api.github.com/repos/USUARIO/orbita/releases/latest */
    private static final String RELEASE_JSON = """
            {
              "tag_name": "v1.5.5",
              "draft": false,
              "html_url": "https://github.com/usuario/orbita/releases/tag/v1.5.5",
              "body": "- Arreglado el zoom\\n- Nuevo módulo",
              "assets": [
                {"name": "Orbita-1.5.5-windows-x64.zip", "browser_download_url": "https://example.com/zip"},
                {"name": "orbita-launcher-1.5.5.jar", "browser_download_url": "https://example.com/jar"},
                {"name": "orbita-launcher-1.5.5.jar.sha256", "browser_download_url": "https://example.com/sha"}
              ]
            }
            """;

    private static JsonObject json(String s) {
        return JsonParser.parseString(s).getAsJsonObject();
    }

    @Test
    void leeLaReleaseDeGitHub() {
        Updater.Release r = Updater.parseRelease(json(RELEASE_JSON));
        assertNotNull(r);
        assertEquals(Version.parse("1.5.5"), r.version());
        assertEquals("https://example.com/jar", r.jarUrl());
        assertEquals("orbita-launcher-1.5.5.jar", r.jarName());
        assertEquals("https://example.com/sha", r.sha256Url());
        assertEquals("https://example.com/zip", r.zipUrl());
        assertFalse(r.needsFullDownload());
    }

    @Test
    void sinJarHayQueDescargarTodo() {
        Updater.Release r = Updater.parseRelease(json("""
                {"tag_name": "v1.6.0", "assets": [{"name": "Orbita-1.6.0-windows-x64.zip", "browser_download_url": "z"}]}
                """));
        assertNotNull(r);
        assertTrue(r.needsFullDownload());
    }

    @Test
    void laMarcaCompletaPideElZip() {
        Updater.Release r = Updater.parseRelease(json(RELEASE_JSON.replace("Arreglado el zoom", "Nuevo Java [COMPLETA]")));
        assertTrue(r.needsFullDownload());
    }

    @Test
    void ignoraBorradoresYEtiquetasRaras() {
        assertNull(Updater.parseRelease(json("{\"tag_name\": \"v2.0.0\", \"draft\": true}")));
        assertNull(Updater.parseRelease(json("{\"tag_name\": \"prueba\"}")));
    }

    @Test
    void reescribeOrbitaCfg() {
        String cfg = """
                [Application]
                app.classpath=$APPDIR\\orbita-launcher-1.5.4.jar
                app.mainclass=dev.orbita.launcher.OrbitaLauncher

                [JavaOptions]
                java-options=-Djpackage.app-version=1.5.4
                java-options=-Xmx256m
                """;
        String out = Updater.rewriteCfg(cfg, "orbita-launcher-1.5.5.jar", "1.5.5");
        assertTrue(out.contains("app.classpath=$APPDIR\\orbita-launcher-1.5.5.jar\n"), out);
        assertTrue(out.contains("java-options=-Djpackage.app-version=1.5.5\n"), out);
        assertTrue(out.contains("app.mainclass=dev.orbita.launcher.OrbitaLauncher"));
        assertTrue(out.contains("java-options=-Xmx256m"));
        assertFalse(out.contains("1.5.4"));
    }

    @Test
    void leeElSha256() {
        String h = "a".repeat(64);
        assertEquals(h, Updater.parseSha256(h));
        assertEquals(h, Updater.parseSha256(h.toUpperCase() + "  orbita-launcher-1.5.5.jar\n"));
        assertNull(Updater.parseSha256("no es un hash"));
        assertNull(Updater.parseSha256(null));
    }
}
