package dev.orbita.launcher.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dev.orbita.launcher.LauncherInfo;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

/** Cliente HTTP mínimo sobre java.net.http con reintentos y verificación de hash en descargas. */
public final class Http {
    public static final String USER_AGENT = "OrbitaLauncher/" + LauncherInfo.VERSION;
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    public record Response(int status, String body) {
        public boolean ok() {
            return status >= 200 && status < 300;
        }

        public JsonElement json() {
            return JsonParser.parseString(body);
        }
    }

    private Http() {
    }

    private static HttpRequest.Builder request(String url) {
        return HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(60))
                .header("User-Agent", USER_AGENT);
    }

    private static Response send(HttpRequest req) throws IOException {
        try {
            HttpResponse<String> res = CLIENT.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new Response(res.statusCode(), res.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Petición interrumpida", e);
        }
    }

    public static Response get(String url, String... headers) throws IOException {
        HttpRequest.Builder b = request(url).GET();
        addHeaders(b, headers);
        return send(b.build());
    }

    public static JsonElement getJson(String url) throws IOException {
        Response r = get(url, "Accept", "application/json");
        if (!r.ok()) {
            throw new IOException("HTTP " + r.status() + " en " + url);
        }
        return r.json();
    }

    public static Response postJson(String url, String json, String... headers) throws IOException {
        HttpRequest.Builder b = request(url)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));
        addHeaders(b, headers);
        return send(b.build());
    }

    public static Response postForm(String url, Map<String, String> form) throws IOException {
        String body = form.entrySet().stream()
                .map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
                .collect(Collectors.joining("&"));
        return send(request(url)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build());
    }

    /**
     * Descarga a un .part y lo mueve al destino solo si el hash coincide.
     *
     * @param algorithm "SHA-1" o "SHA-512"; null para no verificar
     */
    public static void download(String url, Path target, String algorithm, String expectedHash) throws IOException {
        Files.createDirectories(target.getParent());
        Path tmp = target.resolveSibling(target.getFileName() + ".part");
        IOException last = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                HttpResponse<Path> res = CLIENT.send(request(url).GET().build(), HttpResponse.BodyHandlers.ofFile(tmp));
                if (res.statusCode() != 200) {
                    throw new IOException("HTTP " + res.statusCode() + " al descargar " + url);
                }
                if (algorithm != null && expectedHash != null && !expectedHash.equalsIgnoreCase(Hashing.hash(tmp, algorithm))) {
                    throw new IOException("El archivo descargado no coincide con su hash: " + url);
                }
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Files.deleteIfExists(tmp);
                throw new IOException("Descarga interrumpida", e);
            } catch (IOException e) {
                last = e;
            }
        }
        Files.deleteIfExists(tmp);
        throw last;
    }

    public static String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static void addHeaders(HttpRequest.Builder b, String... headers) {
        for (int i = 0; i + 1 < headers.length; i += 2) {
            b.header(headers[i], headers[i + 1]);
        }
    }
}
