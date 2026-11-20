package dev.orbita.client.nova.module;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import dev.orbita.client.nova.NovaClient;
import dev.orbita.client.nova.core.Setting;
import dev.orbita.client.nova.core.SkyPainter;
import dev.orbita.client.nova.video.VideoPlayer;
import org.joml.Matrix4f;

import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FogType;

/**
 * Cielo personalizado. La textura 360 grados se calcula en un hilo aparte (sin tirones) y se dibuja en una
 * esfera fina alrededor de la camara, antes de las entidades y sin escribir profundidad.
 * Calidad Low/Medium/High = 256/512/1024 px de ancho; el modo ahorro fuerza Low.
 */
public final class SkyModule extends Module {
    private static final String[] STYLES = java.util.stream.Stream.concat(java.util.Arrays.stream(SkyPainter.MODES), java.util.stream.Stream.of("Video")).toArray(String[]::new);
    public final Setting.Choice mode = add(new Setting.Choice("Style", 0, STYLES));
    public final Setting.Num tiles = add(new Setting.Num("Video tiles", 2, 1, 4, 1)).showIf(() -> isVideo());
    public final Setting.Choice quality = add(new Setting.Choice("Sky quality", 3, SkyPainter.QUALITY));
    public final Setting.Col color = add(new Setting.Col("Color", 0.80f, 0.55f));
    public final Setting.Num speed = add(new Setting.Num("Speed", 1.0, 0.0, 4.0, 0.1));
    public final Setting.Num brightness = add(new Setting.Num("Brightness", 1.0, 0.3, 2.0, 0.1));

    private static final Identifier TEX = Identifier.fromNamespaceAndPath("nova", "sky_dynamic");
    private static final int ROWS = 48, COLS = 96; // esfera fina: sin facetas visibles

    private DynamicTexture texture;
    private int texW, texH;
    private volatile int[] ready;          // ultimo resultado del hilo de calculo
    private final AtomicBoolean busy = new AtomicBoolean();
    private double clock;
    private float drift;

    public SkyModule() { super("Sky Changer", "Change the sky: nebula, aurora, cells...", Category.SKY); }
    @Override public String info() { return dev.orbita.client.nova.core.Lang.t(mode.get()); }

    private boolean isVideo() { return mode.index == SkyPainter.MODES.length; }

    /** Color medio del cielo justo encima del horizonte: la niebla lo usa para no quedar celeste. */
    private volatile int horizon = -1;

    /** Solo en el mundo normal y con la camara fuera del agua/lava (ahi manda la niebla de Minecraft). */
    private static boolean showsSky(Camera camera, ClientLevel world) {
        return world != null && world.dimension() == Level.OVERWORLD
                && (camera == null || camera.getFluidInCamera() == FogType.NONE);
    }

    /** Lo llama BackgroundRendererMixin: color RGB para la niebla, o -1 para dejar el de Minecraft. */
    public static int fogColor(Camera camera, ClientLevel world) {
        if (NovaClient.modules == null) return -1;
        SkyModule m = NovaClient.modules.get(SkyModule.class);
        if (m == null || !m.enabled || ClientSettings.eco() || !showsSky(camera, world)) return -1;
        return m.isVideo() ? 0x0B0B12 : m.horizon;
    }

    /** Modo ahorro: se usa el cielo normal de Minecraft (nuestro cielo se dibuja encima y cuesta FPS). */
    @Override public boolean ecoSkip() { return true; }

    private int effectiveQuality() { return ClientSettings.eco() ? 0 : quality.index; }

    /** Cielo calculado en la tarjeta grafica (nitido y suave). Si el shader no carga, se usa la imagen. */
    private boolean shaderFailed;
    private int paintCooldown;

    private boolean useShader() { return effectiveQuality() == 3 && !shaderFailed; }

    @Override public void onDisable() {
        if (texture != null) {
            mc().getTextureManager().release(TEX);
            texture = null;
        }
        ready = null;
        horizon = -1;
    }

    @Override public void onTick() {
        if (isVideo()) return; // el video lo decodifica VideoPlayer (modulo "Video Background")
        clock += speed.value;
        drift += 0.0009f * (float) speed.value; // rotacion continua: da movimiento fluido entre actualizaciones

        int q = effectiveQuality(), w = SkyPainter.QW[q], h = w / 2;
        if (texture != null && (texW != w)) onDisable(); // cambio de calidad: se recrea la textura

        // pasa el resultado listo a la GPU (hilo principal)
        int[] px = ready;
        if (px != null && px.length == w * h) {
            ready = null;
            upload(px, w, h);
        }
        // pide un nuevo calculo si el anterior termino (con el shader, solo de vez en cuando: es para la niebla)
        if (useShader() && texture != null && paintCooldown-- > 0) return;
        paintCooldown = 40;
        if (busy.compareAndSet(false, true)) {
            final int mo = mode.index, rgb = color.rgb();
            final double t = clock, br = brightness.value;
            Thread th = new Thread(() -> {
                try {
                    int[] out = new int[w * h];
                    SkyPainter.paint(out, w, h, mo, rgb, t, br);
                    horizon = SkyPainter.horizonColor(out, w, h);
                    ready = out;
                } finally { busy.set(false); }
            }, "nova-sky");
            th.setDaemon(true);
            th.setPriority(Thread.MIN_PRIORITY + 1);
            th.start();
        }
    }

    private void upload(int[] px, int w, int h) {
        if (texture == null) {
            texture = new DynamicTexture(() -> "nova", w, h, false);
            texW = w; texH = h;
            mc().getTextureManager().register(TEX, texture);
            dev.orbita.client.nova.compat.V.filter(texture, true, false);
        }
        NativeImage img = texture.getPixels();
        if (img == null) return;
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                dev.orbita.client.nova.compat.V.setArgb(img, x, y, px[y * w + x]);
        texture.upload();
    }

    @Override public void onWorldBefore(dev.orbita.client.nova.gfx.WCtx ctx) {
        boolean video = isVideo();
        if (!showsSky(ctx.camera(), mc().level)) return;
        if (video) { if (!VideoPlayer.MENU.pump()) return; }
        float radius = Math.max(64f, mc().gameRenderer.getDepthFar() * 0.9f);
        Matrix4f m = ctx.matrixStack() != null ? new Matrix4f(ctx.matrixStack().last().pose()) : new Matrix4f();
        if (!video && useShader() && drawShader(ctx, m, radius)) return;
        if (!video && texture == null) return;

        net.minecraft.resources.Identifier tex = video ? VideoPlayer.MENU.TEX : TEX;
        BufferBuilder b = dev.orbita.client.nova.compat.V.begin(dev.orbita.client.nova.compat.V.Mode.SKY_TEX, tex);
        for (int i = 0; i < ROWS; i++) {
            float v0 = i / (float) ROWS, v1 = (i + 1) / (float) ROWS;
            for (int j = 0; j < COLS; j++) {
                float u0 = j / (float) COLS, u1 = (j + 1) / (float) COLS;
                vert(b, m, radius, u0, v0, video); vert(b, m, radius, u1, v0, video);
                vert(b, m, radius, u1, v1, video); vert(b, m, radius, u0, v1, video);
            }
        }
        dev.orbita.client.nova.compat.V.end(dev.orbita.client.nova.compat.V.Mode.SKY_TEX, tex, b);
    }

    private boolean drawShader(dev.orbita.client.nova.gfx.WCtx ctx, Matrix4f m, float radius) {
        float pt = ctx.tickDelta();
        int rgb = color.rgb();
        float[] hsb = java.awt.Color.RGBtoHSB((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, null);
        int rgb2 = java.awt.Color.HSBtoRGB((hsb[0] + 0.14f) % 1f, Math.min(1f, hsb[1] * 0.9f), Math.min(1f, hsb[2] * 1.05f));
        boolean ok = dev.orbita.client.nova.compat.V.skyShader((float) (clock + pt * speed.value),
                new float[]{((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f},
                new float[]{((rgb2 >> 16) & 255) / 255f, ((rgb2 >> 8) & 255) / 255f, (rgb2 & 255) / 255f},
                new float[]{(float) mode.index, brightness.f(), drift + 0.0009f * (float) speed.value * pt, 0f});
        if (!ok) {
            shaderFailed = true;
            dev.orbita.client.nova.NovaClient.LOG.warn("Nova: el shader del cielo no cargo; se usa la imagen");
            return false;
        }
        BufferBuilder b = dev.orbita.client.nova.compat.V.begin(dev.orbita.client.nova.compat.V.Mode.SKY_SHADER, null);
        int rows = 24, cols = 48; // la forma da igual: el color sale del shader por pixel
        for (int i = 0; i < rows; i++) {
            float v0 = i / (float) rows, v1 = (i + 1) / (float) rows;
            for (int j = 0; j < cols; j++) {
                float u0 = j / (float) cols, u1 = (j + 1) / (float) cols;
                pos(b, m, radius, u0, v0); pos(b, m, radius, u1, v0); pos(b, m, radius, u1, v1); pos(b, m, radius, u0, v1);
            }
        }
        dev.orbita.client.nova.compat.V.end(dev.orbita.client.nova.compat.V.Mode.SKY_SHADER, null, b);
        return true;
    }


    private static void pos(BufferBuilder b, Matrix4f m, float r, float u, float v) {
        double az = u * Math.PI * 2, el = (0.5 - v) * Math.PI;
        float cy = (float) Math.cos(el);
        dev.orbita.client.nova.compat.V.skyVertex(b, m, r * cy * (float) Math.cos(az), r * (float) Math.sin(el), r * cy * (float) Math.sin(az));
    }

    private void vert(BufferBuilder b, Matrix4f m, float r, float u, float v, boolean video) {
        double az = u * Math.PI * 2 + drift, el = (0.5 - v) * Math.PI;
        float cy = (float) Math.cos(el);
        float x = r * cy * (float) Math.cos(az), y = r * (float) Math.sin(el), z = r * cy * (float) Math.sin(az);
        if (!video) { b.addVertex(m, x, y, z).setUv(u, v).setColor(0xFFFFFFFF); return; }
        // video: se repite en espejo (sin costura) y la parte baja se apaga hacia el horizonte
        double uu = u * tiles.value * 2; uu = uu % 2; if (uu > 1) uu = 2 - uu;
        float tv = (float) Math.max(0, Math.min(1, 1 - el / (Math.PI / 2)));
        int shade = (int) (255 * Math.max(0, Math.min(1, 1 + el * 5)));
        b.addVertex(m, x, y, z).setUv((float) uu, tv).setColor(0xFF000000 | (shade << 16) | (shade << 8) | shade);
    }
}
