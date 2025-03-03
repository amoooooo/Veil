package foundry.veil.impl.client.render.pipeline;

import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.framebuffer.VeilFramebuffers;
import foundry.veil.api.client.render.post.PostPipeline;
import foundry.veil.api.client.render.shader.program.TextureUniformAccess;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation of {@link PostPipeline.Context}.
 */
@ApiStatus.Internal
public class PostPipelineContext implements PostPipeline.Context {

    private final Map<CharSequence, Pair<Integer, Integer>> samplers;
    private final Map<ResourceLocation, AdvancedFbo> framebuffers;

    /**
     * Creates a new context to fit the specified window.
     */
    public PostPipelineContext() {
        this.samplers = new HashMap<>();
        this.framebuffers = new HashMap<>();
    }

    /**
     * Marks the start of a new post run.
     */
    public void begin() {
        VeilRenderSystem.renderer().getFramebufferManager().getFramebuffers().forEach(this::setFramebuffer);
    }

    /**
     * Ends the running pass and cleans up resources.
     */
    public void end() {
        this.samplers.clear();
        this.framebuffers.clear();
    }

    @Override
    public void setSampler(CharSequence name, int textureId, int samplerId) {
        this.samplers.put(name, Pair.of(textureId, samplerId));
    }

    @Override
    public void setFramebuffer(ResourceLocation name, AdvancedFbo framebuffer) {
        this.framebuffers.put(name, framebuffer);
    }

    @Override
    public void applySamplers(TextureUniformAccess shader) {
        this.samplers.forEach((name, pair) -> shader.setSampler(name, pair.getLeft(), pair.getRight()));
    }

    @Override
    public void clearSamplers(TextureUniformAccess shader) {
        this.samplers.keySet().forEach(shader::removeSampler);
    }

    @Override
    public @Nullable AdvancedFbo getFramebuffer(ResourceLocation name) {
        return this.framebuffers.get(name);
    }

    @Override
    public AdvancedFbo getDrawFramebuffer() {
        return this.framebuffers.getOrDefault(VeilFramebuffers.POST, AdvancedFbo.getMainFramebuffer());
    }
}
