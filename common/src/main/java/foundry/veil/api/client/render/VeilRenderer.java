package foundry.veil.api.client.render;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import foundry.veil.Veil;
import foundry.veil.api.client.editor.EditorManager;
import foundry.veil.api.client.render.dynamicbuffer.DynamicBufferType;
import foundry.veil.api.client.render.framebuffer.FramebufferManager;
import foundry.veil.api.client.render.light.renderer.LightRenderer;
import foundry.veil.api.client.render.post.PostPipeline;
import foundry.veil.api.client.render.post.PostProcessingManager;
import foundry.veil.api.client.render.rendertype.DynamicRenderTypeManager;
import foundry.veil.api.client.render.shader.ShaderManager;
import foundry.veil.api.client.render.shader.ShaderModificationManager;
import foundry.veil.api.client.render.shader.definition.ShaderPreDefinitions;
import foundry.veil.api.quasar.particle.ParticleSystemManager;
import foundry.veil.ext.LevelRendererExtension;
import foundry.veil.impl.client.imgui.VeilImGuiImpl;
import foundry.veil.impl.client.render.dynamicbuffer.DynamicBufferManger;
import foundry.veil.impl.client.render.dynamicbuffer.VanillaShaderCompiler;
import foundry.veil.mixin.accessor.ReloadableResourceManagerAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import org.jetbrains.annotations.ApiStatus;
import org.lwjgl.system.NativeResource;

import java.util.List;
import java.util.function.Consumer;

/**
 * Manages the render pipeline for Veil.
 *
 * @author Ocelot
 */
public class VeilRenderer implements NativeResource {

    public static final ResourceLocation ALBEDO_BUFFER_TEXTURE = Veil.veilPath("dynamic_buffer/albedo");
    public static final ResourceLocation NORMAL_BUFFER_TEXTURE = Veil.veilPath("dynamic_buffer/normal");
    public static final ResourceLocation LIGHT_UV_BUFFER_TEXTURE = Veil.veilPath("dynamic_buffer/light_uv");
    public static final ResourceLocation LIGHT_COLOR_BUFFER_TEXTURE = Veil.veilPath("dynamic_buffer/light_color");
    public static final ResourceLocation DEBUG_BUFFER_TEXTURE = Veil.veilPath("dynamic_buffer/debug");

    public static final ResourceLocation LIGHT_POST = Veil.veilPath("core/light_post");
    public static final ResourceLocation COMPOSITE = Veil.veilPath("core/composite");

    private final VanillaShaderCompiler vanillaShaderCompiler;
    private final DynamicBufferManger dynamicBufferManger;
    private final ShaderModificationManager shaderModificationManager;
    private final ShaderPreDefinitions shaderPreDefinitions;
    private final ShaderManager shaderManager;
    private final FramebufferManager framebufferManager;
    private final PostProcessingManager postProcessingManager;
    private final DynamicRenderTypeManager dynamicRenderTypeManager;
    private final ParticleSystemManager quasarParticleManager;
    private final EditorManager editorManager;
    private final CameraMatrices cameraMatrices;
    private final LightRenderer lightRenderer;
    private final GuiInfo guiInfo;

    @ApiStatus.Internal
    public VeilRenderer(ReloadableResourceManager resourceManager, Window window) {
        this.vanillaShaderCompiler = new VanillaShaderCompiler();
        this.dynamicBufferManger = new DynamicBufferManger(window.getWidth(), window.getHeight());
        this.shaderPreDefinitions = new ShaderPreDefinitions();
        this.shaderModificationManager = new ShaderModificationManager(this.shaderPreDefinitions);
        this.shaderManager = new ShaderManager(ShaderManager.PROGRAM_SET, this.shaderPreDefinitions, this.dynamicBufferManger);
        this.framebufferManager = new FramebufferManager();
        this.postProcessingManager = new PostProcessingManager();
        this.dynamicRenderTypeManager = new DynamicRenderTypeManager();
        this.quasarParticleManager = new ParticleSystemManager();
        this.editorManager = new EditorManager(resourceManager);
        this.cameraMatrices = new CameraMatrices();
        this.lightRenderer = new LightRenderer();
        this.guiInfo = new GuiInfo();

        List<PreparableReloadListener> listeners = ((ReloadableResourceManagerAccessor) resourceManager).getListeners();

        // This must finish loading before the game renderer so modifications can apply on load
        listeners.add(0, this.shaderModificationManager);
        // This must be before vanilla shaders so vanilla shaders can be replaced
        listeners.add(1, this.shaderManager);
        resourceManager.registerReloadListener(this.framebufferManager);
        resourceManager.registerReloadListener(this.postProcessingManager);
        resourceManager.registerReloadListener(this.dynamicRenderTypeManager);
    }

    @ApiStatus.Internal
    public void addDebugInfo(Consumer<String> consumer) {
        boolean ambientOcclusion = this.lightRenderer.isAmbientOcclusionEnabled();
//        boolean vanillaLights = this.lightRenderer.isVanillaLightEnabled();
//        boolean vanillaEntityLights = this.shaderPreDefinitions.getDefinition(DISABLE_VANILLA_ENTITY_LIGHT_KEY) == null;
        consumer.accept("Ambient Occlusion: " + (ambientOcclusion ? ChatFormatting.GREEN + "On" : ChatFormatting.RED + "Off"));
//        consumer.accept("Vanilla Light: " + (vanillaLights ? ChatFormatting.GREEN + "On" : ChatFormatting.RED + "Off"));
//        consumer.accept("Vanilla Entity Light: " + (vanillaEntityLights ? ChatFormatting.GREEN + "On" : ChatFormatting.RED + "Off"));
        this.lightRenderer.addDebugInfo(consumer);
    }

    /**
     * Enables the specified dynamic render buffers.
     *
     * @param buffers The buffers to enable
     * @return Whether any change occurred
     */
    public boolean enableBuffers(DynamicBufferType... buffers) {
        RenderSystem.assertOnRenderThreadOrInit();
        if (buffers.length == 0) {
            return false;
        }

        int active = this.dynamicBufferManger.getActiveBuffers() | DynamicBufferType.encode(buffers);
        return this.dynamicBufferManger.setActiveBuffers(active);
    }

    /**
     * Disables the specified dynamic render buffers.
     *
     * @param buffers The buffers to disable
     * @return Whether any change occurred
     */
    public boolean disableBuffers(DynamicBufferType... buffers) {
        RenderSystem.assertOnRenderThreadOrInit();
        if (buffers.length == 0) {
            return false;
        }

        int active = this.dynamicBufferManger.getActiveBuffers() & ~DynamicBufferType.encode(buffers);
        return this.dynamicBufferManger.setActiveBuffers(active);
    }

    /**
     * @return The Veil compiler for vanilla shaders
     */
    public VanillaShaderCompiler getVanillaShaderCompiler() {
        return this.vanillaShaderCompiler;
    }

    /**
     * @return The manger for all dynamically added framebuffer attachments
     */
    public DynamicBufferManger getDynamicBufferManger() {
        return this.dynamicBufferManger;
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
     * @return The Veil light renderer instance
     */
    public LightRenderer getLightRenderer() {
        return this.lightRenderer;
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
        this.dynamicBufferManger.free();
        this.shaderManager.close();
        this.framebufferManager.free();
        this.postProcessingManager.free();
        this.quasarParticleManager.clear();
        this.cameraMatrices.free();
        this.lightRenderer.free();
        this.guiInfo.free();
    }
}
