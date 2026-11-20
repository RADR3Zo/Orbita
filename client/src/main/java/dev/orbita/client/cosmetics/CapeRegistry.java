package dev.orbita.client.cosmetics;

import com.mojang.blaze3d.platform.NativeImage;
import dev.orbita.client.OrbitaClient;
import dev.orbita.client.config.OrbitaConfig;
import dev.orbita.client.ui.theme.OrbitaTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Genera las texturas de capa (64x32, formato vanilla) en tiempo de ejecución, sin archivos externos.
 * Se usan tanto para la capa como para los élitros del jugador local.
 */
public final class CapeRegistry {
    private static final Map<CapeStyle, Identifier> TEXTURES = new EnumMap<>(CapeStyle.class);

    private CapeRegistry() {
    }

    public static CapeStyle selected() {
        return CapeStyle.byName(OrbitaConfig.get().cape);
    }

    public static void select(CapeStyle style) {
        OrbitaConfig.get().cape = style.name();
        OrbitaConfig.save();
    }

    @Nullable
    public static Identifier selectedTexture() {
        return texture(selected());
    }

    /** Devuelve la skin con la capa elegida (capa y élitros), o la misma skin si no hay capa. */
    public static net.minecraft.world.entity.player.PlayerSkin withCape(net.minecraft.world.entity.player.PlayerSkin skin) {
        Identifier cape = selectedTexture();
        if (cape == null || skin == null) {
            return skin;
        }
        // getSkin() se llama muchas veces por fotograma: se reutiliza el resultado mientras no cambie nada.
        if (skin == lastIn && cape == lastCape) {
            return lastOut;
        }
        var asset = new net.minecraft.core.ClientAsset.ResourceTexture(cape, cape);
        lastIn = skin;
        lastCape = cape;
        lastOut = new net.minecraft.world.entity.player.PlayerSkin(skin.body(), asset, asset, skin.model(), skin.secure());
        return lastOut;
    }

    private static net.minecraft.world.entity.player.PlayerSkin lastIn;
    private static net.minecraft.world.entity.player.PlayerSkin lastOut;
    private static Identifier lastCape;

    /** Debe llamarse desde el hilo de render. */
    @Nullable
    public static Identifier texture(CapeStyle style) {
        if (style == CapeStyle.NONE) {
            return null;
        }
        return TEXTURES.computeIfAbsent(style, CapeRegistry::create);
    }

    private static Identifier create(CapeStyle style) {
        int seed = style.ordinal() * 7919 + 17;
        Random random = new Random(seed);
        NativeImage image = new NativeImage(64, 32, true);
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 64; x++) {
                float n = fbm(x / 9f, y / 9f, seed);
                float v = y / 32f;
                float t = Math.max(0f, Math.min(1f, n * 0.85f + v * 0.25f - 0.05f));
                int color = t < 0.55f
                        ? OrbitaTheme.lerpColor(style.dark, style.mid, t / 0.55f)
                        : OrbitaTheme.lerpColor(style.mid, style.bright, (t - 0.55f) / 0.45f * 0.85f);
                image.setPixel(x, y, color | 0xFF000000);
            }
        }
        // Estrellas
        for (int i = 0; i < 45; i++) {
            int sx = random.nextInt(64);
            int sy = random.nextInt(32);
            int base = image.getPixel(sx, sy);
            image.setPixel(sx, sy, OrbitaTheme.lerpColor(base, 0xFFFFFFFF, 0.55f + random.nextFloat() * 0.45f));
        }
        if (style == CapeStyle.ECLIPSE) {
            ring(image, 6.0f, 8.5f, 3.6f, style.bright);
        }

        Identifier id = OrbitaClient.id("cape/" + style.name().toLowerCase(Locale.ROOT));
        Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> "orbita_cape_" + style, image));
        return id;
    }

    /** Anillo luminoso (eclipse) centrado en la cara frontal de la capa. */
    private static void ring(NativeImage image, float cx, float cy, float radius, int color) {
        for (int y = 1; y < 17; y++) {
            for (int x = 1; x < 11; x++) {
                float d = (float) Math.hypot(x + 0.5f - cx, y + 0.5f - cy);
                float k = 1f - Math.abs(d - radius) / 1.2f;
                if (d < radius - 0.6f) {
                    image.setPixel(x, y, 0xFF000000);
                } else if (k > 0f) {
                    image.setPixel(x, y, OrbitaTheme.lerpColor(image.getPixel(x, y), color, k));
                }
            }
        }
    }

    private static float fbm(float x, float y, int seed) {
        float sum = 0f;
        float amp = 0.5f;
        float freq = 1f;
        for (int i = 0; i < 4; i++) {
            sum += amp * noise(x * freq, y * freq, seed + i * 101);
            amp *= 0.5f;
            freq *= 2f;
        }
        return sum / 0.9375f;
    }

    private static float noise(float x, float y, int seed) {
        int xi = (int) Math.floor(x);
        int yi = (int) Math.floor(y);
        float xf = x - xi;
        float yf = y - yi;
        float u = xf * xf * (3 - 2 * xf);
        float v = yf * yf * (3 - 2 * yf);
        float a = hash(xi, yi, seed);
        float b = hash(xi + 1, yi, seed);
        float c = hash(xi, yi + 1, seed);
        float d = hash(xi + 1, yi + 1, seed);
        return a + (b - a) * u + (c - a) * v + (a - b - c + d) * u * v;
    }

    private static float hash(int x, int y, int seed) {
        int h = x * 374761393 + y * 668265263 + seed * 982451653;
        h = (h ^ (h >>> 13)) * 1274126177;
        return ((h ^ (h >>> 16)) & 0xFFFF) / 65535f;
    }
}
