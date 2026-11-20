package dev.orbita.client.nova.module.cosmetic;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.orbita.client.nova.gfx.Tex;
import dev.orbita.client.nova.module.Category;
import dev.orbita.client.nova.module.Module;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/**
 * Cosmetico que se dibuja como parte del jugador (igual que la armadura): sigue la cabeza y el cuerpo, se agacha
 * contigo, recibe la luz del mundo y tambien sale en la vista previa del menu y en el inventario.
 */
public abstract class ModelCosmetic extends Module {
    protected ModelCosmetic(String name, String desc) { super(name, desc, Category.COSMETICS); }

    /** Todo lo que necesita un cosmetico para dibujarse. Pide cada capa justo antes de usarla. */
    public record Ctx(PoseStack ms, MultiBufferSource vcp, int light, AvatarRenderState state,
                      PlayerModel model, LocalPlayer player, float time) {
        /** Solido con luz y sombras. */
        public VertexConsumer lit() { Tex.ensure(); return vcp.getBuffer(net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucent(Tex.WHITE)); }
        /** Con textura propia (por ejemplo la tela del pañuelo). */
        public VertexConsumer textured(net.minecraft.resources.Identifier id) { Tex.ensure(); return vcp.getBuffer(net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucent(id)); }
        /** Brillo que suma luz (resplandor). */
        public VertexConsumer glow() { Tex.ensure(); return vcp.getBuffer(net.minecraft.client.renderer.rendertype.RenderTypes.eyes(Tex.WHITE)); }
        /** Siempre a plena luz (gemas, runas...). */
        public VertexConsumer emissive() { Tex.ensure(); return vcp.getBuffer(net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucentEmissive(Tex.WHITE)); }
        public int fullLight() { return LightTexture.FULL_BRIGHT; }
        /** El jugador esta en el aire (saltando, cayendo o volando). */
        public boolean airborne() { return player != null && !player.onGround(); }
        public float speed() { return player == null ? 0 : (float) player.getDeltaMovement().horizontalDistance(); }
    }

    public abstract void render(Ctx c);
}
