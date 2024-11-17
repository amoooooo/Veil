package foundry.veil.impl.client.render.pipeline;

import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.framebuffer.VeilFramebuffers;
import foundry.veil.api.client.render.post.PostPipeline;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation of {@link PostPipeline.Context}.
 */
@ApiStatus.Internal
public class PostPipelineContext implements PostPipeline.Context {

    private final Map<CharSequence, Integer> samplers;
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
        this.setFramebuffer(VeilFramebuffers.POST, this.getDrawFramebuffer());
    }

    /**
     * Ends the running pass and cleans up resources.
     */
    public void end() {
        this.samplers.clear();
        this.framebuffers.clear();
    }

    @Override
    public void setSampler(CharSequence name, int id) {
        this.samplers.put(name, id);
    }

    @Override
    public void setFramebuffer(ResourceLocation name, AdvancedFbo framebuffer) {
        this.framebuffers.put(name, framebuffer);
    }

    @Override
    public void applySamplers(ShaderProgram shader) {
        this.samplers.forEach(shader::addSampler);
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
