package dev.orbita.client.account;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Inicio de sesión premium dentro del juego (igual que en el launcher): Microsoft → Xbox Live → XSTS → Minecraft.
 * Se usa el código de dispositivo: el juego muestra un código y lo escribes en microsoft.com/link.
 * Órbita nunca ve tu contraseña.
 */
public final class MicrosoftLogin {
    private static final String CLIENT_ID = "00000000402b5328";
    private static final String DEVICE = "https://login.live.com/oauth20_connect.srf";
    private static final String TOKEN = "https://login.live.com/oauth20_token.srf";
    private static final String SCOPE = "service::user.auth.xboxlive.com::MBI_SSL";
    private static final String REDIRECT = "https://login.live.com/oauth20_desktop.srf";
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20))
            .followRedirects(HttpClient.Redirect.NORMAL).build();

    public record DeviceCode(String deviceCode, String userCode, String verificationUri, int interval, long expiresAt) {
    }

    private record Res(int status, JsonObject json) {
        boolean ok() {
            return status >= 200 && status < 300;
        }
    }

    private MicrosoftLogin() {
    }

    public static DeviceCode requestCode() throws IOException {
        Res r = form(DEVICE, Map.of("client_id", CLIENT_ID, "scope", SCOPE, "response_type", "device_code"));
        if (!r.ok()) {
            throw new IOException("Microsoft rechazó la solicitud: " + str(r.json(), "error_description", "HTTP " + r.status()));
        }
        JsonObject o = r.json();
        return new DeviceCode(o.get("device_code").getAsString(), o.get("user_code").getAsString(),
                o.get("verification_uri").getAsString(), o.has("interval") ? o.get("interval").getAsInt() : 5,
                System.currentTimeMillis() + o.get("expires_in").getAsLong() * 1000);
    }

    /** Espera a que confirmes en el navegador y devuelve la cuenta. */
    public static Accounts.Acc waitForAccount(DeviceCode code, BooleanSupplier cancelled, Consumer<String> status) throws IOException {
        int interval = Math.max(1, code.interval());
        while (System.currentTimeMillis() < code.expiresAt()) {
            if (cancelled.getAsBoolean()) {
                throw new IOException("Cancelado.");
            }
            try {
                Thread.sleep(interval * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrumpido.");
            }
            Res r = form(TOKEN, Map.of("grant_type", "urn:ietf:params:oauth:grant-type:device_code",
                    "client_id", CLIENT_ID, "device_code", code.deviceCode()));
            if (r.ok()) {
                return finish(r.json().get("access_token").getAsString(), str(r.json(), "refresh_token", null), status);
            }
            String error = str(r.json(), "error", "");
            switch (error) {
                case "authorization_pending" -> {
                }
                case "slow_down" -> interval += 5;
                case "authorization_declined" -> throw new IOException("Has rechazado el inicio de sesión.");
                case "expired_token" -> throw new IOException("El código caducó. Vuelve a intentarlo.");
                default -> throw new IOException("Error de Microsoft: " + str(r.json(), "error_description", error));
            }
        }
        throw new IOException("El código caducó. Vuelve a intentarlo.");
    }

    /** Renueva la sesión de una cuenta premium guardada (cuando su token de Minecraft ya caducó). */
    public static Accounts.Acc refresh(Accounts.Acc acc, Consumer<String> status) throws IOException {
        if (acc.refreshToken() == null) {
            throw new IOException("La sesión de " + acc.name() + " caducó: vuelve a añadir la cuenta.");
        }
        Res r = form(TOKEN, Map.of("grant_type", "refresh_token", "client_id", CLIENT_ID, "refresh_token", acc.refreshToken(),
                "scope", SCOPE, "redirect_uri", REDIRECT));
        if (!r.ok()) {
            throw new IOException("La sesión de " + acc.name() + " caducó: vuelve a añadir la cuenta.");
        }
        return finish(r.json().get("access_token").getAsString(), str(r.json(), "refresh_token", acc.refreshToken()), status);
    }

    private static Accounts.Acc finish(String msToken, String refreshToken, Consumer<String> status) throws IOException {
        status.accept("Autenticando con Xbox Live…");
        Res xbl = null;
        for (String prefix : new String[]{"t=", "d="}) {
            JsonObject props = new JsonObject();
            props.addProperty("AuthMethod", "RPS");
            props.addProperty("SiteName", "user.auth.xboxlive.com");
            props.addProperty("RpsTicket", prefix + msToken);
            JsonObject body = new JsonObject();
            body.add("Properties", props);
            body.addProperty("RelyingParty", "http://auth.xboxlive.com");
            body.addProperty("TokenType", "JWT");
            xbl = json("https://user.auth.xboxlive.com/user/authenticate", body.toString(), null);
            if (xbl.ok()) {
                break;
            }
        }
        if (xbl == null || !xbl.ok()) {
            throw new IOException("Xbox Live rechazó la cuenta (HTTP " + (xbl == null ? 0 : xbl.status()) + ").");
        }
        JsonObject xstsProps = new JsonObject();
        xstsProps.addProperty("SandboxId", "RETAIL");
        JsonArray tokens = new JsonArray();
        tokens.add(xbl.json().get("Token").getAsString());
        xstsProps.add("UserTokens", tokens);
        JsonObject xstsBody = new JsonObject();
        xstsBody.add("Properties", xstsProps);
        xstsBody.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
        xstsBody.addProperty("TokenType", "JWT");
        Res xsts = json("https://xsts.auth.xboxlive.com/xsts/authorize", xstsBody.toString(), null);
        if (xsts.status() == 401) {
            long err = xsts.json().has("XErr") ? xsts.json().get("XErr").getAsLong() : 0;
            throw new IOException(err == 2148916233L ? "Esta cuenta no tiene perfil de Xbox: créalo en xbox.com."
                    : err == 2148916238L ? "Las cuentas infantiles necesitan pertenecer a una familia de Microsoft."
                    : "Xbox Live denegó el acceso (XErr " + err + ").");
        }
        if (!xsts.ok()) {
            throw new IOException("XSTS rechazó la cuenta (HTTP " + xsts.status() + ").");
        }
        String uhs = xsts.json().getAsJsonObject("DisplayClaims").getAsJsonArray("xui").get(0).getAsJsonObject().get("uhs").getAsString();
        status.accept("Autenticando con Minecraft…");
        JsonObject mcBody = new JsonObject();
        mcBody.addProperty("identityToken", "XBL3.0 x=" + uhs + ";" + xsts.json().get("Token").getAsString());
        Res mc = json("https://api.minecraftservices.com/authentication/login_with_xbox", mcBody.toString(), null);
        if (!mc.ok()) {
            throw new IOException("Minecraft rechazó el inicio de sesión (HTTP " + mc.status() + ").");
        }
        String mcToken = mc.json().get("access_token").getAsString();
        long expires = System.currentTimeMillis() + mc.json().get("expires_in").getAsLong() * 1000;
        Res prof = get("https://api.minecraftservices.com/minecraft/profile", mcToken);
        if (prof.status() == 404) {
            throw new IOException("Esta cuenta no tiene Minecraft: Java Edition.");
        }
        if (!prof.ok()) {
            throw new IOException("No se pudo leer el perfil de Minecraft (HTTP " + prof.status() + ").");
        }
        return new Accounts.Acc("msa", prof.json().get("name").getAsString(), prof.json().get("id").getAsString(),
                mcToken, refreshToken, expires, "0");
    }

    // ------------------------------------------------------------------ red

    private static Res form(String url, Map<String, String> data) throws IOException {
        String body = data.entrySet().stream().map(e -> enc(e.getKey()) + "=" + enc(e.getValue())).collect(Collectors.joining("&"));
        return send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/x-www-form-urlencoded").header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build());
    }

    private static Res json(String url, String body, String bearer) throws IOException {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json").header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        if (bearer != null) {
            b.header("Authorization", "Bearer " + bearer);
        }
        return send(b.build());
    }

    private static Res get(String url, String bearer) throws IOException {
        return send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + bearer).GET().build());
    }

    private static Res send(HttpRequest req) throws IOException {
        try {
            HttpResponse<String> r = HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonObject o;
            try {
                o = JsonParser.parseString(r.body()).getAsJsonObject();
            } catch (Exception e) {
                o = new JsonObject();
            }
            return new Res(r.statusCode(), o);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrumpido.", e);
        }
    }

    private static String enc(String v) {
        return URLEncoder.encode(v, StandardCharsets.UTF_8);
    }

    private static String str(JsonObject o, String k, String def) {
        return o != null && o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : def;
    }
}
