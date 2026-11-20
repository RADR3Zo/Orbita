package dev.orbita.launcher.core.auth;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Cuenta con la que se lanza el juego.
 *
 * @param type "msa" (Microsoft) o "legacy" (offline)
 */
public record Account(String type, String name, String uuid, String accessToken, String refreshToken,
                      long expiresAt, String xuid) {

    public static Account offline(String name) {
        UUID id = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
        return new Account("legacy", name, id.toString().replace("-", ""), "0", null, Long.MAX_VALUE, "0");
    }

    public boolean isMicrosoft() {
        return "msa".equals(type);
    }

    public boolean expired() {
        return System.currentTimeMillis() > expiresAt - 60_000;
    }
}
