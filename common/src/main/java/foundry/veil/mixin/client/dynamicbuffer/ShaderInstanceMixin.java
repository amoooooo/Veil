package foundry.veil.mixin.client.dynamicbuffer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.Program;
import com.mojang.blaze3d.shaders.Shader;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.VertexFormat;
import foundry.veil.Veil;
import foundry.veil.ext.ShaderInstanceExtension;
import foundry.veil.impl.client.render.dynamicbuffer.VanillaShaderCompiler;
import foundry.veil.mixin.accessor.ProgramAccessor;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
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
public abstract class ShaderInstanceMixin implements Shader, ShaderInstanceExtension {

    @Mutable
    @Shadow
    @Final
    private Program vertexProgram;

    @Mutable
    @Shadow
    @Final
    private Program fragmentProgram;

    @Shadow
    @Final
    private int programId;

    @Shadow
    @Final
    private VertexFormat vertexFormat;

    @Shadow
    protected abstract void updateLocations();

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
    @Unique
    private String veil$vertexSource;
    @Unique
    private String veil$fragmentSource;
    @Unique
    private int veil$activeBuffers;
    @Unique
    private final Int2ObjectMap<Program> veil$programCache = new Int2ObjectArrayMap<>(2);

    @Inject(method = "apply", at = @At("HEAD"))
    public void apply(CallbackInfo ci) {
        VanillaShaderCompiler.markRendered(this.name);
        this.veil$applyCompile();
    }

    @Inject(method = "close", at = @At("HEAD"))
    public void close(CallbackInfo ci) {
        if (this.veil$programCache.isEmpty()) {
            return;
        }

        // Swap out the shaders before vanilla mc tries to delete them
        this.vertexProgram = this.veil$programCache.remove(0);
        this.fragmentProgram = this.veil$programCache.remove(1);
        // Delete all extra shaders created by veil
        for (Program program : this.veil$programCache.values()) {
            glDeleteShader(((ProgramAccessor) program).getId());
        }
        this.veil$programCache.clear();
    }

    @Unique
    private void veil$applyCompile() {
        if (this.veil$vertexSource != null && this.veil$fragmentSource != null) {
            try {
                Program vertexProgram = this.veil$programCache.computeIfAbsent(this.veil$activeBuffers << 1, unused -> new Program(Program.Type.VERTEX, GlStateManager.glCreateShader(GL_VERTEX_SHADER), this.vertexProgram.getName()));
                Program fragmentProgram = this.veil$programCache.computeIfAbsent((this.veil$activeBuffers << 1) + 1, unused -> new Program(Program.Type.FRAGMENT, GlStateManager.glCreateShader(GL_FRAGMENT_SHADER), this.fragmentProgram.getName()));

                ProgramAccessor vertexAccessor = (ProgramAccessor) vertexProgram;
                ProgramAccessor fragmentAccessor = (ProgramAccessor) fragmentProgram;
                int vertexShader = vertexAccessor.getId();
                int fragmentShader = fragmentAccessor.getId();

                glShaderSource(vertexShader, this.veil$vertexSource);
                glCompileShader(vertexShader);
                if (glGetShaderi(vertexShader, GL_COMPILE_STATUS) != GL_TRUE) {
                    String error = StringUtils.trim(glGetShaderInfoLog(vertexShader));
                    throw new IOException("Couldn't compile vertex program (" + vertexProgram.getName() + ", " + this.name + ") : " + error);
                }

                glShaderSource(fragmentShader, this.veil$fragmentSource);
                glCompileShader(fragmentShader);
                if (glGetShaderi(fragmentShader, GL_COMPILE_STATUS) != GL_TRUE) {
                    String error = StringUtils.trim(glGetShaderInfoLog(fragmentShader));
                    throw new IOException("Couldn't compile fragment program (" + fragmentProgram.getName() + ", " + this.name + ") : " + error);
                }

                this.veil$link(vertexProgram, fragmentProgram);
            } catch (Throwable t) {
                Veil.LOGGER.error("Failed to recompile vanilla shader: {}", this.name, t);
            }
            this.veil$vertexSource = null;
            this.veil$fragmentSource = null;
        }
    }

    @Unique
    private void veil$link(Program vertexProgram, Program fragmentProgram) throws IOException {
        glDetachShader(this.programId, ((ProgramAccessor) this.vertexProgram).getId());
        glDetachShader(this.programId, ((ProgramAccessor) this.fragmentProgram).getId());
        this.vertexProgram = vertexProgram;
        this.fragmentProgram = fragmentProgram;
        vertexProgram.attachToShader(this);
        fragmentProgram.attachToShader(this);

        int index = 0;
        for (String name : this.vertexFormat.getElementAttributeNames()) {
            glBindAttribLocation(this.programId, index, name);
            index++;
        }

        glLinkProgram(this.programId);
        if (glGetProgrami(this.programId, GL_LINK_STATUS) != GL_TRUE) {
            String error = StringUtils.trim(glGetProgramInfoLog(this.programId));
            throw new IOException("Couldn't link shader (" + this.name + ") : " + error);
        }

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
        Program vertexProgram = this.veil$programCache.get(activeBuffers << 1);
        Program fragmentProgram = this.veil$programCache.get((activeBuffers << 1) + 1);
        if (vertexProgram != null && fragmentProgram != null) {
            this.veil$activeBuffers = activeBuffers;
            try {
                this.veil$link(vertexProgram, fragmentProgram);
            } catch (Throwable t) {
                throw new RuntimeException("Failed to swap vanilla shader: " + this.name, t);
            }
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

        if (this.veil$activeBuffers == 0) {
            this.veil$programCache.put(0, this.vertexProgram);
            this.veil$programCache.put(1, this.fragmentProgram);
        }
        this.veil$activeBuffers = activeBuffers;
        if (vertex) {
            this.veil$vertexSource = source;
        } else {
            this.veil$fragmentSource = source;
        }
    }
}
