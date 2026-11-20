package dev.orbita.client.account;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Cuentas guardadas, compartidas con el launcher de Órbita: se leen y escriben en .orbita/launcher.json
 * ("accounts" y "activeAccount"), así una cuenta añadida en el juego también aparece en el launcher y al revés.
 */
public final class Accounts {
    /** Misma forma que el Account del launcher. type: "msa" (Microsoft) o "legacy" (sin premium). */
    public record Acc(String type, String name, String uuid, String accessToken, String refreshToken, long expiresAt, String xuid) {
        public boolean premium() {
            return "msa".equals(type);
        }

        public boolean expired() {
            return System.currentTimeMillis() > expiresAt - 60_000;
        }

        public static Acc offline(String name) {
            UUID id = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
            return new Acc("legacy", name, id.toString().replace("-", ""), "0", null, Long.MAX_VALUE, "0");
        }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private Accounts() {
    }

    private static Path file() {
        return FabricLoader.getInstance().getGameDir().resolve("launcher.json");
    }

    private static synchronized JsonObject read() {
        try {
            JsonElement e = JsonParser.parseString(Files.readString(file(), StandardCharsets.UTF_8));
            if (e.isJsonObject()) {
                return e.getAsJsonObject();
            }
        } catch (Exception ignored) {
            // no existe o está dañado: se crea
        }
        return new JsonObject();
    }

    private static synchronized void write(JsonObject o) {
        try {
            Path f = file();
            Path tmp = f.resolveSibling("launcher.json.tmp");
            Files.writeString(tmp, GSON.toJson(o), StandardCharsets.UTF_8);
            Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
            // se reintenta en el próximo cambio
        }
    }

    /** Lista de cuentas; si está vacía, se crea con la cuenta con la que se abrió el juego. */
    public static synchronized List<Acc> list() {
        JsonObject o = read();
        List<Acc> out = new ArrayList<>();
        if (o.has("accounts") && o.get("accounts").isJsonArray()) {
            for (JsonElement e : o.getAsJsonArray("accounts")) {
                try {
                    Acc a = GSON.fromJson(e, Acc.class);
                    if (a != null && a.name() != null && a.uuid() != null) {
                        out.add(a);
                    }
                } catch (Exception ignored) {
                    // cuenta dañada: se salta
                }
            }
        }
        if (out.isEmpty()) {
            out.add(fromCurrent());
            save(o, out, 0);
        }
        return out;
    }

    /** Índice de la cuenta con la que se juega ahora (la del juego; si no está en la lista, la guardada). */
    public static synchronized int active() {
        List<Acc> l = list();
        User u = Minecraft.getInstance().getUser();
        String id = u.getProfileId().toString().replace("-", "");
        for (int i = 0; i < l.size(); i++) {
            if (l.get(i).uuid().replace("-", "").equalsIgnoreCase(id)) {
                return i;
            }
        }
        JsonObject o = read();
        int a = o.has("activeAccount") ? o.get("activeAccount").getAsInt() : 0;
        return Math.max(0, Math.min(l.size() - 1, a));
    }

    private static Acc fromCurrent() {
        User u = Minecraft.getInstance().getUser();
        String token = u.getAccessToken();
        boolean premium = token != null && !token.isEmpty() && !"0".equals(token);
        return premium
                ? new Acc("msa", u.getName(), u.getProfileId().toString().replace("-", ""), token, null, System.currentTimeMillis() + 3_600_000L, u.getXuid().orElse("0"))
                : Acc.offline(u.getName());
    }

    private static void save(JsonObject o, List<Acc> l, int active) {
        JsonArray arr = new JsonArray();
        for (Acc a : l) {
            arr.add(GSON.toJsonTree(a));
        }
        o.add("accounts", arr);
        o.addProperty("activeAccount", Math.max(0, Math.min(l.size() - 1, active)));
        Acc cur = l.get(Math.max(0, Math.min(l.size() - 1, active)));
        if (!cur.premium()) {
            o.addProperty("username", cur.name());
        }
        o.remove("account"); // formato viejo de una sola cuenta
        write(o);
    }

    /** Añade (o actualiza, si ya estaba) y la deja como activa. Devuelve su índice. */
    public static synchronized int add(Acc acc) {
        List<Acc> l = new ArrayList<>(list());
        int i = indexOf(l, acc);
        if (i >= 0) {
            l.set(i, acc);
        } else {
            l.add(acc);
            i = l.size() - 1;
        }
        save(read(), l, i);
        return i;
    }

    public static synchronized void select(int index) {
        List<Acc> l = list();
        if (index >= 0 && index < l.size()) {
            save(read(), l, index);
        }
    }

    public static synchronized void remove(int index) {
        List<Acc> l = new ArrayList<>(list());
        if (index < 0 || index >= l.size() || l.size() <= 1) {
            return;
        }
        int active = active();
        l.remove(index);
        if (active >= index && active > 0) {
            active--;
        }
        save(read(), l, active);
    }

    private static int indexOf(List<Acc> l, Acc acc) {
        for (int i = 0; i < l.size(); i++) {
            Acc a = l.get(i);
            if (a.type().equals(acc.type()) && (a.premium() ? a.uuid().equalsIgnoreCase(acc.uuid()) : a.name().equalsIgnoreCase(acc.name()))) {
                return i;
            }
        }
        return -1;
    }
}
