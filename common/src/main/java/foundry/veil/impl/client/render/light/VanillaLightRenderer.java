package foundry.veil.impl.client.render.light;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.light.renderer.LightRenderer;
import foundry.veil.api.client.render.light.renderer.LightTypeRenderer;
import foundry.veil.api.client.render.shader.VeilShaders;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.ApiStatus;
import org.lwjgl.system.NativeResource;

@ApiStatus.Internal
public class VanillaLightRenderer implements NativeResource {

    private final VertexBuffer vbo;

    public VanillaLightRenderer() {
        this.vbo = new VertexBuffer(VertexBuffer.Usage.STATIC);
        this.vbo.bind();
        this.vbo.upload(createMesh());
        VertexBuffer.unbind();
    }

    private static MeshData createMesh() {
        Tesselator tesselator = RenderSystem.renderThreadTesselator();
        BufferBuilder bufferBuilder = tesselator.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION);
        LightTypeRenderer.createQuad(bufferBuilder);
        return bufferBuilder.buildOrThrow();
    }

    public void render(LightRenderer lightRenderer, ClientLevel level) {
        VeilRenderSystem.setShader(VeilShaders.LIGHT_VANILLA_LIGHTMAP);
        lightRenderer.applyShader();

        ShaderProgram shader = VeilRenderSystem.getShader();
        if (shader == null) {
            return;
        }

        for (Direction direction : Direction.values()) {
            shader.setFloat("LightShading" + direction.ordinal(), level.getShade(direction, true));
        }

        this.vbo.bind();
        this.vbo.draw();
        VertexBuffer.unbind();
    }

    @Override
    public void free() {
        this.vbo.close();
    }
}
