package foundry.veil.mixin.dynamicbuffer.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.Program;
import com.mojang.blaze3d.shaders.Shader;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.VertexFormat;
import foundry.veil.Veil;
import foundry.veil.api.client.render.ext.VeilDebug;
import foundry.veil.ext.ShaderInstanceExtension;
import foundry.veil.impl.client.render.dynamicbuffer.VanillaShaderCompiler;
import foundry.veil.impl.client.render.shader.program.ShaderProgramImpl;
import foundry.veil.mixin.dynamicbuffer.accessor.DynamicBufferProgramAccessor;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL20C.*;
import static org.lwjgl.opengl.KHRDebug.GL_PROGRAM;
import static org.lwjgl.opengl.KHRDebug.GL_SHADER;

@Mixin(ShaderInstance.class)
public abstract class DynamicBufferShaderInstanceMixin implements Shader, ShaderInstanceExtension {

    @Mutable
    @Shadow
    @Final
    private Program vertexProgram;

    @Mutable
    @Shadow
    @Final
    private Program fragmentProgram;

    @Mutable
    @Shadow
    @Final
    private int programId;

    @Shadow
    @Final
    private VertexFormat vertexFormat;

    @Shadow
    @Final
    public Map<String, Uniform> uniformMap;

    @Shadow
    @Final
    private List<Integer> uniformLocations;

    @Shadow
    @Final
    private List<Integer> samplerLocations;

    @Shadow
    @Final
    private String name;

    @Shadow
    protected abstract void updateLocations();

    @Shadow
    @Final
    private List<Uniform> uniforms;

    @Shadow
    public abstract void attachToProgram();

    @Unique
    private String veil$vertexSource;
    @Unique
    private String veil$fragmentSource;
    @Unique
    private int veil$activeBuffers;
    @Unique
    private final Int2IntMap veil$programCache = new Int2IntArrayMap(1);

    @Inject(method = "<init>", at = @At("TAIL"))
    public void init(CallbackInfo ci) {
        this.veil$programCache.put(0, this.programId);
        if ((Object) this instanceof ShaderProgramImpl.Wrapper) {
            return;
        }

        VeilDebug debug = VeilDebug.get();
        debug.objectLabel(GL_PROGRAM, this.programId, "Vanilla Shader Program " + this.name + ":default");
        debug.objectLabel(GL_SHADER, ((DynamicBufferProgramAccessor) this.vertexProgram).getId(), "Vanilla vertex Shader " + this.vertexProgram.getName() + ":default");
        debug.objectLabel(GL_SHADER, ((DynamicBufferProgramAccessor) this.fragmentProgram).getId(), "Vanilla fragment Shader " + this.fragmentProgram.getName() + ":default");
    }

    @Inject(method = "apply", at = @At("HEAD"))
    public void apply(CallbackInfo ci) {
        if (Veil.platform().hasErrors()) {
            return;
        }

        VanillaShaderCompiler.markRendered(this.name);
        this.veil$applyCompile();
    }

    @Inject(method = "close", at = @At("HEAD"))
    public void close(CallbackInfo ci) {
        if (this.veil$programCache.isEmpty()) {
            return;
        }

        // Swap out the program before vanilla mc tries to delete them
        this.programId = this.veil$programCache.remove(0);
        // Delete all extra shaders created by veil
        for (int program : this.veil$programCache.values()) {
            glDeleteProgram(program);
        }
        this.veil$programCache.clear();
    }

    @Unique
    private void veil$applyCompile() {
        if (this.veil$vertexSource != null && this.veil$fragmentSource != null) {
            int oldProgram = this.programId;
            this.programId = glCreateProgram();
            int vertexShader = glCreateShader(GL_VERTEX_SHADER);
            int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);

            try {
                GlStateManager.glShaderSource(vertexShader, List.of(this.veil$vertexSource));
                glCompileShader(vertexShader);
                if (glGetShaderi(vertexShader, GL_COMPILE_STATUS) != GL_TRUE) {
                    String error = glGetShaderInfoLog(vertexShader).trim();
                    throw new IOException("Couldn't compile dynamic vertex program (" + this.vertexProgram.getName() + ", " + this.name + ") : " + error);
                }

                GlStateManager.glShaderSource(fragmentShader, List.of(this.veil$fragmentSource));
                glCompileShader(fragmentShader);
                if (glGetShaderi(fragmentShader, GL_COMPILE_STATUS) != GL_TRUE) {
                    String error = glGetShaderInfoLog(fragmentShader).trim();
                    throw new IOException("Couldn't compile dynamic fragment program (" + this.fragmentProgram.getName() + ", " + this.name + ") : " + error);
                }

                int index = 0;
                for (String name : this.vertexFormat.getElementAttributeNames()) {
                    glBindAttribLocation(this.programId, index, name);
                    index++;
                }

                // This allows other people to attach their own shaders
                DynamicBufferProgramAccessor vertexAccessor = (DynamicBufferProgramAccessor) this.vertexProgram;
                DynamicBufferProgramAccessor fragmentAccessor = (DynamicBufferProgramAccessor) this.fragmentProgram;

                int oldVertex = vertexAccessor.getId();
                int oldFragment = fragmentAccessor.getId();
                vertexAccessor.setId(vertexShader);
                fragmentAccessor.setId(fragmentShader);
                this.attachToProgram();
                vertexAccessor.setId(oldVertex);
                fragmentAccessor.setId(oldFragment);

                glLinkProgram(this.programId);
                if (glGetProgrami(this.programId, GL_LINK_STATUS) != GL_TRUE) {
                    String error = glGetProgramInfoLog(this.programId).trim();
                    throw new IOException("Couldn't link shader (" + this.name + ") : " + error);
                }

                this.veil$invalidate();

                // This is a *bit* of a waste, but it's more compatible than trying to attach new shaders to the program
                int old = this.veil$programCache.put(this.veil$activeBuffers, this.programId);
                if (old != 0) {
                    glDeleteProgram(old);
                }

                // Add debug names
                String type = this.veil$activeBuffers == 0 ? "default" : Integer.toString(this.veil$activeBuffers);
                VeilDebug debug = VeilDebug.get();
                debug.objectLabel(GL_PROGRAM, this.programId, "Vanilla Shader Program " + this.name + ":" + type);
                debug.objectLabel(GL_SHADER, vertexShader, "Vanilla vertex Shader " + this.vertexProgram.getName() + ":" + type);
                debug.objectLabel(GL_SHADER, fragmentShader, "Vanilla fragment Shader " + this.fragmentProgram.getName() + ":" + type);
            } catch (Throwable t) {
                this.veil$programCache.remove(this.veil$activeBuffers);
                glDeleteProgram(this.programId);
                // Revert program state
                this.programId = oldProgram;
                Veil.LOGGER.error("Failed to recompile vanilla shader: {}", this.name, t);
            } finally {
                glDeleteShader(vertexShader);
                glDeleteShader(fragmentShader);
            }
            this.veil$vertexSource = null;
            this.veil$fragmentSource = null;
        }
    }

    @Unique
    private void veil$invalidate() {
        this.samplerLocations.clear();
        this.uniformLocations.clear();
        this.uniformMap.clear();
        for (Uniform uniform : this.uniforms) {
            uniform.setLocation(-1);
        }
        this.updateLocations();
        this.markDirty();
    }

    @Override
    public Collection<ResourceLocation> veil$getShaderSources() {
        // TODO probably extra code for iris/sodium needed
        ResourceLocation vertexProgramName = ResourceLocation.parse(this.vertexProgram.getName());
        ResourceLocation fragmentProgramName = ResourceLocation.parse(this.fragmentProgram.getName());
        ResourceLocation vertexPath = ResourceLocation.fromNamespaceAndPath(vertexProgramName.getNamespace(), "shaders/core/" + vertexProgramName.getPath() + Program.Type.VERTEX.getExtension());
        ResourceLocation fragmentPath = ResourceLocation.fromNamespaceAndPath(fragmentProgramName.getNamespace(), "shaders/core/" + fragmentProgramName.getPath() + Program.Type.FRAGMENT.getExtension());
        return List.of(vertexPath, fragmentPath);
    }

    @Override
    public boolean veil$swapBuffers(int activeBuffers) {
        if (this.veil$activeBuffers == activeBuffers) {
            return false;
        }

        // This makes sure shaders aren't recompiled multiple times
        this.veil$applyCompile();
        int programId = this.veil$programCache.get(activeBuffers);
        if (programId != 0) {
            this.veil$activeBuffers = activeBuffers;
            this.programId = programId;
            this.veil$invalidate();
            return false;
        }
        return true;
    }

    @Override
    public void veil$recompile(boolean vertex, String source, int activeBuffers) {
        if (this.veil$activeBuffers != activeBuffers) {
            this.veil$vertexSource = null;
            this.veil$fragmentSource = null;
        }

        this.veil$activeBuffers = activeBuffers;
        if (vertex) {
            this.veil$vertexSource = source;
        } else {
            this.veil$fragmentSource = source;
        }
    }
}
