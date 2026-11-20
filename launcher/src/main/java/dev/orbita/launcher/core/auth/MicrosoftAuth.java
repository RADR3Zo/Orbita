package dev.orbita.launcher.core.auth;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.orbita.launcher.core.Http;
import dev.orbita.launcher.core.Progress;

import java.io.IOException;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * Inicio de sesión Microsoft → Xbox Live → XSTS → Minecraft mediante "device code flow"
 * (te sale un código, lo pones en microsoft.com/link y listo).
 *
 * <p>Sin Client ID propio (lo normal) usa el inicio de sesión oficial de Microsoft (login.live.com) con el
 * identificador del launcher de Minecraft, igual que otros launchers: no hace falta ninguna app de Azure ni
 * permiso de Mojang. Si en Ajustes pones tu propio Client ID de Azure (aprobado por Mojang), se usa ese.
 */
public final class MicrosoftAuth {
    // Login oficial de Microsoft (sin Azure): mismo identificador que usa el launcher de Minecraft.
    private static final String LIVE_CLIENT_ID = "00000000402b5328";
    private static final String LIVE_DEVICE = "https://login.live.com/oauth20_connect.srf";
    private static final String LIVE_TOKEN = "https://login.live.com/oauth20_token.srf";
    private static final String LIVE_SCOPE = "service::user.auth.xboxlive.com::MBI_SSL";
    /** Página a la que Microsoft vuelve al terminar el login con correo y contraseña (trae ?code=...). */
    public static final String LIVE_REDIRECT = "https://login.live.com/oauth20_desktop.srf";
    // App propia de Azure (opcional, solo si el usuario pone su Client ID en Ajustes).
    private static final String TENANT = "https://login.microsoftonline.com/consumers/oauth2/v2.0";
    private static final String AZURE_SCOPE = "XboxLive.signin offline_access";

    public record DeviceCode(String deviceCode, String userCode, String verificationUri, int interval, long expiresAt) {
    }

    private final String clientId;
    private final boolean live;
    private final String customClientId; // el de Ajustes (puede estar vacío)

    /** @param clientId Client ID de Azure de Ajustes; vacío o null = login oficial de Microsoft (recomendado). */
    public MicrosoftAuth(String clientId) {
        this.customClientId = clientId == null ? "" : clientId.trim();
        this.live = customClientId.isEmpty();
        this.clientId = live ? LIVE_CLIENT_ID : customClientId;
    }

    private String deviceUrl() {
        return live ? LIVE_DEVICE : TENANT + "/devicecode";
    }

    private String tokenUrl() {
        return live ? LIVE_TOKEN : TENANT + "/token";
    }

    private String scope() {
        return live ? LIVE_SCOPE : AZURE_SCOPE;
    }

    /** Página oficial de Microsoft para poner correo y contraseña (igual que el launcher de Minecraft y Nova). */
    public static String browserLoginUrl() {
        return "https://login.live.com/oauth20_authorize.srf?client_id=" + LIVE_CLIENT_ID
                + "&response_type=code&scope=" + enc(LIVE_SCOPE) + "&redirect_uri=" + enc(LIVE_REDIRECT)
                + "&prompt=select_account";
    }

    /** Termina el login con correo y contraseña: cambia el ?code= de la página de Microsoft por la cuenta. */
    public Account loginWithCode(String code, Progress p) throws IOException {
        if (!live) {
            return new MicrosoftAuth("").loginWithCode(code, p); // este login siempre es el oficial
        }
        p.status("Conectando con Microsoft…");
        Http.Response r = Http.postForm(LIVE_TOKEN, Map.of(
                "client_id", LIVE_CLIENT_ID,
                "grant_type", "authorization_code",
                "code", code,
                "redirect_uri", LIVE_REDIRECT,
                "scope", LIVE_SCOPE));
        JsonObject o = r.json().getAsJsonObject();
        if (!r.ok() || !o.has("access_token")) {
            throw new IOException("Microsoft rechazó el inicio de sesión: " + str(o, "error_description", str(o, "error", "HTTP " + r.status())));
        }
        return finish(o.get("access_token").getAsString(), str(o, "refresh_token", null), p);
    }

    private static String enc(String v) {
        return java.net.URLEncoder.encode(v, java.nio.charset.StandardCharsets.UTF_8);
    }

    public DeviceCode requestDeviceCode() throws IOException {
        Http.Response r = Http.postForm(deviceUrl(), live
                ? Map.of("client_id", clientId, "scope", scope(), "response_type", "device_code")
                : Map.of("client_id", clientId, "scope", scope()));
        JsonObject o = r.json().getAsJsonObject();
        if (!r.ok()) {
            throw new IOException("Microsoft rechazó la solicitud: " + str(o, "error_description", str(o, "error", "HTTP " + r.status())));
        }
        return new DeviceCode(
                o.get("device_code").getAsString(),
                o.get("user_code").getAsString(),
                o.get("verification_uri").getAsString(),
                o.has("interval") ? o.get("interval").getAsInt() : 5,
                System.currentTimeMillis() + o.get("expires_in").getAsLong() * 1000);
    }

    /** Bloquea hasta que el usuario completa el inicio de sesión en el navegador. */
    public Account pollForAccount(DeviceCode code, BooleanSupplier cancelled, Progress p) throws IOException {
        int interval = Math.max(code.interval(), 1);
        while (System.currentTimeMillis() < code.expiresAt()) {
            if (cancelled.getAsBoolean()) {
                throw new IOException("Inicio de sesión cancelado.");
            }
            try {
                Thread.sleep(interval * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Inicio de sesión interrumpido.", e);
            }
            Http.Response r = Http.postForm(tokenUrl(), Map.of(
                    "grant_type", "urn:ietf:params:oauth:grant-type:device_code",
                    "client_id", clientId,
                    "device_code", code.deviceCode()));
            JsonObject o = r.json().getAsJsonObject();
            if (r.ok()) {
                return finish(o.get("access_token").getAsString(), str(o, "refresh_token", null), p);
            }
            String error = str(o, "error", "");
            switch (error) {
                case "authorization_pending" -> {
                }
                case "slow_down" -> interval += 5;
                case "authorization_declined" -> throw new IOException("Has rechazado el inicio de sesión.");
                case "expired_token" -> throw new IOException("El código caducó. Vuelve a intentarlo.");
                default -> throw new IOException("Error de Microsoft: " + str(o, "error_description", error));
            }
        }
        throw new IOException("El código caducó. Vuelve a intentarlo.");
    }

    public Account refresh(Account account, Progress p) throws IOException {
        if (account.refreshToken() == null) {
            throw new IOException("La sesión de Microsoft caducó; vuelve a iniciar sesión.");
        }
        IOException first;
        try {
            return refreshWith(account, p);
        } catch (IOException e) {
            first = e;
        }
        // La cuenta pudo crearse con el otro modo (login oficial ↔ Client ID propio): se prueba también.
        MicrosoftAuth other = live
                ? (customClientId.isEmpty() ? null : new MicrosoftAuth(customClientId))
                : new MicrosoftAuth("");
        if (other != null) {
            try {
                return other.refreshWith(account, p);
            } catch (IOException ignored) {
                // se informa el primer error
            }
        }
        throw first;
    }

    private Account refreshWith(Account account, Progress p) throws IOException {
        Http.Response r = Http.postForm(tokenUrl(), live
                ? Map.of("grant_type", "refresh_token", "client_id", clientId, "refresh_token", account.refreshToken(),
                        "scope", scope(), "redirect_uri", LIVE_REDIRECT)
                : Map.of("grant_type", "refresh_token", "client_id", clientId, "refresh_token", account.refreshToken(),
                        "scope", scope()));
        if (!r.ok()) {
            throw new IOException("La sesión de Microsoft caducó; vuelve a iniciar sesión.");
        }
        JsonObject o = r.json().getAsJsonObject();
        return finish(o.get("access_token").getAsString(), str(o, "refresh_token", account.refreshToken()), p);
    }

    private Account finish(String msToken, String refreshToken, Progress p) throws IOException {
        p.status("Autenticando con Xbox Live…");
        // El token de login.live.com se manda con "t=" y el de Azure con "d=". Si uno no entra, se prueba el otro.
        String[] prefixes = live ? new String[]{"t=", "d="} : new String[]{"d=", "t="};
        Http.Response xbl = null;
        for (String prefix : prefixes) {
            JsonObject xblBody = new JsonObject();
            JsonObject props = new JsonObject();
            props.addProperty("AuthMethod", "RPS");
            props.addProperty("SiteName", "user.auth.xboxlive.com");
            props.addProperty("RpsTicket", prefix + msToken);
            xblBody.add("Properties", props);
            xblBody.addProperty("RelyingParty", "http://auth.xboxlive.com");
            xblBody.addProperty("TokenType", "JWT");
            xbl = Http.postJson("https://user.auth.xboxlive.com/user/authenticate", xblBody.toString());
            if (xbl.ok()) {
                break;
            }
        }
        if (!xbl.ok()) {
            throw new IOException("Xbox Live rechazó la cuenta (HTTP " + xbl.status() + ").");
        }
        JsonObject xblJson = xbl.json().getAsJsonObject();
        String xblToken = xblJson.get("Token").getAsString();

        JsonObject xstsBody = new JsonObject();
        JsonObject xstsProps = new JsonObject();
        xstsProps.addProperty("SandboxId", "RETAIL");
        JsonArray tokens = new JsonArray();
        tokens.add(xblToken);
        xstsProps.add("UserTokens", tokens);
        xstsBody.add("Properties", xstsProps);
        xstsBody.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
        xstsBody.addProperty("TokenType", "JWT");
        Http.Response xsts = Http.postJson("https://xsts.auth.xboxlive.com/xsts/authorize", xstsBody.toString());
        if (xsts.status() == 401) {
            long err = xsts.json().getAsJsonObject().has("XErr") ? xsts.json().getAsJsonObject().get("XErr").getAsLong() : 0;
            throw new IOException(err == 2148916233L ? "Esta cuenta no tiene perfil de Xbox. Créalo en xbox.com e inténtalo de nuevo."
                    : err == 2148916238L ? "Las cuentas infantiles necesitan pertenecer a una familia de Microsoft."
                    : "Xbox Live denegó el acceso (XErr " + err + ").");
        }
        if (!xsts.ok()) {
            throw new IOException("XSTS rechazó la cuenta (HTTP " + xsts.status() + ").");
        }
        JsonObject xstsJson = xsts.json().getAsJsonObject();
        String xstsToken = xstsJson.get("Token").getAsString();
        String uhs = xstsJson.getAsJsonObject("DisplayClaims").getAsJsonArray("xui").get(0).getAsJsonObject().get("uhs").getAsString();

        p.status("Autenticando con Minecraft…");
        JsonObject mcBody = new JsonObject();
        mcBody.addProperty("identityToken", "XBL3.0 x=" + uhs + ";" + xstsToken);
        Http.Response mc = Http.postJson("https://api.minecraftservices.com/authentication/login_with_xbox", mcBody.toString());
        if (!mc.ok()) {
            throw new IOException(mc.status() == 403
                    ? "Mojang no ha autorizado este Client ID para la API de Minecraft."
                    : "Minecraft rechazó el inicio de sesión (HTTP " + mc.status() + ").");
        }
        JsonObject mcJson = mc.json().getAsJsonObject();
        String mcToken = mcJson.get("access_token").getAsString();
        long expires = System.currentTimeMillis() + mcJson.get("expires_in").getAsLong() * 1000;

        Http.Response profile = Http.get("https://api.minecraftservices.com/minecraft/profile",
                "Authorization", "Bearer " + mcToken);
        if (profile.status() == 404) {
            throw new IOException("Esta cuenta no tiene Minecraft: Java Edition.");
        }
        if (!profile.ok()) {
            throw new IOException("No se pudo leer el perfil de Minecraft (HTTP " + profile.status() + ").");
        }
        JsonObject prof = profile.json().getAsJsonObject();
        return new Account("msa", prof.get("name").getAsString(), prof.get("id").getAsString(), mcToken, refreshToken, expires, "0");
    }

    private static String str(JsonObject o, String key, String fallback) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : fallback;
    }
}
