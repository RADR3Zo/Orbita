package dev.orbita.client.nova.compat;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

/**
 * Todo lo que cambia de una version de Minecraft a otra al dibujar, en un solo lugar.
 * Version 1.21.9 - 1.21.11: se dibuja con capas (RenderLayer) que usan "pipelines" en vez de shaders sueltos.
 */
public final class V {
    private V() { }

    // ------------------------------------------------------------------ GUI (1.21.6+: se dibuja con pipelines y matrices 2D)
    public static void tex(GuiGraphics c, Identifier id, int x, int y, float u, float v, int w, int h, int rw, int rh, int tw, int th, int color) {
        c.blit(RenderPipelines.GUI_TEXTURED, id, x, y, u, v, w, h, rw, rh, tw, th, color);
    }

    public static void tex(GuiGraphics c, Identifier id, int x, int y, float u, float v, int w, int h, int tw, int th, int color) {
        tex(c, id, x, y, u, v, w, h, w, h, tw, th, color);
    }

    // ------------------------------------------------------------------ dibujo en el mundo
    public enum Mode { COLOR_ADD, TEX_ADD, SKY_TEX, SKY_SHADER }

    private static final RenderPipeline COLOR_ADD_PIPE = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("nova", "pipeline/color_add")).withBlend(BlendFunction.LIGHTNING)
            .withCull(false).withDepthWrite(false).withVertexFormat(com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS).build();
    private static final RenderPipeline TEX_ADD_PIPE = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("nova", "pipeline/tex_add")).withBlend(BlendFunction.LIGHTNING)
            .withCull(false).withDepthWrite(false).withVertexFormat(com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS).build();
    private static final RenderPipeline SKY_TEX_PIPE = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("nova", "pipeline/sky_tex")).withoutBlend()
            .withCull(false).withDepthWrite(false).withVertexFormat(com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS).build();

    /** Cielo animado (1.21.5+): los datos van en cada vertice, ver shaders/core/sky2. */
    private static final RenderPipeline SKY_SHADER_PIPE = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("nova", "pipeline/sky_shader")).withVertexShader(Identifier.fromNamespaceAndPath("nova", "core/sky2")).withFragmentShader(Identifier.fromNamespaceAndPath("nova", "core/sky2"))
            .withoutBlend().withCull(false).withDepthWrite(false).withVertexFormat(com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS).build();
    private static RenderType skyShaderLayer;

    private static RenderType colorAdd;
    private static final Map<Identifier, RenderType> TEX_ADD = new HashMap<>(), SKY = new HashMap<>();

    private static RenderType layer(Mode m, Identifier tex) {
        return switch (m) {
            case COLOR_ADD -> colorAdd != null ? colorAdd : (colorAdd = RenderType.create("nova_color_add", net.minecraft.client.renderer.rendertype.RenderSetup.builder(COLOR_ADD_PIPE).sortOnUpload().createRenderSetup()));
            case SKY_SHADER -> skyShaderLayer != null ? skyShaderLayer : (skyShaderLayer = RenderType.create("nova_sky_shader", net.minecraft.client.renderer.rendertype.RenderSetup.builder(SKY_SHADER_PIPE).createRenderSetup()));
            case TEX_ADD -> TEX_ADD.computeIfAbsent(tex, id -> RenderType.create("nova_tex_add", net.minecraft.client.renderer.rendertype.RenderSetup.builder(TEX_ADD_PIPE).withTexture("Sampler0", id).sortOnUpload().createRenderSetup()));
            default -> SKY.computeIfAbsent(tex, id -> RenderType.create("nova_sky", net.minecraft.client.renderer.rendertype.RenderSetup.builder(SKY_TEX_PIPE).withTexture("Sampler0", id).createRenderSetup()));
        };
    }

    public static BufferBuilder begin(Mode m, Identifier tex) {
        var fmt = m == Mode.COLOR_ADD ? com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR
                : com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX_COLOR;
        return Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, fmt);
    }

    public static void end(Mode m, Identifier tex, BufferBuilder b) {
        MeshData built = b.build();
        if (built != null) layer(m, tex).draw(built);
    }

    private static float skyTime, skyDrift; private static int skyColor;

    /** Guarda los datos del cielo animado; skyVertex los escribe en cada vertice. */
    public static boolean skyShader(float time, float[] col, float[] col2, float[] params) {
        // UV0.x = tiempo, UV0.y = estilo * 10 + giro (0..2pi), alfa = brillo / 4
        float twoPi = (float) (Math.PI * 2);
        skyTime = time; skyDrift = ((int) params[0]) * 10f + ((params[2] % twoPi) + twoPi) % twoPi;
        int a = Math.max(0, Math.min(255, Math.round(params[1] / 4f * 255)));
        skyColor = (a << 24) | (Math.round(col[0] * 255) << 16) | (Math.round(col[1] * 255) << 8) | Math.round(col[2] * 255);
        return true;
    }

    public static void skyVertex(BufferBuilder b, org.joml.Matrix4f m, float x, float y, float z) {
        b.addVertex(m, x, y, z).setUv(skyTime, skyDrift).setColor(skyColor);
    }

    // ------------------------------------------------------------------ imagenes, cajas, objetos
    public static void setArgb(com.mojang.blaze3d.platform.NativeImage img, int x, int y, int argb) { img.setPixel(x, y, argb); }
    public static int getArgb(com.mojang.blaze3d.platform.NativeImage img, int x, int y) { return img.getPixel(x, y); }

    public static void drawBox(com.mojang.blaze3d.vertex.PoseStack ms, VertexConsumer lines, net.minecraft.world.phys.AABB box, float r, float g, float b, float a) {
        int argb = ((int) (a * 255) << 24) | ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
        net.minecraft.client.renderer.ShapeRenderer.renderShape(ms, lines, net.minecraft.world.phys.shapes.Shapes.create(box), 0, 0, 0, argb, 1f);
    }

    public static void stackOverlay(GuiGraphics c, net.minecraft.client.gui.Font tr, net.minecraft.world.item.ItemStack s, int x, int y) { c.renderItemDecorations(tr, s, x, y); }
    public static void stackOverlay(GuiGraphics c, net.minecraft.client.gui.Font tr, net.minecraft.world.item.ItemStack s, int x, int y, String text) { c.renderItemDecorations(tr, s, x, y, text); }

    public static boolean coolingDown(net.minecraft.world.entity.player.Player p, net.minecraft.world.item.ItemStack s) { return p.getCooldowns().isOnCooldown(s); }
    public static float cooldown(net.minecraft.world.entity.player.Player p, net.minecraft.world.item.ItemStack s, float td) { return p.getCooldowns().getCooldownPercent(s, td); }

    private static java.util.function.Supplier<net.minecraft.world.entity.player.PlayerSkin> skin;

    /** 1.21.9+: la skin llega por un Supplier (se pide una vez; mientras baja, da la de por defecto). */
    private static net.minecraft.world.entity.player.PlayerSkin skinTextures() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (skin == null) skin = mc.getSkinManager().createLookup(mc.getGameProfile(), false);
        return skin.get();
    }

    /** Textura de la skin del jugador de la sesion. */
    public static Identifier skin() { return skinTextures().body().texturePath(); }

    /** Skin de brazos finos (modelo "Alex"). */
    public static boolean skinSlim() { return skinTextures().model() == net.minecraft.world.entity.player.PlayerModelType.SLIM; }

    public static void init() { }

    // ------------------------------------------------------------------ matriz de la GUI (2D)
    public static void push(GuiGraphics c) { c.pose().pushMatrix(); }
    public static void pop(GuiGraphics c) { c.pose().popMatrix(); }
    public static void translate(GuiGraphics c, double x, double y) { c.pose().translate((float) x, (float) y); }
    public static void scale(GuiGraphics c, float x, float y) { c.pose().scale(x, y); }
    public static void rotate(GuiGraphics c, float radians) { c.pose().rotate(radians); }

    /** Linea gruesa con textura: un rectangulo de 1x1 girado y estirado de a hasta b. */
    public static void lineQuad(GuiGraphics c, Identifier tex, float ax, float ay, float bx, float by, float nx, float ny, int color) {
        float dx = bx - ax, dy = by - ay, len = (float) Math.sqrt(dx * dx + dy * dy), half = (float) Math.sqrt(nx * nx + ny * ny);
        if (len < 1e-4f) return;
        push(c);
        c.pose().translate(ax, ay);
        c.pose().rotate((float) Math.atan2(dy, dx));
        c.pose().translate(0, -half);
        c.pose().scale(len, half * 2);
        c.blit(RenderPipelines.GUI_TEXTURED, tex, 0, 0, 0f, 0f, 1, 1, 1, 1, 1, 1, color);
        pop(c);
    }

    /** Sale del mundo o del servidor (como el boton de la pausa de Minecraft). */
    public static void disconnect(net.minecraft.client.Minecraft client, boolean singleplayer) {
        if (client.level != null) client.level.disconnect(net.minecraft.client.multiplayer.ClientLevel.DEFAULT_QUIT_MESSAGE);
        if (singleplayer) client.disconnectWithSavingScreen(); else client.disconnectWithProgressScreen();
    }

    /** Dibuja una entidad en la GUI (vista previa del jugador). */
    public static void drawEntity(GuiGraphics c, int x1, int y1, int x2, int y2, float cx, float cy, float size, org.joml.Vector3f off,
                                  org.joml.Quaternionf rot, org.joml.Quaternionf tilt, net.minecraft.world.entity.LivingEntity e) {
        // 1.21.11 solo deja dibujarla mirando al raton: se apunta al centro (mira de frente)
        net.minecraft.client.gui.screens.inventory.InventoryScreen.renderEntityInInventoryFollowsMouse(c, x1, y1, x2, y2, Math.round(size), 0.0625f, cx, cy, e);
    }

    private static final org.joml.Matrix4f VIEW = new org.joml.Matrix4f(), PROJ = new org.joml.Matrix4f();

    /** Engancha el dibujo de Nova antes y despues de las entidades (1.21.9+: Fabric separa "extraer" de "dibujar"). */
    public static void registerWorld(java.util.function.Consumer<dev.orbita.client.nova.gfx.WCtx> before, java.util.function.Consumer<dev.orbita.client.nova.gfx.WCtx> after) {
        net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents.END_EXTRACTION.register(c -> { VIEW.set(c.viewMatrix()); PROJ.set(c.cullProjectionMatrix()); });
        net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents.BEFORE_ENTITIES.register(c -> before.accept(ctx(c)));
        net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents.AFTER_ENTITIES.register(c -> after.accept(ctx(c)));
    }

    private static dev.orbita.client.nova.gfx.WCtx ctx(net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext c) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        return new dev.orbita.client.nova.gfx.WCtx(mc.gameRenderer.getMainCamera(), mc.getDeltaTracker().getGameTimeDeltaPartialTick(false), c.matrices(), c.consumers(),
                new org.joml.Matrix4f(VIEW), new org.joml.Matrix4f(PROJ));
    }

    /** 1.21.11: el filtrado lo decide cada capa al dibujar (ya no se guarda en la textura). */
    public static void filter(net.minecraft.client.renderer.texture.AbstractTexture t, boolean blur, boolean mipmap) { }

    /** Categoria de las teclas de Nova en Controles (1.21.9+: es un objeto, no un texto). */
    public static final net.minecraft.client.KeyMapping.Category KEY_CATEGORY = net.minecraft.client.KeyMapping.Category.register(Identifier.fromNamespaceAndPath("nova", "main"));
}
