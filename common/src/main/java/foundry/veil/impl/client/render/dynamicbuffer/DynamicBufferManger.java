package foundry.veil.impl.client.render.dynamicbuffer;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import foundry.veil.Veil;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.VeilRenderer;
import foundry.veil.api.client.render.dynamicbuffer.DynamicBufferType;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.ext.LevelRendererExtension;
import foundry.veil.impl.compat.SodiumCompat;
import foundry.veil.mixin.accessor.GameRendererAccessor;
import net.minecraft.client.Minecraft;
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
import static org.lwjgl.opengl.GL20C.*;
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
    private boolean enabled;
    private final int[] clearBuffers;
    private final Map<ResourceLocation, AdvancedFbo> framebuffers;
    private final EnumMap<DynamicBufferType, DynamicBuffer> dynamicBuffers;

    public DynamicBufferManger(int width, int height) {
        this.activeBuffers = 0;
        this.enabled = false;
        this.clearBuffers = Arrays.stream(DynamicBufferType.values()).mapToInt(type -> GL_COLOR_ATTACHMENT1 + type.ordinal()).toArray();
        this.framebuffers = new HashMap<>();
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
    }

    public int getActiveBuffers() {
        return this.activeBuffers;
    }

    public int getBufferTexture(DynamicBufferType type) {
        return this.dynamicBuffers.get(type).textureId;
    }

    public boolean setActiveBuffers(int activeBuffers) {
        if (this.activeBuffers == activeBuffers) {
            return false;
        }

        this.activeBuffers = activeBuffers;
        VeilRenderer renderer = VeilRenderSystem.renderer();
        renderer.getVanillaShaderCompiler().reload(((GameRendererAccessor) Minecraft.getInstance().gameRenderer).getShaders().values());
        this.deleteFramebuffers();

        // This rebuild all chunks in view without clearing them if normals need to be corrected
        if ((this.activeBuffers & DynamicBufferType.NORMAL.getMask()) != (activeBuffers & DynamicBufferType.NORMAL.getMask())) {
            ((LevelRendererExtension) Minecraft.getInstance().levelRenderer).markChunksDirty();
        }

        if (SodiumCompat.INSTANCE != null) {
            SodiumCompat.INSTANCE.recompile();
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

    @ApiStatus.Internal
    public void setupRenderState(ResourceLocation name, @Nullable RenderTarget renderTarget) {
        if (this.activeBuffers == 0 || !this.enabled) {
            return;
        }

        if (renderTarget == null) {
            VeilRenderSystem.renderer().getFramebufferManager().removeFramebuffer(name);
            AdvancedFbo fbo = this.framebuffers.remove(name);
            if (fbo != null) {
                fbo.free();
            }
            return;
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
        fbo.bind(true);
    }

    @ApiStatus.Internal
    public void clearRenderState() {
        if (this.activeBuffers == 0 || !this.enabled) {
            return;
        }

        this.setupRenderState(MAIN_WRAPPER, Minecraft.getInstance().getMainRenderTarget());
    }

    @ApiStatus.Internal
    public void clear() {
        for (AdvancedFbo framebuffer : this.framebuffers.values()) {
            framebuffer.bind(false);
            glDrawBuffers(this.clearBuffers);
            GlStateManager._clear(GL_COLOR_BUFFER_BIT, Minecraft.ON_OSX);
            glDrawBuffers(framebuffer.getDrawBuffers());
        }
    }

    @ApiStatus.Internal
    public void resizeFramebuffers(int width, int height) {
        this.deleteFramebuffers();
        for (DynamicBuffer buffer : this.dynamicBuffers.values()) {
            buffer.resize(width, height);
        }
    }

    public static int getShaderIndex(int glType, int activeBuffers) {
        for (int i = 0; i < GL_MAPPING.length; i++) {
            if (GL_MAPPING[i] == glType) {
                return i | activeBuffers << 4;
            }
        }
        throw new IllegalArgumentException("Invalid GL Shader Type: 0x" + Integer.toHexString(glType).toUpperCase(Locale.ROOT));
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
