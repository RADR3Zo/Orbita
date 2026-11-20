package dev.orbita.launcher.core.auth;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

/**
 * Hace que Gson cifre {@code accessToken} y {@code refreshToken} al guardar una {@link Account} y los
 * descifre al leerla. En memoria la cuenta siempre tiene los tokens en texto plano, así el resto del
 * launcher no tiene que saber nada del cifrado.
 */
public final class AccountJson implements TypeAdapterFactory {
    private static final String[] TOKEN_FIELDS = {"accessToken", "refreshToken"};

    @Override
    @SuppressWarnings("unchecked")
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (type.getRawType() != Account.class) {
            return null;
        }
        TypeAdapter<Account> delegate = gson.getDelegateAdapter(this, TypeToken.get(Account.class));
        TypeAdapter<JsonElement> elements = gson.getAdapter(JsonElement.class);
        return (TypeAdapter<T>) new TypeAdapter<Account>() {
            @Override
            public void write(JsonWriter out, Account value) throws IOException {
                JsonElement tree = delegate.toJsonTree(value);
                if (tree.isJsonObject()) {
                    transform(tree.getAsJsonObject(), true);
                }
                elements.write(out, tree);
            }

            @Override
            public Account read(JsonReader in) throws IOException {
                JsonElement tree = elements.read(in);
                if (tree != null && tree.isJsonObject()) {
                    transform(tree.getAsJsonObject(), false);
                }
                return delegate.fromJsonTree(tree);
            }
        };
    }

    private static void transform(JsonObject o, boolean protect) {
        for (String field : TOKEN_FIELDS) {
            JsonElement e = o.get(field);
            if (e == null || !e.isJsonPrimitive()) {
                continue;
            }
            String value = e.getAsString();
            String result = protect ? TokenVault.protect(value) : TokenVault.reveal(value);
            if (result == null) {
                // No se pudo descifrar (launcher.json copiado de otro equipo): hay que volver a iniciar sesión.
                o.remove(field);
                if ("accessToken".equals(field)) {
                    o.addProperty("expiresAt", 0L);
                }
            } else {
                o.addProperty(field, result);
            }
        }
    }
}
