package foundry.veil.impl.client.render.light;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import foundry.veil.api.client.render.CullFrustum;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.light.AreaLight;
import foundry.veil.api.client.render.light.renderer.InstancedLightRenderer;
import foundry.veil.api.client.render.light.renderer.LightRenderer;
import foundry.veil.api.client.render.light.renderer.LightTypeRenderer;
import foundry.veil.api.client.render.vertex.VertexArrayBuilder;
import foundry.veil.api.client.render.shader.VeilShaders;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2f;
import org.joml.Vector3d;

import java.util.List;

@ApiStatus.Internal
public class AreaLightRenderer extends InstancedLightRenderer<AreaLight> {

    public AreaLightRenderer() {
        super(Float.BYTES * 22 + 2);
    }

    @Override
    protected MeshData createMesh() {
        BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION);
        LightTypeRenderer.createInvertedCube(builder);
        return builder.buildOrThrow();
    }

    @Override
    protected void setupBufferState(VertexArrayBuilder builder) {
        builder.setVertexAttribute(1, 2, 4, VertexArrayBuilder.DataType.FLOAT, false, 0);
        builder.setVertexAttribute(2, 2, 4, VertexArrayBuilder.DataType.FLOAT, false, Float.BYTES * 4);
        builder.setVertexAttribute(3, 2, 4, VertexArrayBuilder.DataType.FLOAT, false, Float.BYTES * 8);
        builder.setVertexAttribute(4, 2, 4, VertexArrayBuilder.DataType.FLOAT, false, Float.BYTES * 12); // matrix !
        builder.setVertexAttribute(5, 2, 3, VertexArrayBuilder.DataType.FLOAT, false, Float.BYTES * 16); // color
        builder.setVertexAttribute(6, 2, 2, VertexArrayBuilder.DataType.FLOAT, false, Float.BYTES * 19); // size
        builder.setVertexAttribute(7, 2, 1, VertexArrayBuilder.DataType.UNSIGNED_SHORT, true, Float.BYTES * 21); // angle
        builder.setVertexAttribute(8, 2, 1, VertexArrayBuilder.DataType.FLOAT, false, Float.BYTES * 21 + 2); // distance
    }

    @Override
    protected void setupRenderState(@NotNull LightRenderer lightRenderer, @NotNull List<AreaLight> lights) {
        VeilRenderSystem.setShader(VeilShaders.LIGHT_AREA);
    }

    @Override
    protected void clearRenderState(@NotNull LightRenderer lightRenderer, @NotNull List<AreaLight> lights) {
    }

    // the bounding box here isn't particularly tight, but it should always encapsulate the light's area.
    @Override
    protected boolean isVisible(AreaLight light, CullFrustum frustum) {
        Vector2f size = light.getSize();
        Vector3d position = light.getPosition();
        float radius = Math.max(size.x, size.y) + light.getDistance();
        return frustum.testAab(
                position.x - radius,
                position.y - radius,
                position.z - radius,
                position.x + radius,
                position.y + radius,
                position.z + radius);
    }
}
