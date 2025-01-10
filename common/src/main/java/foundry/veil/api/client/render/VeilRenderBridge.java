package foundry.veil.api.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.rendertype.VeilRenderTypeBuilder;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import foundry.veil.impl.client.render.pipeline.AdvancedFboShard;
import foundry.veil.impl.client.render.pipeline.PatchStateShard;
import foundry.veil.impl.client.render.pipeline.ShaderProgramShard;
import foundry.veil.impl.client.render.wrapper.VanillaAdvancedFboWrapper;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

/**
 * Bridges between Minecraft and Veil render classes.
 *
 * @author Ocelot
 */
public interface VeilRenderBridge {

    /**
     * Creates a cull frustum helper from the specified vanilla frustum.
     *
     * @param frustum The frustum to use for the cull frustum
     * @return The cull frustum
     */
    static CullFrustum create(Frustum frustum) {
        return (CullFrustum) frustum;
    }

    /**
     * Creates a render type builder helper from the specified vanilla composite state builder.
     *
     * @param builder The state builder to wrap
     * @return The render type builder
     */
    static VeilRenderTypeBuilder create(RenderType.CompositeState.CompositeStateBuilder builder) {
        return (VeilRenderTypeBuilder) builder;
    }

    /**
     * Creates a matrix stack wrapper for the specified post stack.
     *
     * @param poseStack The pose stack to wrap
     * @return The matrix stack representation
     */
    static MatrixStack create(PoseStack poseStack) {
        return (MatrixStack) poseStack;
    }

    /**
     * Wraps the specified render target in a new advanced fbo.
     *
     * @param renderTarget The render target instance
     * @return A new advanced fbo that wraps the target in the api
     */
    static AdvancedFbo wrap(RenderTarget renderTarget) {
        return VeilRenderBridge.wrap(() -> renderTarget);
    }

    /**
     * Wraps the specified render target in a new advanced fbo.
     *
     * @param renderTargetSupplier The supplier to the render target instance
     * @return A new advanced fbo that wraps the target in the api
     */
    static AdvancedFbo wrap(Supplier<RenderTarget> renderTargetSupplier) {
        return new VanillaAdvancedFboWrapper(renderTargetSupplier);
    }

    /**
     * Creates a new shader state that points to the specified Veil shader name.
     *
     * @param shader The name of the shader to point to.
     * @return A new shader state shard for that shader
     */
    static RenderStateShard.ShaderStateShard shaderState(ResourceLocation shader) {
        return new ShaderProgramShard(shader);
    }

    /**
     * Creates a new output state that draws into the specified Veil framebuffer.
     *
     * @param framebuffer The framebuffer to use
     * @return A new shader state shard for that shader
     */
    static RenderStateShard.OutputStateShard outputState(ResourceLocation framebuffer) {
        return new AdvancedFboShard(framebuffer, () -> VeilRenderSystem.renderer().getFramebufferManager().getFramebuffer(framebuffer));
    }

    /**
     * Creates a new output state that draws into the specified Veil framebuffer.
     *
     * @param framebuffer The framebuffer to use
     * @return A new shader state shard for that shader
     */
    static RenderStateShard.OutputStateShard outputState(AdvancedFbo framebuffer) {
        return new AdvancedFboShard(null, () -> framebuffer);
    }

    /**
     * Creates a new output state that draws into the specified Veil framebuffer.
     *
     * @param framebuffer A supplier to the framebuffer to use
     * @return A new shader state shard for that shader
     */
    static RenderStateShard.OutputStateShard outputState(Supplier<AdvancedFbo> framebuffer) {
        return new AdvancedFboShard(null, framebuffer);
    }

    /**
     * Creates a new render state shard for tesselation patch size.
     *
     * @param patchVertices The number of vertices per patch
     * @return A new patch state
     */
    static RenderStateShard patchState(int patchVertices) {
        return new PatchStateShard(patchVertices);
    }
}
