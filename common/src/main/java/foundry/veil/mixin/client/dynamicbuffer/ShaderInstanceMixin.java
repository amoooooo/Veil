package foundry.veil.mixin.client.dynamicbuffer;

import com.mojang.blaze3d.shaders.Program;
import com.mojang.blaze3d.shaders.Shader;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.VertexFormat;
import foundry.veil.Veil;
import foundry.veil.ext.ShaderInstanceExtension;
import foundry.veil.impl.client.render.dynamicbuffer.VanillaShaderCompiler;
import foundry.veil.mixin.accessor.ProgramAccessor;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
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

    @Shadow
    @Final
    private Program vertexProgram;

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

    @Inject(method = "apply", at = @At("HEAD"))
    public void apply(CallbackInfo ci) {
        VanillaShaderCompiler.markRendered(this.name);
        if (this.veil$vertexSource != null && this.veil$fragmentSource != null) {
            try {
                ProgramAccessor vertexAccessor = (ProgramAccessor) this.vertexProgram;
                ProgramAccessor fragmentAccessor = (ProgramAccessor) this.fragmentProgram;
                int vertexShader = vertexAccessor.getId();
                int fragmentShader = fragmentAccessor.getId();

                glShaderSource(vertexShader, this.veil$vertexSource);
                glCompileShader(vertexShader);
                if (glGetShaderi(vertexShader, GL_COMPILE_STATUS) != GL_TRUE) {
                    String error = StringUtils.trim(glGetShaderInfoLog(vertexShader));
                    throw new IOException("Couldn't compile vertex program (" + this.vertexProgram.getName() + ", " + this.name + ") : " + error);
                }

                glShaderSource(fragmentShader, this.veil$fragmentSource);
                glCompileShader(fragmentShader);
                if (glGetShaderi(fragmentShader, GL_COMPILE_STATUS) != GL_TRUE) {
                    String error = StringUtils.trim(glGetShaderInfoLog(fragmentShader));
                    throw new IOException("Couldn't compile fragment program (" + this.fragmentProgram.getName() + ", " + this.name + ") : " + error);
                }

                int i = 0;
                for (String name : this.vertexFormat.getElementAttributeNames()) {
                    glBindAttribLocation(this.programId, i, name);
                    i++;
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
            } catch (Throwable t) {
                Veil.LOGGER.error("Failed to recompile vanilla shader: {}", this.name, t);
            }
            this.veil$vertexSource = null;
            this.veil$fragmentSource = null;
        }
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
