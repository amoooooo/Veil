package foundry.veil.impl.client.render.light;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import foundry.veil.Veil;
import foundry.veil.api.client.render.CullFrustum;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.light.DirectionalLight;
import foundry.veil.api.client.render.light.renderer.LightRenderer;
import foundry.veil.api.client.render.light.renderer.LightTypeRenderer;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import foundry.veil.api.client.render.vertex.VertexArray;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.List;
import java.util.Objects;
import java.util.Set;

@ApiStatus.Internal
public class DirectionalLightRenderer implements LightTypeRenderer<DirectionalLight> {

    private static final Vector3f DIRECTION = new Vector3f();
    private static final ResourceLocation SHADER = Veil.veilPath("light/directional");

    private final VertexArray vertexArray;
    private int visibleLights;

    public DirectionalLightRenderer() {
        this.vertexArray = VertexArray.create();
        this.vertexArray.upload(createMesh(), VertexArray.DrawUsage.STATIC);
        VertexArray.unbind();
    }

    private static MeshData createMesh() {
        Tesselator tesselator = RenderSystem.renderThreadTesselator();
        BufferBuilder bufferBuilder = tesselator.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION);
        LightTypeRenderer.createQuad(bufferBuilder);
        return bufferBuilder.buildOrThrow();
    }

    @Override
    public void prepareLights(LightRenderer lightRenderer, List<DirectionalLight> lights, Set<DirectionalLight> removedLights, CullFrustum frustum) {
        this.visibleLights = lights.size();
    }

    @Override
    public void renderLights(LightRenderer lightRenderer, List<DirectionalLight> lights) {
        VeilRenderSystem.setShader(SHADER);
        if (lightRenderer.applyShader()) {
            return;
        }

        ShaderProgram shader = Objects.requireNonNull(VeilRenderSystem.getShader());
        this.vertexArray.bind();
        for (DirectionalLight light : lights) {
            Vector3fc lightColor = light.getColor();
            float brightness = light.getBrightness();
            shader.setVector("LightColor", lightColor.x() * brightness, lightColor.y() * brightness, lightColor.z() * brightness);
            shader.setVector("LightDirection", DIRECTION.set(light.getDirection()).normalize());
            this.vertexArray.draw();
        }

        VertexArray.unbind();
    }

    @Override
    public int getVisibleLights() {
        return this.visibleLights;
    }

    @Override
    public void free() {
        this.vertexArray.close();
    }
}
