package dev.orbita.client.nova.module.cosmetic;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.orbita.client.nova.gfx.Gfx;
import dev.orbita.client.nova.module.Category;
import dev.orbita.client.nova.module.Module;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Base de los cosmeticos: coloca el sistema de coordenadas en el jugador local (+Z = hacia donde mira). */
abstract class Cosmetic extends Module {
    Cosmetic(String name, String desc) { super(name, desc, Category.COSMETICS); }

    /** Se llama con la matriz ya movida al jugador. */
    abstract void draw(PoseStack ms, Player p, float time, float delta, dev.orbita.client.nova.gfx.WCtx ctx);

    boolean showInFirstPerson() { return false; }
    boolean rotateWithBody() { return true; }

    @Override
    public void onWorldAfter(dev.orbita.client.nova.gfx.WCtx ctx) {
        var mc = mc();
        Player p = mc.player;
        if (p == null || mc.level == null) return;
        if (mc.options.getCameraType().isFirstPerson() && !showInFirstPerson()) return;

        float delta = ctx.tickDelta();
        Vec3 cam = ctx.camera().position();
        Vec3 pos = p.getPosition(delta);
        PoseStack ms = ctx.matrixStack() != null ? ctx.matrixStack() : new PoseStack(); // la vista ya va en el ModelView
        ms.pushPose();
        ms.translate(pos.x - cam.x, pos.y - cam.y, pos.z - cam.z);
        if (rotateWithBody()) {
            float yaw = Mth.rotLerp(delta, p.yBodyRotO, p.yBodyRot);
            ms.mulPose(Axis.YP.rotationDegrees(-yaw));
        }
        Gfx.begin();
        draw(ms, p, mc.level.getGameTime() + delta, delta, ctx);
        Gfx.end();
        ms.popPose();
    }
}
