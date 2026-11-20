package dev.orbita.launcher.core.install;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RulesTest {
    private static JsonArray rules(String json) {
        return JsonParser.parseString(json).getAsJsonArray();
    }

    @Test
    void sinReglasSePermite() {
        assertTrue(Rules.allowed(null));
        assertTrue(Rules.allowed(new JsonArray()));
    }

    @Test
    void soloParaOtroSistema() {
        assertFalse(Rules.allowed(rules("[{\"action\":\"allow\",\"os\":{\"name\":\"sistema-que-no-existe\"}}]")));
    }

    @Test
    void permitirTodoMenosOtroSistema() {
        assertTrue(Rules.allowed(rules("""
                [{"action":"allow"}, {"action":"disallow","os":{"name":"sistema-que-no-existe"}}]
                """)));
    }

    @Test
    void features() {
        JsonArray demo = rules("[{\"action\":\"allow\",\"features\":{\"is_demo_user\":true}}]");
        assertFalse(Rules.allowed(demo));
        assertTrue(Rules.allowed(demo, Map.of("is_demo_user", true)));
    }
}
