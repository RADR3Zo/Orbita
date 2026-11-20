package dev.orbita.launcher.core.mods;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.orbita.launcher.LauncherInfo;
import dev.orbita.launcher.core.Http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Cliente de la API v2 de Modrinth, limitado a mods Fabric para la versión de Minecraft de Órbita. */
public final class ModrinthClient {
    private static final String API = "https://api.modrinth.com/v2";

    public record SearchHit(String projectId, String slug, String title, String description, String author,
                            long downloads, String iconUrl) {
    }

    public record Dependency(String projectId, String versionId, String type) {
    }

    public record ModVersion(String id, String projectId, String versionNumber, String fileName, String url,
                             String sha512, long size, List<Dependency> dependencies, List<String> gameVersions) {
    }

    public record Project(String id, String slug, String title, String description, String iconUrl) {
    }

    private final String gameVersion = LauncherInfo.MINECRAFT;

    public List<SearchHit> search(String query, int limit) throws IOException {
        String facets = "[[\"categories:fabric\"],[\"versions:" + gameVersion + "\"],[\"project_type:mod\"]]";
        String url = API + "/search?limit=" + limit
                + "&index=" + (query.isBlank() ? "downloads" : "relevance")
                + "&query=" + Http.encode(query)
                + "&facets=" + Http.encode(facets);
        JsonObject res = Http.getJson(url).getAsJsonObject();
        List<SearchHit> hits = new ArrayList<>();
        for (JsonElement e : res.getAsJsonArray("hits")) {
            JsonObject h = e.getAsJsonObject();
            hits.add(new SearchHit(str(h, "project_id"), str(h, "slug"), str(h, "title"), str(h, "description"),
                    str(h, "author"), h.has("downloads") ? h.get("downloads").getAsLong() : 0, str(h, "icon_url")));
        }
        return hits;
    }

    /** Versión más reciente compatible (prefiere "release" sobre beta/alpha). */
    public Optional<ModVersion> latestVersion(String idOrSlug) throws IOException {
        String url = API + "/project/" + Http.encode(idOrSlug) + "/version"
                + "?loaders=" + Http.encode("[\"fabric\"]")
                + "&game_versions=" + Http.encode("[\"" + gameVersion + "\"]");
        Http.Response r = Http.get(url, "Accept", "application/json");
        if (r.status() == 404) {
            return Optional.empty();
        }
        if (!r.ok()) {
            throw new IOException("Modrinth respondió HTTP " + r.status());
        }
        JsonArray versions = r.json().getAsJsonArray();
        JsonObject best = null;
        for (JsonElement e : versions) {
            JsonObject v = e.getAsJsonObject();
            if (best == null) {
                best = v;
            }
            if ("release".equals(str(v, "version_type"))) {
                best = v;
                break;
            }
        }
        return best == null ? Optional.empty() : Optional.of(parseVersion(best));
    }

    /** Identifica archivos locales por su SHA-512. */
    public Map<String, ModVersion> versionsByHash(Collection<String> hashes) throws IOException {
        if (hashes.isEmpty()) {
            return Map.of();
        }
        JsonObject body = new JsonObject();
        body.add("hashes", toArray(hashes));
        body.addProperty("algorithm", "sha512");
        return parseHashMap(Http.postJson(API + "/version_files", body.toString()));
    }

    /** Última versión compatible de cada archivo local, indexada por el hash del archivo local. */
    public Map<String, ModVersion> latestByHash(Collection<String> hashes) throws IOException {
        if (hashes.isEmpty()) {
            return Map.of();
        }
        JsonObject body = new JsonObject();
        body.add("hashes", toArray(hashes));
        body.addProperty("algorithm", "sha512");
        body.add("loaders", toArray(List.of("fabric")));
        body.add("game_versions", toArray(List.of(gameVersion)));
        return parseHashMap(Http.postJson(API + "/version_files/update", body.toString()));
    }

    public Map<String, Project> projects(Collection<String> ids) throws IOException {
        if (ids.isEmpty()) {
            return Map.of();
        }
        JsonArray arr = toArray(ids);
        JsonArray res = Http.getJson(API + "/projects?ids=" + Http.encode(arr.toString())).getAsJsonArray();
        Map<String, Project> out = new HashMap<>();
        for (JsonElement e : res) {
            JsonObject p = e.getAsJsonObject();
            out.put(str(p, "id"), new Project(str(p, "id"), str(p, "slug"), str(p, "title"), str(p, "description"), str(p, "icon_url")));
        }
        return out;
    }

    private static Map<String, ModVersion> parseHashMap(Http.Response r) throws IOException {
        if (!r.ok()) {
            throw new IOException("Modrinth respondió HTTP " + r.status());
        }
        Map<String, ModVersion> out = new HashMap<>();
        for (Map.Entry<String, JsonElement> e : r.json().getAsJsonObject().entrySet()) {
            out.put(e.getKey(), parseVersion(e.getValue().getAsJsonObject()));
        }
        return out;
    }

    private static ModVersion parseVersion(JsonObject v) {
        JsonArray files = v.getAsJsonArray("files");
        JsonObject file = files.get(0).getAsJsonObject();
        for (JsonElement f : files) {
            if (f.getAsJsonObject().has("primary") && f.getAsJsonObject().get("primary").getAsBoolean()) {
                file = f.getAsJsonObject();
                break;
            }
        }
        List<Dependency> deps = new ArrayList<>();
        if (v.has("dependencies")) {
            for (JsonElement d : v.getAsJsonArray("dependencies")) {
                JsonObject o = d.getAsJsonObject();
                deps.add(new Dependency(str(o, "project_id"), str(o, "version_id"), str(o, "dependency_type")));
            }
        }
        List<String> games = new ArrayList<>();
        if (v.has("game_versions")) {
            for (JsonElement gv : v.getAsJsonArray("game_versions")) {
                games.add(gv.getAsString());
            }
        }
        return new ModVersion(str(v, "id"), str(v, "project_id"), str(v, "version_number"),
                str(file, "filename"), str(file, "url"),
                file.getAsJsonObject("hashes").get("sha512").getAsString(),
                file.has("size") ? file.get("size").getAsLong() : -1, deps, games);
    }

    private static JsonArray toArray(Collection<String> values) {
        JsonArray arr = new JsonArray();
        values.forEach(arr::add);
        return arr;
    }

    private static String str(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : null;
    }
}
