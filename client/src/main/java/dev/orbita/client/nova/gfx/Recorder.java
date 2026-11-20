package dev.orbita.client.nova.gfx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;

/**
 * 1.21.9+: los jugadores se dibujan mas tarde (cola de comandos). Los cosmeticos escriben aqui sus vertices
 * (ya transformados) y despues se entregan a la cola, una tanda por capa.
 */
public final class Recorder implements MultiBufferSource {
    private final Map<RenderType, Rec> layers = new LinkedHashMap<>();

    @Override public VertexConsumer getBuffer(RenderType layer) { return layers.computeIfAbsent(layer, l -> new Rec()); }

    public void submit(SubmitNodeCollector queue) {
        for (var e : layers.entrySet()) {
            Rec r = e.getValue();
            if (r.v.isEmpty()) continue;
            queue.submitCustomGeometry(new PoseStack(), e.getKey(), (entry, vc) -> r.replay(vc));
        }
    }

    /** Un vertice grabado (todo lo que puede pedir una capa). */
    private static final class Vtx { float x, y, z, u, v, nx, ny, nz = 1; int color = -1, ou, ov = 10, lu, lv; }

    private static final class Rec implements VertexConsumer {
        final List<Vtx> v = new ArrayList<>();
        Vtx cur;
        @Override public VertexConsumer addVertex(float x, float y, float z) { cur = new Vtx(); cur.x = x; cur.y = y; cur.z = z; v.add(cur); return this; }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) { cur.color = (a << 24) | (r << 16) | (g << 8) | b; return this; }
        @Override public VertexConsumer setColor(int argb) { cur.color = argb; return this; }
        @Override public VertexConsumer setUv(float u, float vv) { cur.u = u; cur.v = vv; return this; }
        @Override public VertexConsumer setUv1(int u, int vv) { cur.ou = u; cur.ov = vv; return this; }
        @Override public VertexConsumer setUv2(int u, int vv) { cur.lu = u; cur.lv = vv; return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { cur.nx = x; cur.ny = y; cur.nz = z; return this; }
        @Override public VertexConsumer setLineWidth(float w) { return this; }

        void replay(VertexConsumer out) {
            for (Vtx p : v) out.addVertex(p.x, p.y, p.z).setColor(p.color).setUv(p.u, p.v).setUv1(p.ou, p.ov).setUv2(p.lu, p.lv).setNormal(p.nx, p.ny, p.nz);
        }
    }
}
