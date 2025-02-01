package foundry.veil.impl.client.render.dynamicbuffer;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import foundry.veil.Veil;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.VeilRenderer;
import foundry.veil.api.client.render.dynamicbuffer.DynamicBufferType;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.framebuffer.FramebufferAttachmentDefinition;
import foundry.veil.api.compat.SodiumCompat;
import foundry.veil.ext.RenderTargetExtension;
import foundry.veil.ext.ShaderInstanceExtension;
import foundry.veil.mixin.dynamicbuffer.accessor.DynamicBufferGameRendererAccessor;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.NativeResource;

import java.nio.IntBuffer;
import java.util.*;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL12C.*;
import static org.lwjgl.opengl.GL14C.GL_TEXTURE_LOD_BIAS;
import static org.lwjgl.opengl.GL20C.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20C.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL30C.GL_COLOR_ATTACHMENT1;
import static org.lwjgl.opengl.GL32C.GL_GEOMETRY_SHADER;
import static org.lwjgl.opengl.GL40C.GL_TESS_CONTROL_SHADER;
import static org.lwjgl.opengl.GL40C.GL_TESS_EVALUATION_SHADER;
import static org.lwjgl.opengl.GL43C.GL_COMPUTE_SHADER;

public class DynamicBufferManger implements NativeResource {

    private static final int[] GL_MAPPING = {
            GL_VERTEX_SHADER,
            GL_TESS_CONTROL_SHADER,
            GL_TESS_EVALUATION_SHADER,
            GL_GEOMETRY_SHADER,
            GL_FRAGMENT_SHADER,
            GL_COMPUTE_SHADER
    };
    public static final ResourceLocation MAIN_WRAPPER = Veil.veilPath("dynamic_main");

    private int activeBuffers;
    private final Object2IntMap<ResourceLocation> activeBufferLayers;
    private boolean enabled;
    private final int[] clearBuffers;
    private final Map<ResourceLocation, AdvancedFbo> framebuffers;
    private final List<AdvancedFbo> dynamicFramebuffers;
    private final EnumMap<DynamicBufferType, DynamicBuffer> dynamicBuffers;
    private int dynamicFboPointer;

    public DynamicBufferManger(int width, int height) {
        this.activeBuffers = 0;
        this.activeBufferLayers = new Object2IntArrayMap<>();
        this.enabled = false;
        this.clearBuffers = Arrays.stream(DynamicBufferType.values()).mapToInt(type -> GL_COLOR_ATTACHMENT1 + type.ordinal()).toArray();
        this.framebuffers = new HashMap<>();
        this.dynamicFramebuffers = new ArrayList<>();
        this.dynamicBuffers = new EnumMap<>(DynamicBufferType.class);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer textures = stack.mallocInt(DynamicBufferType.values().length);
            glGenTextures(textures);
            for (DynamicBufferType value : DynamicBufferType.values()) {
                DynamicBuffer buffer = new DynamicBuffer(value, textures.get(value.ordinal()));
                buffer.init(width, height);
                this.dynamicBuffers.put(value, buffer);
            }
        }
    }

    private void deleteFramebuffers() {
        for (Map.Entry<ResourceLocation, AdvancedFbo> entry : this.framebuffers.entrySet()) {
            entry.getValue().free();
            VeilRenderSystem.renderer().getFramebufferManager().removeFramebuffer(entry.getKey());
        }
        this.framebuffers.clear();
        for (AdvancedFbo fbo : this.dynamicFramebuffers) {
            fbo.free();
        }
        this.dynamicFramebuffers.clear();
    }

    public int getActiveBuffers(ResourceLocation name) {
        return this.activeBufferLayers.getOrDefault(name, 0);
    }

    public int getActiveBuffers() {
        return this.activeBuffers;
    }

    public int getBufferTexture(DynamicBufferType buffer) {
        if ((this.activeBuffers & buffer.getMask()) != 0) {
            int index = Integer.bitCount(this.activeBuffers & (buffer.getMask() - 1));
            int texture = ((RenderTargetExtension) Minecraft.getInstance().getMainRenderTarget()).veil$getTexture(index);
            if (texture != -1) {
                return texture;
            }
            return this.dynamicBuffers.get(buffer).textureId;
        }
        return MissingTextureAtlasSprite.getTexture().getId();
    }

    public boolean setActiveBuffers(ResourceLocation name, int activeBuffers) {
        int buffers = this.activeBufferLayers.getOrDefault(name, 0);
        if (buffers == activeBuffers) {
            return false;
        }

        if (activeBuffers == 0) {
            this.activeBufferLayers.removeInt(name);
        } else {
            this.activeBufferLayers.put(name, activeBuffers);
        }

        int flags = 0;
        for (int value : this.activeBufferLayers.values()) {
            flags |= value;
        }
        if (flags == this.activeBuffers) {
            return false;
        }

        this.activeBuffers = flags;
        this.deleteFramebuffers();

        VeilRenderer renderer = VeilRenderSystem.renderer();
        List<ShaderInstance> shaders = new ArrayList<>();

        DynamicBufferGameRendererAccessor accessor = (DynamicBufferGameRendererAccessor) Minecraft.getInstance().gameRenderer;
        for (ShaderInstance shader : accessor.getShaders().values()) {
            if (((ShaderInstanceExtension) shader).veil$swapBuffers(this.activeBuffers)) {
                shaders.add(shader);
            }
        }
        if (!shaders.isEmpty()) {
            renderer.getVanillaShaderCompiler().reload(shaders);
        }

        // This rebuild all chunks in view without clearing them if normals need to be corrected
        if ((this.activeBuffers & DynamicBufferType.NORMAL.getMask()) != (activeBuffers & DynamicBufferType.NORMAL.getMask())) {
            VeilRenderSystem.rebuildChunks();
        }

        if (SodiumCompat.INSTANCE != null) {
            SodiumCompat.INSTANCE.setActiveBuffers(activeBuffers);
        }

        try {
            renderer.getShaderManager().setActiveBuffers(activeBuffers);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return true;
    }

    @ApiStatus.Internal
    public boolean isEnabled() {
        return this.enabled;
    }

    @ApiStatus.Internal
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void free() {
        this.deleteFramebuffers();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer textures = stack.mallocInt(DynamicBufferType.values().length);
            for (DynamicBufferType value : DynamicBufferType.values()) {
                textures.put(value.ordinal(), this.dynamicBuffers.get(value).textureId);
            }
            glDeleteTextures(textures);
        }
        this.dynamicBuffers.clear();
    }

    /**
     * Sets up the rendering state for the specified target.
     *
     * @param name         The name of the framebuffer
     * @param renderTarget The render target to wrap or <code>null</code> to free
     * @param setViewport  Whether the viewport should also be set
     * @return Whether the buffer was bound successfully
     */
    @ApiStatus.Internal
    public boolean setupRenderState(ResourceLocation name, @Nullable RenderTarget renderTarget, boolean setViewport) {
        if (this.activeBuffers == 0 || !this.enabled) {
            return false;
        }

        if (renderTarget == null) {
            VeilRenderSystem.renderer().getFramebufferManager().removeFramebuffer(name);
            AdvancedFbo fbo = this.framebuffers.remove(name);
            if (fbo != null) {
                fbo.free();
            }
            // If the buffer doesn't exist, then try to bind the main framebuffer
            return this.setupRenderState(MAIN_WRAPPER, Objects.requireNonNull(Minecraft.getInstance().getMainRenderTarget()), setViewport);
        }

        AdvancedFbo fbo = this.framebuffers.get(name);
        if (fbo == null) {
            AdvancedFbo.Builder builder = AdvancedFbo.withSize(renderTarget.width, renderTarget.height);
            builder.addColorTextureWrapper(renderTarget.getColorTextureId());
            for (Map.Entry<DynamicBufferType, DynamicBuffer> entry : this.dynamicBuffers.entrySet()) {
                DynamicBufferType type = entry.getKey();
                if ((this.activeBuffers & type.getMask()) != 0) {
                    builder.setName(type.getSourceName()).addColorTextureWrapper(entry.getValue().textureId);
                }
            }
            builder.setDepthTextureWrapper(renderTarget.getDepthTextureId());
            fbo = builder.build(true);
            this.framebuffers.put(name, fbo);
        }

        VeilRenderSystem.renderer().getFramebufferManager().setFramebuffer(name, fbo);
        fbo.bind(setViewport);
        return true;
    }

    /**
     * Creates a dynamic fbo from the specified framebuffer. It will only write into the first color texture buffer and optionally the depth texture.
     *
     * @param framebuffer The framebuffer to add dynamic buffers to
     * @return The created buffer or <code>null</code> to use the input value
     */
    public @Nullable AdvancedFbo getDynamicFbo(AdvancedFbo framebuffer) {
        if (this.activeBuffers == 0 || !this.enabled) {
            return null;
        }

        if (!framebuffer.isColorTextureAttachment(0)) {
            return null;
        }

        if (this.dynamicFboPointer < this.dynamicFramebuffers.size()) {
            AdvancedFbo fbo = this.dynamicFramebuffers.get(this.dynamicFboPointer);
            if (fbo.getWidth() == framebuffer.getWidth() && fbo.getHeight() == framebuffer.getHeight()) {
                this.dynamicFboPointer++;
                return fbo;
            }
            fbo.free();
        }

        AdvancedFbo.Builder builder = AdvancedFbo.withSize(framebuffer.getWidth(), framebuffer.getHeight());
        builder.addColorTextureWrapper(framebuffer.getColorTextureAttachment(0).getId());
        for (Map.Entry<DynamicBufferType, DynamicBuffer> entry : this.dynamicBuffers.entrySet()) {
            DynamicBufferType type = entry.getKey();
            if ((this.activeBuffers & type.getMask()) != 0) {
//                if (createTextures) {
                    builder.setName(type.getSourceName())
                            .setFormat(type.getTexelFormat(), type.getInternalFormat())
                            .addColorTextureBuffer();
//                } else {
//                    builder.setName(type.getSourceName())
//                            .addColorTextureWrapper(entry.getValue().textureId);
//                }
            }
        }
        if (framebuffer.isDepthTextureAttachment()) {
            builder.setDepthTextureWrapper(framebuffer.getDepthTextureAttachment().getId());
        } else {
            builder.setFormat(FramebufferAttachmentDefinition.Format.DEPTH_COMPONENT).setDepthTextureBuffer();
        }
        AdvancedFbo fbo = builder.build(true);

        if (this.dynamicFboPointer < this.dynamicFramebuffers.size()) {
            this.dynamicFramebuffers.set(this.dynamicFboPointer, fbo);
        } else {
            this.dynamicFramebuffers.add(fbo);
        }
        this.dynamicFboPointer++;

        return fbo;
    }

    public int[] getClearBuffers() {
        return this.clearBuffers;
    }

    /**
     * Clears the current render state by binding the main framebuffer.
     *
     * @param setViewport Whether the viewport should also be set
     * @return Whether the buffer was bound successfully
     */
    @ApiStatus.Internal
    public boolean clearRenderState(boolean setViewport) {
        if (this.activeBuffers == 0 || !this.enabled) {
            return false;
        }

        return this.setupRenderState(MAIN_WRAPPER, Minecraft.getInstance().getMainRenderTarget(), setViewport);
    }

    @ApiStatus.Internal
    public void clear() {
        for (AdvancedFbo framebuffer : this.framebuffers.values()) {
            framebuffer.clear(0.0F, 0.0F, 0.0F, 0.0F, GL_COLOR_BUFFER_BIT, this.clearBuffers);
        }
        ListIterator<AdvancedFbo> iterator = this.dynamicFramebuffers.listIterator(this.dynamicFboPointer);
        while (iterator.hasNext()) {
            iterator.next().free();
            iterator.remove();
        }
        this.dynamicFboPointer = 0;
    }

    @ApiStatus.Internal
    public void resizeFramebuffers(int width, int height) {
        this.deleteFramebuffers();
        for (DynamicBuffer buffer : this.dynamicBuffers.values()) {
            buffer.resize(width, height);
        }
    }

    private record DynamicBuffer(DynamicBufferType type, int textureId) {

        public void init(int width, int height) {
            GlStateManager._bindTexture(this.textureId);
            GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

            GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, 0);
            GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_MIN_LOD, 0);
            GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAX_LOD, 0);
            GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_LOD_BIAS, 0.0F);
            GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

            GlStateManager._texImage2D(GL_TEXTURE_2D, 0, this.type.getInternalFormat(), width, height, 0, this.type.getTexelFormat(), GL_UNSIGNED_INT, null);
        }

        public void resize(int width, int height) {
            GlStateManager._bindTexture(this.textureId);
            GlStateManager._texImage2D(GL_TEXTURE_2D, 0, this.type.getInternalFormat(), width, height, 0, this.type.getTexelFormat(), GL_UNSIGNED_INT, null);
        }
    }
}
