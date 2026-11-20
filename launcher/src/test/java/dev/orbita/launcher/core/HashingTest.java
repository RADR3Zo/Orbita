package dev.orbita.launcher.core;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HashingTest {

    @Test
    void sha1YSha256Conocidos() throws Exception {
        // Valores de referencia del estándar para el texto "abc"
        assertEquals("a9993e364706816aba3e25717850c26c9cd0d89d",
                Hashing.hash(new ByteArrayInputStream("abc".getBytes(StandardCharsets.US_ASCII)), "SHA-1"));
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                Hashing.hash(new ByteArrayInputStream("abc".getBytes(StandardCharsets.US_ASCII)), "SHA-256"));
    }
}
