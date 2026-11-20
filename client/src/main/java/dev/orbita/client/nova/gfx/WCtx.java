package dev.orbita.client.nova.gfx;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;

/**
 * Lo que necesita Nova para dibujar en el mundo. Fabric cambia su propio "contexto" entre versiones;
 * cada version lo convierte a este en compat/V.registerWorld, y el resto del codigo no cambia.
 */
public record WCtx(Camera camera, float tickDelta, PoseStack matrixStack, MultiBufferSource consumers,
                   Matrix4f positionMatrix, Matrix4f projectionMatrix) { }
