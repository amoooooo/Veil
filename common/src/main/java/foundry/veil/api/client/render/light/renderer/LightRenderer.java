package foundry.veil.api.client.render.light.renderer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import foundry.veil.Veil;
import foundry.veil.api.client.registry.LightTypeRegistry;
import foundry.veil.api.client.render.CullFrustum;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.dynamicbuffer.DynamicBufferType;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.light.Light;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import foundry.veil.ext.LevelRendererExtension;
import foundry.veil.impl.client.render.dynamicbuffer.DynamicBufferManger;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.NativeResource;

import java.util.*;
import java.util.function.Consumer;

/**
 * Renders all lights in a scene.
 * <p>Lights can be added with {@link #addLight(Light)}, and subsequently removed with
 * {@link #removeLight(Light)}. Lights are automatically updated the next time {@link #render()}
 * is called if {@link Light#isDirty()} is <code>true</code>.
 * </p>
 * <p>There is no way to retrieve a light, so care should be taken to keep track of what lights
 * have been added to the scene and when they should be removed.</p>
 *
 * @author Ocelot
 */
public class LightRenderer implements NativeResource {

    private static final ResourceLocation BUFFER_ID = Veil.veilPath("lights");
    private final Map<LightTypeRegistry.LightType<?>, LightData<?>> lights;

    private boolean ambientOcclusionEnabled;
    private AdvancedFbo framebuffer;

    /**
     * Creates a new light renderer.
     */
    public LightRenderer() {
        this.lights = new HashMap<>();
        this.ambientOcclusionEnabled = true;
    }

    /**
     * Applies the shader set to {@link VeilRenderSystem}.
     */
    public void applyShader() {
        ShaderProgram shader = VeilRenderSystem.getShader();
        if (shader == null) {
            return;
        }

        shader.bind();
        AdvancedFbo fbo = AdvancedFbo.getMainFramebuffer();
        shader.setFramebufferSamplers(fbo);
        shader.setVector("ScreenSize", fbo.getWidth(), fbo.getHeight());

        DynamicBufferManger bufferManger = VeilRenderSystem.renderer().getDynamicBufferManger();
        for (DynamicBufferType dynamicBuffer : DynamicBufferType.values()) {
            if ((bufferManger.getActiveBuffers() & dynamicBuffer.getMask()) != 0) {
                shader.addSampler(dynamicBuffer.getSourceName() + "Sampler", bufferManger.getBufferTexture(dynamicBuffer));
            }
        }

        shader.applyShaderSamplers(0);
    }

    @ApiStatus.Internal
    public void setup(CullFrustum frustum) {
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
        RenderSystem.depthMask(false);

        for (Map.Entry<LightTypeRegistry.LightType<?>, LightData<?>> entry : this.lights.entrySet()) {
            entry.getValue().prepare(this, frustum);
        }
    }

    @ApiStatus.Internal
    public void clear() {
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    @ApiStatus.Internal
    public void render() {
        this.framebuffer = AdvancedFbo.getMainFramebuffer();

        boolean hasRendered = false;
        for (LightData<?> value : this.lights.values()) {
            // If there are no visible lights, then don't render anything
            if (value.renderer.getVisibleLights() > 0) {
                hasRendered = true;
                if (VeilRenderSystem.renderer().enableBuffers(BUFFER_ID, DynamicBufferType.ALBEDO, DynamicBufferType.NORMAL)) {
                    break;
                }
                value.render(this);
            }
        }
        if (!hasRendered) {
            VeilRenderSystem.renderer().disableBuffers(BUFFER_ID, DynamicBufferType.ALBEDO, DynamicBufferType.NORMAL);
        }

        this.framebuffer = null;
    }

    /**
     * Adds a light to the renderer.
     *
     * @param light The light to add
     */
    public void addLight(Light light) {
        Objects.requireNonNull(light, "light");
        RenderSystem.assertOnRenderThreadOrInit();
        this.lights.computeIfAbsent(light.getType(), LightData::new).addLight(light);
    }

    /**
     * Removes the specified light from the renderer.
     *
     * @param light The light to remove
     */
    @SuppressWarnings("unchecked")
    public <T extends Light> void removeLight(T light) {
        Objects.requireNonNull(light, "light");
        RenderSystem.assertOnRenderThreadOrInit();

        LightData<T> data = (LightData<T>) this.lights.get(light.getType());
        if (data != null) {
            data.removedLights.add(light);
        }
    }

    /**
     * Retrieves all lights of the specified type.
     *
     * @param type The type of lights to get
     * @return A list of lights for the specified type in the scene
     */
    @SuppressWarnings("unchecked")
    public <T extends Light> List<T> getLights(LightTypeRegistry.LightType<? extends T> type) {
        LightData<?> data = this.lights.get(type);
        if (data == null) {
            return Collections.emptyList();
        }

        return (List<T>) data.lightsView;
    }

    /**
     * Enables ambient occlusion.
     */
    public void enableAmbientOcclusion() {
        if (!this.ambientOcclusionEnabled) {
            this.ambientOcclusionEnabled = true;
            ((LevelRendererExtension) Minecraft.getInstance().levelRenderer).markChunksDirty();
        }
    }

    /**
     * Disables ambient occlusion.
     */
    public void disableAmbientOcclusion() {
        if (this.ambientOcclusionEnabled) {
            this.ambientOcclusionEnabled = false;
            ((LevelRendererExtension) Minecraft.getInstance().levelRenderer).markChunksDirty();
        }
    }

    /**
     * @return The deferred framebuffer being read from
     */
    public @Nullable AdvancedFbo getFramebuffer() {
        return this.framebuffer;
    }

    /**
     * @return Whether chunks can have ambient occlusion
     */
    public boolean isAmbientOcclusionEnabled() {
        return this.ambientOcclusionEnabled;
    }

    @Override
    public void free() {
        this.lights.values().forEach(LightData::free);
        this.lights.clear();
    }

    @ApiStatus.Internal
    public void addDebugInfo(Consumer<String> consumer) {
        int visible = this.lights.values().stream().mapToInt(data -> data.renderer.getVisibleLights()).sum();
        int all = this.lights.values().stream().mapToInt(data -> data.lights.size()).sum();
        consumer.accept("Lights: " + visible + " / " + all);
    }

    @ApiStatus.Internal
    private static class LightData<T extends Light> implements NativeResource {

        private final LightTypeRenderer<T> renderer;
        private final List<T> lights;
        private final List<T> lightsView;
        private final Set<T> removedLights;

        private LightData(LightTypeRenderer<T> renderer) {
            this.renderer = renderer;
            this.lights = new ArrayList<>();
            this.lightsView = Collections.unmodifiableList(this.lights);
            this.removedLights = new HashSet<>();
        }

        @SuppressWarnings("unchecked")
        public LightData(LightTypeRegistry.LightType<?> type) {
            this((LightTypeRenderer<T>) Objects.requireNonNull(type, "type").rendererFactory().createRenderer());
        }

        private void prepare(LightRenderer lightRenderer, CullFrustum frustum) {
            this.lights.removeAll(this.removedLights);
            this.renderer.prepareLights(lightRenderer, this.lights, this.removedLights, frustum);
            this.removedLights.clear();
        }

        private void render(LightRenderer lightRenderer) {
            this.renderer.renderLights(lightRenderer, this.lights);
        }

        @SuppressWarnings("unchecked")
        private void addLight(Light light) {
            this.lights.add((T) light);
        }

        @Override
        public void free() {
            this.renderer.free();
        }
    }
}
