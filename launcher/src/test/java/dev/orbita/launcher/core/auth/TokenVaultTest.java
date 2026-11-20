package dev.orbita.launcher.core.auth;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenVaultTest {
    private TokenVault.Backend original;

    /** Cifrado de mentira (invierte y pasa a base64) para probar sin Windows. */
    private static final TokenVault.Backend FAKE = new TokenVault.Backend() {
        @Override
        public String protect(String plain) {
            return Base64.getEncoder().encodeToString(new StringBuilder(plain).reverse().toString().getBytes());
        }

        @Override
        public String reveal(String cipher) throws IOException {
            if (cipher.equals("roto")) {
                throw new IOException("no se puede");
            }
            return new StringBuilder(new String(Base64.getDecoder().decode(cipher))).reverse().toString();
        }
    };

    private final Gson gson = new GsonBuilder().registerTypeAdapterFactory(new AccountJson()).create();

    @BeforeEach
    void setUp() {
        original = TokenVault.backend;
        TokenVault.backend = FAKE;
        TokenVault.clearCache();
    }

    @AfterEach
    void tearDown() {
        TokenVault.backend = original;
        TokenVault.clearCache();
    }

    @Test
    void cifraYDescifra() {
        String stored = TokenVault.protect("mi-token-secreto");
        assertTrue(stored.startsWith(TokenVault.PREFIX));
        assertFalse(stored.contains("mi-token-secreto"));
        TokenVault.clearCache();
        assertEquals("mi-token-secreto", TokenVault.reveal(stored));
    }

    @Test
    void cuentasOfflineNoSeTocan() {
        assertEquals("0", TokenVault.protect("0"));
        assertNull(TokenVault.protect(null));
        assertEquals("", TokenVault.protect(""));
    }

    @Test
    void archivosViejosSinCifrarSeLeenIgual() {
        assertEquals("token-viejo", TokenVault.reveal("token-viejo"));
    }

    @Test
    void noSeCifraDosVeces() {
        String once = TokenVault.protect("abc");
        assertEquals(once, TokenVault.protect(once));
    }

    @Test
    void elJsonGuardadoNoTieneLosTokensEnClaro() {
        Account acc = new Account("msa", "Jhon", "uuid", "ACCESO-123", "REFRESCO-456", 99L, "0");
        String json = gson.toJson(acc);
        assertFalse(json.contains("ACCESO-123"), json);
        assertFalse(json.contains("REFRESCO-456"), json);

        TokenVault.clearCache();
        Account back = gson.fromJson(json, Account.class);
        assertEquals(acc, back);
    }

    @Test
    void tokenDeOtroEquipoPideIniciarSesion() {
        JsonObject o = new JsonObject();
        o.addProperty("type", "msa");
        o.addProperty("name", "Jhon");
        o.addProperty("uuid", "uuid");
        o.addProperty("accessToken", TokenVault.PREFIX + "roto");
        o.addProperty("refreshToken", TokenVault.PREFIX + "roto");
        o.addProperty("expiresAt", Long.MAX_VALUE);
        Account a = gson.fromJson(o, Account.class);
        assertNull(a.accessToken());
        assertNull(a.refreshToken());
        assertTrue(a.expired());
    }
}
