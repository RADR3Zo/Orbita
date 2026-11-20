package dev.orbita.client.nova.module.cosmetic;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.orbita.client.nova.NovaClient;
import dev.orbita.client.nova.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/** Capa extra del jugador local que dibuja los cosmeticos encendidos. Los demas jugadores no se ven afectados. */
public final class CosmeticFeature extends RenderLayer<AvatarRenderState, PlayerModel> {
    public CosmeticFeature(RenderLayerParent<AvatarRenderState, PlayerModel> context) { super(context); }

    @Override
    public void submit(PoseStack ms, SubmitNodeCollector queue, int light, AvatarRenderState state, float limbAngle, float limbDistance) {
        var mc = Minecraft.getInstance();
        if (NovaClient.modules == null || mc.player == null || state.id != mc.player.getId() || state.isInvisible) return;
        if (dev.orbita.client.nova.module.ClientSettings.extra()) return; // ahorro Extra: sin cosmeticos
        dev.orbita.client.nova.gfx.Recorder vcp = new dev.orbita.client.nova.gfx.Recorder();
        ModelCosmetic.Ctx c = new ModelCosmetic.Ctx(ms, vcp, light, state, getParentModel(), mc.player, state.ageInTicks);
        for (Module m : NovaClient.modules.all) {
            if (m instanceof ModelCosmetic mc2 && m.enabled) {
                ms.pushPose();
                try { mc2.render(c); } catch (RuntimeException e) { m.enabled = false; NovaClient.LOG.error("Cosmetico " + m.name + " fallo y se apago", e); }
                ms.popPose();
            }
        }
        vcp.submit(queue);
    }
}
