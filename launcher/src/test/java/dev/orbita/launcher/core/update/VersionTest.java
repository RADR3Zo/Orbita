package dev.orbita.launcher.core.update;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionTest {

    @Test
    void comparaNumeroANumero() {
        assertTrue(Version.parse("1.5.5").isNewerThan(Version.parse("1.5.4")));
        assertTrue(Version.parse("1.6.0").isNewerThan(Version.parse("1.5.9")));
        assertTrue(Version.parse("2.0").isNewerThan(Version.parse("1.99.99")));
    }

    @Test
    void diezEsMayorQueNueve() {
        // Si se comparara como texto, "1.10.0" saldría menor que "1.9.0"
        assertTrue(Version.parse("1.10.0").isNewerThan(Version.parse("1.9.0")));
    }

    @Test
    void aceptaLaVDeLasEtiquetas() {
        assertEquals(Version.parse("1.5.4"), Version.parse("v1.5.4"));
        assertEquals("1.5.4", Version.parse("v1.5.4").toString());
    }

    @Test
    void cerosDelFinalNoCuentan() {
        assertEquals(Version.parse("1.5"), Version.parse("1.5.0"));
        assertEquals(Version.parse("1.5").hashCode(), Version.parse("1.5.0").hashCode());
        assertFalse(Version.parse("1.5.0").isNewerThan(Version.parse("1.5")));
    }

    @Test
    void unaBetaEsMasViejaQueLaFinal() {
        assertTrue(Version.parse("1.6.0").isNewerThan(Version.parse("1.6.0-beta")));
        assertTrue(Version.parse("1.6.0-beta").isNewerThan(Version.parse("1.5.9")));
    }

    @Test
    void versionesInvalidas() {
        assertNull(Version.tryParse("dev"));
        assertNull(Version.tryParse(""));
        assertNull(Version.tryParse(null));
        assertThrows(IllegalArgumentException.class, () -> Version.parse("1.x.3"));
    }
}
