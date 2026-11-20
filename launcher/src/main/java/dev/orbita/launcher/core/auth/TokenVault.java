package dev.orbita.launcher.core.auth;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Cifra los tokens de las cuentas antes de guardarlos en launcher.json.
 *
 * <p>En Windows usa DPAPI (la misma protección que usan Chrome o el launcher oficial): solo tu usuario de
 * Windows, en este equipo, puede descifrarlos. Si alguien copia launcher.json a otra PC, los tokens no sirven.
 *
 * <p>Formato guardado: {@code "dpapi:<base64>"}. Un valor sin prefijo es texto plano (archivos viejos):
 * se lee tal cual y se cifra la próxima vez que se guarde.
 *
 * <p>IMPORTANTE: hay una copia idéntica de esta clase en el mod cliente
 * ({@code dev.orbita.client.account.TokenVault}). Si cambias una, cambia la otra.
 */
public final class TokenVault {
    public static final String PREFIX = "dpapi:";
    /** Entropía extra: un token cifrado por otro programa con DPAPI no se confunde con uno de Órbita. */
    private static final String ENTROPY = "Orbita-tokens-v1";

    /** Para no lanzar PowerShell cada vez: cifrado ↔ texto plano ya conocidos en esta sesión. */
    private static final Map<String, String> PLAIN_BY_CIPHER = new ConcurrentHashMap<>();
    private static final Map<String, String> CIPHER_BY_PLAIN = new ConcurrentHashMap<>();

    /** Se puede cambiar en pruebas para no depender de Windows. */
    static Backend backend = isWindows() ? new DpapiBackend() : null;

    private TokenVault() {
    }

    /** Devuelve el token listo para guardar. null, vacío y "0" (cuentas offline) se dejan igual. */
    public static String protect(String plain) {
        if (!needsProtection(plain) || plain.startsWith(PREFIX)) {
            return plain;
        }
        String cached = CIPHER_BY_PLAIN.get(plain);
        if (cached != null) {
            return cached;
        }
        if (backend == null) {
            return plain; // fuera de Windows no hay DPAPI: se guarda como antes
        }
        try {
            String cipher = PREFIX + backend.protect(plain);
            CIPHER_BY_PLAIN.put(plain, cipher);
            PLAIN_BY_CIPHER.put(cipher, plain);
            return cipher;
        } catch (IOException e) {
            System.err.println("[Órbita] No se pudo cifrar un token, se guarda sin cifrar: " + e.getMessage());
            return plain;
        }
    }

    /** Devuelve el token en texto plano. Si no se puede descifrar (otro equipo u otro usuario) devuelve null. */
    public static String reveal(String stored) {
        if (stored == null || !stored.startsWith(PREFIX)) {
            return stored;
        }
        String cached = PLAIN_BY_CIPHER.get(stored);
        if (cached != null) {
            return cached;
        }
        if (backend == null) {
            return null;
        }
        try {
            String plain = backend.reveal(stored.substring(PREFIX.length()));
            PLAIN_BY_CIPHER.put(stored, plain);
            CIPHER_BY_PLAIN.put(plain, stored);
            return plain;
        } catch (IOException e) {
            System.err.println("[Órbita] No se pudo descifrar un token (¿launcher.json de otro equipo?): " + e.getMessage());
            return null;
        }
    }

    static boolean needsProtection(String value) {
        return value != null && !value.isEmpty() && !"0".equals(value);
    }

    static void clearCache() {
        PLAIN_BY_CIPHER.clear();
        CIPHER_BY_PLAIN.clear();
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    /** Quién cifra de verdad. Recibe y devuelve base64 del lado cifrado. */
    interface Backend {
        String protect(String plain) throws IOException;

        String reveal(String cipherBase64) throws IOException;
    }

    /**
     * DPAPI a través de PowerShell (viene con Windows 10/11), sin librerías nativas extra.
     * Los datos van por la entrada estándar, nunca en la línea de comandos, para que no se vean
     * en el Administrador de tareas.
     */
    static final class DpapiBackend implements Backend {
        private static final String SCRIPT_TEMPLATE =
                "Add-Type -AssemblyName System.Security;"
                        + "$i=[Console]::In.ReadToEnd().Trim();"
                        + "$e=[Text.Encoding]::UTF8.GetBytes('" + ENTROPY + "');"
                        + "$b=[Convert]::FromBase64String($i);"
                        + "$o=[Security.Cryptography.ProtectedData]::%s($b,$e,'CurrentUser');"
                        + "[Console]::Out.Write([Convert]::ToBase64String($o))";

        @Override
        public String protect(String plain) throws IOException {
            String in = Base64.getEncoder().encodeToString(plain.getBytes(StandardCharsets.UTF_8));
            return run("Protect", in);
        }

        @Override
        public String reveal(String cipherBase64) throws IOException {
            String out = run("Unprotect", cipherBase64);
            return new String(Base64.getDecoder().decode(out), StandardCharsets.UTF_8);
        }

        private static String run(String op, String input) throws IOException {
            Process p = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass",
                    "-Command", String.format(SCRIPT_TEMPLATE, op))
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            try (OutputStream os = p.getOutputStream()) {
                os.write(input.getBytes(StandardCharsets.US_ASCII));
            }
            String out;
            try (InputStream is = p.getInputStream()) {
                out = new String(is.readAllBytes(), StandardCharsets.US_ASCII).trim();
            }
            try {
                if (!p.waitFor(20, TimeUnit.SECONDS)) {
                    p.destroyForcibly();
                    throw new IOException("PowerShell tardó demasiado");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrumpido", e);
            }
            if (p.exitValue() != 0 || out.isEmpty()) {
                throw new IOException("DPAPI " + op + " falló (código " + p.exitValue() + ")");
            }
            return out;
        }
    }
}
