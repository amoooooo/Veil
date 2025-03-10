package foundry.veil.impl.client.render.light;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import foundry.veil.Veil;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.light.PointLight;
import foundry.veil.api.client.render.light.renderer.IndirectLightRenderer;
import foundry.veil.api.client.render.light.renderer.LightRenderer;
import foundry.veil.api.client.render.light.renderer.LightTypeRenderer;
import foundry.veil.api.client.render.vertex.VertexArrayBuilder;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@ApiStatus.Internal
public class IndirectPointLightRenderer extends IndirectLightRenderer<PointLight> {

    private static final ResourceLocation SHADER = Veil.veilPath("light/point");

    public IndirectPointLightRenderer() {
        super(Float.BYTES * 7, 4, 0, 6);
    }

    @Override
    protected MeshData createMesh() {
        Tesselator tesselator = RenderSystem.renderThreadTesselator();
        BufferBuilder bufferBuilder = tesselator.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION);

        // High-res mesh
        LightTypeRenderer.createInvertedCube(bufferBuilder);

        // Low-res mesh
        float sqrt2 = (float) Math.sqrt(2.0);
        bufferBuilder.addVertex(-sqrt2, -sqrt2, 0);
        bufferBuilder.addVertex(sqrt2, -sqrt2, 0);
        bufferBuilder.addVertex(-sqrt2, sqrt2, 0);
        bufferBuilder.addVertex(sqrt2, sqrt2, 0);

        return bufferBuilder.buildOrThrow();
    }

    @Override
    protected void setupBufferState(VertexArrayBuilder builder) {
        builder.setVertexAttribute(1, 2, 3, VertexArrayBuilder.DataType.FLOAT, false, 0);
        builder.setVertexAttribute(2, 2, 3, VertexArrayBuilder.DataType.FLOAT, false, Float.BYTES * 3);
        builder.setVertexAttribute(3, 2, 1, VertexArrayBuilder.DataType.FLOAT, false, Float.BYTES * 6);
    }

    @Override
    protected void setupRenderState(@NotNull LightRenderer lightRenderer, @NotNull List<PointLight> lights) {
        VeilRenderSystem.setShader(SHADER);
    }

    @Override
    protected void clearRenderState(@NotNull LightRenderer lightRenderer, @NotNull List<PointLight> lights) {
    }
}
