package dev.orbita.launcher.core.install;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MavenTest {

    @Test
    void rutaNormal() {
        assertEquals("net/fabricmc/fabric-loader/0.19.5/fabric-loader-0.19.5.jar",
                Maven.path("net.fabricmc:fabric-loader:0.19.5"));
    }

    @Test
    void rutaConClasificadorYExtension() {
        assertEquals("org/lwjgl/lwjgl/3.3.3/lwjgl-3.3.3-natives-windows.jar",
                Maven.path("org.lwjgl:lwjgl:3.3.3:natives-windows"));
        assertEquals("com/example/cosa/1.0/cosa-1.0.zip", Maven.path("com.example:cosa:1.0@zip"));
    }

    @Test
    void claveSinVersion() {
        // Dos versiones de la misma librería tienen la misma clave: así no se meten las dos en el classpath
        assertEquals(Maven.key("org.ow2.asm:asm:9.7"), Maven.key("org.ow2.asm:asm:9.8"));
        assertEquals("org.lwjgl:lwjgl:natives-windows", Maven.key("org.lwjgl:lwjgl:3.3.3:natives-windows"));
    }

    @Test
    void coordenadaInvalida() {
        assertThrows(IllegalArgumentException.class, () -> Maven.path("solo:dos"));
    }
}
