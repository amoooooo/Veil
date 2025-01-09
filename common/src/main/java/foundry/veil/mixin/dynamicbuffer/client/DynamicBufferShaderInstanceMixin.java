package foundry.veil.mixin.dynamicbuffer.client;

import com.mojang.blaze3d.shaders.Program;
import com.mojang.blaze3d.shaders.Shader;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.VertexFormat;
import foundry.veil.Veil;
import foundry.veil.ext.ShaderInstanceExtension;
import foundry.veil.impl.client.render.dynamicbuffer.VanillaShaderCompiler;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL20C.*;

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

    @Unique
    private String veil$vertexSource;
    @Unique
    private String veil$fragmentSource;
    @Unique
    private int veil$activeBuffers;
    @Unique
    private final Int2IntMap veil$programCache = new Int2IntArrayMap(1);

    @Inject(method = "<init>", at = @At("TAIL"))
    public void init(ResourceProvider resourceProvider, String name, VertexFormat vertexFormat, CallbackInfo ci) {
        this.veil$programCache.put(0, this.programId);
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
            int programId = glCreateProgram();
            int vertexShader = glCreateShader(GL_VERTEX_SHADER);
            int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);

            try {
                glShaderSource(vertexShader, this.veil$vertexSource);
                glCompileShader(vertexShader);
                if (glGetShaderi(vertexShader, GL_COMPILE_STATUS) != GL_TRUE) {
                    String error = StringUtils.trim(glGetShaderInfoLog(vertexShader));
                    throw new IOException("Couldn't compile dynamic vertex program (" + this.vertexProgram.getName() + ", " + this.name + ") : " + error);
                }

                glShaderSource(fragmentShader, this.veil$fragmentSource);
                glCompileShader(fragmentShader);
                if (glGetShaderi(fragmentShader, GL_COMPILE_STATUS) != GL_TRUE) {
                    String error = StringUtils.trim(glGetShaderInfoLog(fragmentShader));
                    throw new IOException("Couldn't compile dynamic fragment program (" + this.fragmentProgram.getName() + ", " + this.name + ") : " + error);
                }

                glAttachShader(programId, vertexShader);
                glAttachShader(programId, fragmentShader);

                int index = 0;
                for (String name : this.vertexFormat.getElementAttributeNames()) {
                    glBindAttribLocation(programId, index, name);
                    index++;
                }

                glLinkProgram(programId);
                if (glGetProgrami(programId, GL_LINK_STATUS) != GL_TRUE) {
                    String error = StringUtils.trim(glGetProgramInfoLog(programId));
                    throw new IOException("Couldn't link shader (" + this.name + ") : " + error);
                }

                this.programId = programId;
                this.veil$invalidate();

                // This is a *bit* of a waste, but it's more compatible than trying to attach new shaders to the program
                int old = this.veil$programCache.put(this.veil$activeBuffers, programId);
                if (old != 0) {
                    glDeleteProgram(old);
                }
            } catch (Throwable t) {
                this.veil$programCache.remove(this.veil$activeBuffers);
                glDeleteProgram(programId);
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
        this.uniformLocations.clear();
        this.samplerLocations.clear();
        this.uniformMap.clear();
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
