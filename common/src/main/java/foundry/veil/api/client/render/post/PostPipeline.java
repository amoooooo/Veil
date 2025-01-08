package foundry.veil.api.client.render.post;

import com.mojang.serialization.Codec;
import foundry.veil.api.client.registry.PostPipelineStageRegistry;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.shader.program.MutableUniformAccess;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import foundry.veil.api.client.render.shader.program.TextureUniformAccess;
import foundry.veil.api.client.render.shader.program.UniformAccess;
import foundry.veil.api.client.render.shader.texture.ShaderTextureSource;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.joml.*;
import org.lwjgl.system.NativeResource;

/**
 * <p>A series of post-processing effects that can be run to change the current framebuffer state.</p>
 * <p>It can be fully run using {@link PostProcessingManager#runPipeline(PostPipeline)}.</p>
 * <p>This class implements {@link UniformAccess} to allow changing uniforms in all shaders.</p>
 *
 * @author Ocelot
 */
public interface PostPipeline extends MutableUniformAccess, NativeResource {

    Codec<PostPipeline> CODEC = PostPipelineStageRegistry.REGISTRY.byNameCodec().dispatch(PostPipeline::getType, PostPipelineStageRegistry.PipelineType::codec);

    /**
     * Applies this post effect.
     * {@link PostProcessingManager#runPipeline(PostPipeline)} should be called to run this pipeline.
     *
     * @param context The context to use when running this pipeline.
     */
    @ApiStatus.OverrideOnly
    void apply(Context context);

    /**
     * Allows a post pipeline to dispose of any resources it takes up.
     */
    @Override
    default void free() {
    }

    /**
     * @return The type of post effect this is
     */
    PostPipelineStageRegistry.PipelineType<? extends PostPipeline> getType();

    @Override
    default int getUniform(CharSequence name) {
        throw new UnsupportedOperationException("Cannot get values from post pipeline");
    }

    @Override
    default int getUniformBlock(CharSequence name) {
        throw new UnsupportedOperationException("Cannot get values from post pipeline");
    }

    @Override
    default int getStorageBlock(CharSequence name) {
        throw new UnsupportedOperationException("Cannot get values from post pipeline");
    }

    @Override
    default boolean hasUniform(CharSequence name) {
        return false;
    }

    @Override
    default boolean hasUniformBlock(CharSequence name) {
        return false;
    }

    @Override
    default boolean hasStorageBlock(CharSequence name) {
        return false;
    }

    @Override
    default float getFloat(CharSequence name) {
        throw new UnsupportedOperationException("Cannot get values from post pipeline");
    }

    @Override
    default int getInt(CharSequence name) {
        throw new UnsupportedOperationException("Cannot get values from post pipeline");
    }

    @Override
    default void getFloats(CharSequence name, float[] values) {
        throw new UnsupportedOperationException("Cannot get values from post pipeline");
    }

    @Override
    default void getVector(CharSequence name, Vector2f... values) {
        throw new UnsupportedOperationException("Cannot get values from post pipeline");
    }

    @Override
    default void getVector(CharSequence name, Vector3f... values) {
        throw new UnsupportedOperationException("Cannot get values from post pipeline");
    }

    @Override
    default void getVector(CharSequence name, Vector4f... values) {
        throw new UnsupportedOperationException("Cannot get values from post pipeline");
    }

    @Override
    default void getInts(CharSequence name, int[] values) {
        throw new UnsupportedOperationException("Cannot get values from post pipeline");
    }

    @Override
    default void getVector(CharSequence name, Vector2i... values) {
        throw new UnsupportedOperationException("Cannot get values from post pipeline");
    }

    @Override
    default void getVector(CharSequence name, Vector3i... values) {
        throw new UnsupportedOperationException("Cannot get values from post pipeline");
    }

    @Override
    default void getVector(CharSequence name, Vector4i... values) {
        throw new UnsupportedOperationException("Cannot get values from post pipeline");
    }

    @Override
    default void getMatrix(CharSequence name, Matrix2f value) {
        throw new UnsupportedOperationException("Cannot get values from post pipeline");
    }

    @Override
    default void getMatrix(CharSequence name, Matrix3f value) {
        throw new UnsupportedOperationException("Cannot get values from post pipeline");
    }

    @Override
    default void getMatrix(CharSequence name, Matrix3x2f value) {
        throw new UnsupportedOperationException("Cannot get values from post pipeline");
    }

    @Override
    default void getMatrix(CharSequence name, Matrix4f value) {
        throw new UnsupportedOperationException("Cannot get values from post pipeline");
    }

    @Override
    default void getMatrix(CharSequence name, Matrix4x3f value) {
        throw new UnsupportedOperationException("Cannot get values from post pipeline");
    }

    @Override
    default void setUniformBlock(CharSequence name, int binding) {
    }

    @Override
    default void setStorageBlock(CharSequence name, int binding) {
    }

    @Override
    default void setFloat(CharSequence name, float value) {
    }

    @Override
    default void setVector(CharSequence name, float x, float y) {
    }

    @Override
    default void setVector(CharSequence name, float x, float y, float z) {
    }

    @Override
    default void setVector(CharSequence name, float x, float y, float z, float w) {
    }

    @Override
    default void setInt(CharSequence name, int value) {
    }

    @Override
    default void setVectorI(CharSequence name, int x, int y) {
    }

    @Override
    default void setVectorI(CharSequence name, int x, int y, int z) {
    }

    @Override
    default void setVectorI(CharSequence name, int x, int y, int z, int w) {
    }

    @Override
    default void setFloats(CharSequence name, float... values) {
    }

    @Override
    default void setVectors(CharSequence name, Vector2fc... values) {
    }

    @Override
    default void setVectors(CharSequence name, Vector3fc... values) {
    }

    @Override
    default void setVectors(CharSequence name, Vector4fc... values) {
    }

    @Override
    default void setInts(CharSequence name, int... values) {
    }

    @Override
    default void setVectors(CharSequence name, Vector2ic... values) {
    }

    @Override
    default void setVectors(CharSequence name, Vector3ic... values) {
    }

    @Override
    default void setVectors(CharSequence name, Vector4ic... values) {
    }

    @Override
    default void setMatrix(CharSequence name, Matrix2fc value, boolean transpose) {
    }

    @Override
    default void setMatrix(CharSequence name, Matrix3fc value, boolean transpose) {
    }

    @Override
    default void setMatrix(CharSequence name, Matrix3x2fc value, boolean transpose) {
    }

    @Override
    default void setMatrix(CharSequence name, Matrix4fc value, boolean transpose) {
    }

    @Override
    default void setMatrix(CharSequence name, Matrix4x3fc value, boolean transpose) {
    }

    /**
     * Context for applying post pipelines.
     *
     * @author Ocelot
     */
    interface Context extends ShaderTextureSource.Context {

        /**
         * Binds a named sampler id. All samplers can be applied with {@link #applySamplers(TextureUniformAccess)} for adding them to shaders.
         *
         * @param name The name of the sampler
         * @param id   The id of the texture to bind
         */
        void setSampler(CharSequence name, int id);

        /**
         * Sets a framebuffer to a name. This allows post stages to create new framebuffers that can be accessed later on.
         *
         * @param name        The name of the framebuffer
         * @param framebuffer The framebuffer to set
         */
        void setFramebuffer(ResourceLocation name, AdvancedFbo framebuffer);

        /**
         * Applies each sampler to the specified shader.
         *
         * @param shader The shader to apply the samplers to
         */
        void applySamplers(TextureUniformAccess shader);

        /**
         * Removes all post-processing samplers from the specified shader.
         *
         * @param shader The shader to apply the samplers to
         */
        void clearSamplers(TextureUniformAccess shader);

        /**
         * Retrieves a framebuffer by id or the main framebuffer if it doesn't exist.
         *
         * @param name The name of the framebuffer to retrieve
         * @return The framebuffer with that id or the main framebuffer
         */
        default AdvancedFbo getFramebufferOrDraw(ResourceLocation name) {
            AdvancedFbo fbo = this.getFramebuffer(name);
            return fbo != null ? fbo : this.getDrawFramebuffer();
        }

        /**
         * @return The main framebuffer to draw into. This is later copied onto the main framebuffer
         */
        AdvancedFbo getDrawFramebuffer();

        /**
         * Retrieves a post pipeline by name.
         *
         * @param name The name of the pipeline to get
         * @return The registered pipeline or <code>null</code> if it couldn't be found
         */
        default @Nullable PostPipeline getPipeline(ResourceLocation name) {
            return VeilRenderSystem.renderer().getPostProcessingManager().getPipeline(name);
        }

        /**
         * Retrieves a shader by name.
         *
         * @param name The name of the shader to get
         * @return The registered shader or <code>null</code> if it couldn't be found
         */
        default @Nullable ShaderProgram getShader(ResourceLocation name) {
            return VeilRenderSystem.renderer().getShaderManager().getShader(name);
        }
    }
}
