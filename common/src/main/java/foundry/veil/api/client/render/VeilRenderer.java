package foundry.veil.api.client.render;

import foundry.veil.api.client.editor.EditorManager;
import foundry.veil.api.client.render.deferred.VeilDeferredRenderer;
import foundry.veil.api.client.render.framebuffer.FramebufferManager;
import foundry.veil.api.client.render.post.PostPipeline;
import foundry.veil.api.client.render.post.PostProcessingManager;
import foundry.veil.api.client.render.rendertype.DynamicRenderTypeManager;
import foundry.veil.api.client.render.shader.ShaderManager;
import foundry.veil.api.client.render.shader.ShaderModificationManager;
import foundry.veil.api.client.render.shader.definition.ShaderPreDefinitions;
import foundry.veil.api.quasar.particle.ParticleSystemManager;
import foundry.veil.ext.LevelRendererExtension;
import foundry.veil.impl.client.imgui.VeilImGuiImpl;
import foundry.veil.mixin.accessor.ReloadableResourceManagerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import org.jetbrains.annotations.ApiStatus;
import org.lwjgl.system.NativeResource;

import java.util.List;

/**
 * Manages the render pipeline for Veil.
 *
 * @author Ocelot
 */
public class VeilRenderer implements NativeResource {

    private final ShaderModificationManager shaderModificationManager;
    private final ShaderPreDefinitions shaderPreDefinitions;
    private final ShaderManager shaderManager;
    private final FramebufferManager framebufferManager;
    private final PostProcessingManager postProcessingManager;
    private final VeilDeferredRenderer deferredRenderer;
    private final DynamicRenderTypeManager dynamicRenderTypeManager;
    private final ParticleSystemManager quasarParticleManager;
    private final EditorManager editorManager;
    private final CameraMatrices cameraMatrices;
    private final GuiInfo guiInfo;

    @ApiStatus.Internal
    public VeilRenderer(ReloadableResourceManager resourceManager) {
        this.shaderModificationManager = new ShaderModificationManager();
        this.shaderPreDefinitions = new ShaderPreDefinitions();
        this.shaderManager = new ShaderManager(ShaderManager.PROGRAM_SET, this.shaderPreDefinitions);
        this.framebufferManager = new FramebufferManager();
        this.postProcessingManager = new PostProcessingManager();
        ShaderManager deferredShaderManager = new ShaderManager(ShaderManager.DEFERRED_SET, this.shaderPreDefinitions);
        this.deferredRenderer = new VeilDeferredRenderer(deferredShaderManager, this.shaderPreDefinitions, this.framebufferManager, this.postProcessingManager);
        this.dynamicRenderTypeManager = new DynamicRenderTypeManager();
        this.quasarParticleManager = new ParticleSystemManager();
        this.editorManager = new EditorManager(resourceManager);
        this.cameraMatrices = new CameraMatrices();
        this.guiInfo = new GuiInfo();

        List<PreparableReloadListener> listeners = ((ReloadableResourceManagerAccessor) resourceManager).getListeners();

        // This must finish loading before the game renderer so modifications can apply on load
        listeners.add(0, this.shaderModificationManager);
        // This must be before vanilla shaders so vanilla shaders can be replaced
        listeners.add(1, this.shaderManager);
        resourceManager.registerReloadListener(this.framebufferManager);
        resourceManager.registerReloadListener(this.postProcessingManager);
        resourceManager.registerReloadListener(this.deferredRenderer);
        resourceManager.registerReloadListener(this.dynamicRenderTypeManager);
    }

    /**
     * @return The manager for all custom shader modifications
     */
    public ShaderModificationManager getShaderModificationManager() {
        return this.shaderModificationManager;
    }

    /**
     * @return The set of shader pre-definitions. Changes are automatically synced the next frame
     */
    public ShaderPreDefinitions getShaderDefinitions() {
        return this.shaderPreDefinitions;
    }

    /**
     * @return The manager for all veil shaders
     */
    public ShaderManager getShaderManager() {
        return this.shaderManager;
    }

    /**
     * @return The manager for all custom veil framebuffers
     */
    public FramebufferManager getFramebufferManager() {
        return this.framebufferManager;
    }

    /**
     * @return The manager for all {@link PostPipeline} instances
     */
    public PostProcessingManager getPostProcessingManager() {
        return this.postProcessingManager;
    }

    /**
     * @return The deferred renderer instance
     */
    public VeilDeferredRenderer getDeferredRenderer() {
        return this.deferredRenderer;
    }

    /**
     * @return The manager for all data-driven render types
     */
    public DynamicRenderTypeManager getDynamicRenderTypeManager() {
        return this.dynamicRenderTypeManager;
    }

    /**
     * @return The manager for all quasar particles
     */
    public ParticleSystemManager getParticleManager() {
        return this.quasarParticleManager;
    }

    /**
     * @return The manager for all editors
     */
    public EditorManager getEditorManager() {
        return this.editorManager;
    }

    /**
     * @return The camera matrices instance
     */
    public CameraMatrices getCameraMatrices() {
        return this.cameraMatrices;
    }

    /**
     * @return The gui info instance
     */
    public GuiInfo getGuiInfo() {
        return this.guiInfo;
    }

    /**
     * @return Whether ImGui can be used
     */
    public static boolean hasImGui() {
        return VeilImGuiImpl.get() instanceof VeilImGuiImpl;
    }

    /**
     * @return The culling frustum for the renderer
     */
    public static CullFrustum getCullingFrustum() {
        return ((LevelRendererExtension) Minecraft.getInstance().levelRenderer).veil$getCullFrustum();
    }

    @Override
    public void free() {
        this.shaderManager.close();
        this.framebufferManager.free();
        this.postProcessingManager.free();
        this.deferredRenderer.free();
        this.quasarParticleManager.clear();
        this.cameraMatrices.free();
        this.guiInfo.free();
    }
}
