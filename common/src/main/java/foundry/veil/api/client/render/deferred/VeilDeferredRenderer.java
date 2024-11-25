package foundry.veil.api.client.render.deferred;

import com.mojang.blaze3d.systems.RenderSystem;
import foundry.veil.Veil;
import foundry.veil.VeilClient;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.VeilRenderer;
import foundry.veil.api.client.render.deferred.light.renderer.LightRenderer;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.framebuffer.FramebufferManager;
import foundry.veil.api.client.render.framebuffer.VeilFramebuffers;
import foundry.veil.api.client.render.post.PostPipeline;
import foundry.veil.api.client.render.post.PostProcessingManager;
import foundry.veil.api.client.render.shader.ShaderManager;
import foundry.veil.api.client.render.shader.definition.ShaderPreDefinitions;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.ApiStatus;
import org.lwjgl.system.NativeResource;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import static org.lwjgl.opengl.GL11C.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11C.GL_NEAREST;

/**
 * <p>Handles mixing the regular deferred pipeline and the forward-rendered transparency pipeline.</p>
 * <p>The rendering pipeline goes in this order:</p>
 * <ul>
 *     <li>Opaque Shaders</li>
 *     <li>Opaque post-processing ({@link VeilDeferredRenderer#OPAQUE_POST})</li>
 *     <li>Light Shaders via {@link LightRenderer}</li>
 *     <li>Light post-processing ({@link VeilDeferredRenderer#LIGHT_POST})</li>
 *     <li>Transparency Shaders</li>
 *     <li>Transparency post-processing ({@link VeilDeferredRenderer#TRANSPARENT_POST})</li>
 *     <li>Light Shaders via {@link LightRenderer}</li>
 *     <li>Light post-processing ({@link VeilDeferredRenderer#LIGHT_POST})</li>
 *     <li>Final image compositing</li>
 *     <li>Final post-processing via {@link PostProcessingManager}</li>
 * </ul>
 *
 * @author Ocelot
 */
public class VeilDeferredRenderer implements PreparableReloadListener, NativeResource {

    public static final ResourceLocation PACK_ID = Veil.veilPath("deferred");
    public static final String DISABLE_VANILLA_ENTITY_LIGHT_KEY = "DISABLE_VANILLA_ENTITY_LIGHT";

    public static final ResourceLocation OPAQUE_POST = Veil.veilPath("core/opaque");
    public static final ResourceLocation LIGHT_POST = Veil.veilPath("core/light");
    public static final ResourceLocation OPAQUE_MIX = Veil.veilPath("core/mix_opaque");
    public static final ResourceLocation TRANSPARENT_MIX = Veil.veilPath("core/mix_transparent");
    public static final ResourceLocation TRANSPARENT_POST = Veil.veilPath("core/transparent");
    public static final ResourceLocation SCREEN_POST = Veil.veilPath("core/screen");

    public static final MutableComponent UNSUPPORTED_TITLE = Component.translatable(Veil.MODID + ".deferred.unsupported.title");
    public static final MutableComponent UNSUPPORTED_SODIUM_DESC = Component.translatable(Veil.MODID + ".deferred.unsupported.sodium_desc");
    public static final MutableComponent UNSUPPORTED_FABULOUS_DESC = Component.translatable(Veil.MODID + ".deferred.unsupported.fabulous_desc");

    private final ShaderManager deferredShaderManager;
    private final ShaderPreDefinitions shaderPreDefinitions;
    private final FramebufferManager framebufferManager;
    private final PostProcessingManager postProcessingManager;

    private boolean enabled;
    private RendererState state;

    public VeilDeferredRenderer(ShaderManager deferredShaderManager, ShaderPreDefinitions shaderPreDefinitions, FramebufferManager framebufferManager, PostProcessingManager postProcessingManager) {
        this.deferredShaderManager = deferredShaderManager;
        this.shaderPreDefinitions = shaderPreDefinitions;
        this.framebufferManager = framebufferManager;
        this.postProcessingManager = postProcessingManager;

        this.enabled = false;
        this.state = RendererState.INACTIVE;
    }

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier preparationBarrier, ResourceManager resourceManager, ProfilerFiller prepareProfiler, ProfilerFiller applyProfiler, Executor backgroundExecutor, Executor gameExecutor) {
        return CompletableFuture.<CompletableFuture<Void>>supplyAsync(() -> {
            boolean selected = Minecraft.getInstance().getResourcePackRepository().getSelectedIds().contains(PACK_ID.toString()) || resourceManager.listPacks().anyMatch(pack -> pack.packId().equals(PACK_ID.toString()));
            boolean active = selected && isSupported();
            if (selected && Minecraft.useShaderTransparency()) {
                SystemToast.add(Minecraft.getInstance().getToasts(), VeilClient.UNSUPPORTED_NOTIFICATION, UNSUPPORTED_TITLE, UNSUPPORTED_FABULOUS_DESC);
            }
            if (this.enabled != active) {
                this.enabled = active;
                if (active) {
                    Veil.LOGGER.info("Deferred Renderer Enabled");
                } else {
                    Veil.LOGGER.info("Deferred Renderer Disabled");
                    return preparationBarrier.wait(null).thenRunAsync(this::free, gameExecutor);
                }
            }

            if (this.enabled) {
                return this.deferredShaderManager.reload(preparationBarrier, resourceManager, prepareProfiler, applyProfiler, backgroundExecutor, gameExecutor);
            }

            return preparationBarrier.wait(null);
        }, gameExecutor).thenCompose(future -> future);
    }

    @Override
    public void free() {
        this.enabled = false;
        this.state = RendererState.INACTIVE;
        this.deferredShaderManager.close();
    }

//    @ApiStatus.Internal
//    public void reset() {
//        this.lightRenderer.free();
//    }

    @ApiStatus.Internal
    public void setup() {
//        if (!this.isEnabled() || this.state == RendererState.DISABLED) {
//            return;
//        }
//
//        switch (this.state) {
//            case OPAQUE -> {
//                AdvancedFbo deferredFramebuffer = this.framebufferManager.getFramebuffer(VeilFramebuffers.OPAQUE);
//                if (deferredFramebuffer == null) {
//                    Veil.LOGGER.error("Missing deferred opaque buffer");
//                    this.free();
//                    return;
//                }
//
//                deferredFramebuffer.bind(true);
//            }
//            case TRANSLUCENT -> {
//                AdvancedFbo transparent = this.framebufferManager.getFramebuffer(VeilFramebuffers.TRANSPARENT);
//                if (transparent == null) {
//                    Veil.LOGGER.error("Missing deferred transparent buffer");
//                    this.free();
//                    return;
//                }
//
//                transparent.bind(true);
//            }
//        }

        // Temporary hack until we blit
//        deferredFramebuffer.bindDraw(false);
//        glDrawBuffers(GL_COLOR_ATTACHMENT0);
//        AdvancedFbo.getMainFramebuffer().resolveToAdvancedFbo(deferredFramebuffer);
//        deferredFramebuffer.bind(true);
//        glDrawBuffers(deferredFramebuffer.getDrawBuffers());
    }

    @ApiStatus.Internal
    public void clear() {
//        if (!this.isEnabled() || this.state == RendererState.DISABLED) {
//            return;
//        }
//
//        switch (this.state) {
//            case OPAQUE, TRANSLUCENT -> AdvancedFbo.unbind();
//        }
    }

    @ApiStatus.Internal
    public void beginOpaque() {
//        if (!this.isEnabled() || this.state == RendererState.DISABLED) {
//            return;
//        }
//
//        this.state = RendererState.OPAQUE;
//        AdvancedFbo deferred = this.framebufferManager.getFramebuffer(VeilFramebuffers.OPAQUE);
//        if (deferred == null) {
//            Veil.LOGGER.error("Missing deferred opaque buffer");
//            this.free();
//        }
    }

    @ApiStatus.Internal
    public void beginTranslucent() {
//        if (!this.isEnabled() || this.state == RendererState.DISABLED) {
//            return;
//        }
//
//        this.state = RendererState.TRANSLUCENT;
//        AdvancedFbo transparent = this.framebufferManager.getFramebuffer(VeilFramebuffers.TRANSPARENT);
//        if (transparent == null) {
//            Veil.LOGGER.error("Missing deferred transparent buffer");
//            this.free();
//            return;
//        }
//
//        // Copy opaque depth to transparency, so it doesn't draw on top
//        AdvancedFbo deferredFramebuffer = this.framebufferManager.getFramebuffer(VeilFramebuffers.OPAQUE);
//        if (deferredFramebuffer != null) {
//            deferredFramebuffer.resolveToAdvancedFbo(transparent, GL_DEPTH_BUFFER_BIT, GL_NEAREST);
//        }
    }

    private void run(ProfilerFiller profiler, AdvancedFbo deferred, AdvancedFbo light, ResourceLocation post, ResourceLocation mix) {
        PostPipeline postPipeline = this.postProcessingManager.getPipeline(post);
        if (postPipeline != null) {
            profiler.push("post");
            this.postProcessingManager.runPipeline(postPipeline, false);
            profiler.pop();
        }

        profiler.push("draw_lights");
        light.bind(true);
//        this.lightRenderer.render(deferred);
        profiler.pop();

        // Applies effects to the final light image
        PostPipeline lightPipeline = this.postProcessingManager.getPipeline(LIGHT_POST);
        if (lightPipeline != null) {
            profiler.push("light_post");
            this.postProcessingManager.runPipeline(lightPipeline, false);
            profiler.pop();
        }

        // Applies light to the image
        PostPipeline mixPipeline = this.postProcessingManager.getPipeline(mix);
        if (mixPipeline != null) {
            profiler.push("mix");
            this.postProcessingManager.runPipeline(mixPipeline, false);
            profiler.pop();
        }
    }

//    @ApiStatus.Internal
//    public void blit() {
//        if (!this.isEnabled() || this.state == RendererState.DISABLED) {
//            // Resolve the main framebuffer to the post one for later post-processing
//            AdvancedFbo postFramebuffer = VeilRenderSystem.renderer().getFramebufferManager().getFramebuffer(VeilFramebuffers.POST);
//            if (postFramebuffer != null) {
//                AdvancedFbo.getMainFramebuffer().resolveToAdvancedFbo(postFramebuffer);
//            }
//            return;
//        }
//
//        ProfilerFiller profiler = Minecraft.getInstance().getProfiler();
//        profiler.push("veil_deferred");
//
//        this.end();
//
//        AdvancedFbo deferred = this.framebufferManager.getFramebuffer(VeilFramebuffers.OPAQUE);
//        AdvancedFbo transparent = this.framebufferManager.getFramebuffer(VeilFramebuffers.TRANSPARENT);
//        AdvancedFbo deferredLight = this.framebufferManager.getFramebuffer(VeilFramebuffers.OPAQUE_LIGHT);
//        AdvancedFbo transparentLight = this.framebufferManager.getFramebuffer(VeilFramebuffers.TRANSPARENT_LIGHT);
//        AdvancedFbo post = this.framebufferManager.getFramebuffer(VeilFramebuffers.POST);
//        if (deferred == null || transparent == null || deferredLight == null || transparentLight == null || post == null) {
//            Veil.LOGGER.error("Missing deferred light buffers");
//            this.free();
//            return;
//        }
//
//        profiler.push("setup_lights");
//        this.lightRenderer.setup(VeilRenderer.getCullingFrustum());
//        profiler.popPush("opaque_light");
//        this.run(profiler, deferred, deferredLight, OPAQUE_POST, OPAQUE_MIX);
//        profiler.popPush("transparent_light");
//        this.run(profiler, transparent, transparentLight, TRANSPARENT_POST, TRANSPARENT_MIX);
//        profiler.pop();
//        this.lightRenderer.clear();
//
//        profiler.push("screen_post");
//
//        // Draws the final opaque image and transparent onto the background
//        PostPipeline screenPipeline = this.postProcessingManager.getPipeline(SCREEN_POST);
//        if (screenPipeline != null) {
//            RenderSystem.enableBlend();
//            RenderSystem.defaultBlendFunc();
//            this.postProcessingManager.runPipeline(screenPipeline, false);
//            RenderSystem.disableBlend();
//        }
//
//        // Don't resolve to the main framebuffer to preserve the HDR data
////        profiler.popPush("resolve");
////        post.resolveToFramebuffer(Minecraft.getInstance().getMainRenderTarget());
//        profiler.pop();
//
//        profiler.pop();
//    }

    @ApiStatus.Internal
    public void end() {
        if (!this.isEnabled() || this.state == RendererState.DISABLED) {
            return;
        }

        this.state = RendererState.INACTIVE;
    }

//    @ApiStatus.Internal
//    public void addDebugInfo(Consumer<String> consumer) {
//        boolean ambientOcclusion = this.lightRenderer.isAmbientOcclusionEnabled();
//        boolean vanillaLights = this.lightRenderer.isVanillaLightEnabled();
//        boolean vanillaEntityLights = this.shaderPreDefinitions.getDefinition(DISABLE_VANILLA_ENTITY_LIGHT_KEY) == null;
//        consumer.accept("Ambient Occlusion: " + (ambientOcclusion ? ChatFormatting.GREEN + "On" : ChatFormatting.RED + "Off"));
//        consumer.accept("Vanilla Light: " + (vanillaLights ? ChatFormatting.GREEN + "On" : ChatFormatting.RED + "Off"));
//        consumer.accept("Vanilla Entity Light: " + (vanillaEntityLights ? ChatFormatting.GREEN + "On" : ChatFormatting.RED + "Off"));
//        this.lightRenderer.addDebugInfo(consumer);
//    }

    /**
     * Allows the renderer to run normally.
     */
    public void enable() {
        this.state = RendererState.INACTIVE;
    }

    /**
     * Forces the renderer off.
     */
    public void disable() {
        this.state = RendererState.DISABLED;
    }

    /**
     * @return Whether the deferred renderer is initialized and ready to use
     */
    public boolean isEnabled() {
        return this.enabled && isSupported();
    }

    /**
     * @return Whether the deferred rendering pipeline is supported
     */
    public static boolean isSupported() {
        return !Minecraft.useShaderTransparency() && !Veil.SODIUM; // TODO allow fabulous/sodium
    }

    /**
     * @return Whether the deferred renderer is currently actively being used
     */
    public boolean isActive() {
        return this.isEnabled() && this.state.isActive();
    }

    public RendererState getRendererState() {
        return this.state;
    }

    public ShaderManager getDeferredShaderManager() {
        return this.deferredShaderManager;
    }

    public enum RendererState {
        DISABLED, INACTIVE, OPAQUE, TRANSLUCENT;

        public boolean isActive() {
            return this == OPAQUE || this == TRANSLUCENT;
        }
    }
}
