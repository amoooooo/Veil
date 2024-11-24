package foundry.veil.mixin.client.dynamicbuffer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.Program;
import com.mojang.blaze3d.shaders.ProgramManager;
import com.mojang.blaze3d.shaders.Shader;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.VertexFormat;
import foundry.veil.Veil;
import foundry.veil.ext.ShaderInstanceExtension;
import foundry.veil.impl.client.render.shader.SimpleShaderProcessor;
import foundry.veil.mixin.accessor.ProgramAccessor;
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
public abstract class ShaderInstanceMixin implements Shader, ShaderInstanceExtension {

    @Shadow
    private static Program getOrCreate(ResourceProvider resourceProvider, Program.Type programType, String name) throws IOException {
        return null;
    }

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

    @Inject(method = "apply", at = @At("HEAD"))
    public void apply(CallbackInfo ci) {
        SimpleShaderProcessor.markRendered(this.name);
        if (this.veil$vertexSource != null && this.veil$fragmentSource != null) {
            try {
                ProgramAccessor vertexAccessor = (ProgramAccessor) this.vertexProgram;
                ProgramAccessor fragmentAccessor = (ProgramAccessor) this.fragmentProgram;

                glDetachShader(this.programId, vertexAccessor.getId());
                glDetachShader(this.programId, fragmentAccessor.getId());

                GlStateManager.glShaderSource(vertexAccessor.getId(), List.of(this.veil$vertexSource));
                GlStateManager.glCompileShader(vertexAccessor.getId());
                if (GlStateManager.glGetShaderi(vertexAccessor.getId(), GL_COMPILE_STATUS) == 0) {
                    String error = StringUtils.trim(glGetShaderInfoLog(vertexAccessor.getId()));
                    throw new IOException("Couldn't compile vertex program (" + this.vertexProgram.getName() + ", " + this.name + ") : " + error);
                }

                GlStateManager.glShaderSource(fragmentAccessor.getId(), List.of(this.veil$fragmentSource));
                GlStateManager.glCompileShader(fragmentAccessor.getId());
                if (GlStateManager.glGetShaderi(fragmentAccessor.getId(), GL_COMPILE_STATUS) == 0) {
                    String error = StringUtils.trim(glGetShaderInfoLog(fragmentAccessor.getId()));
                    throw new IOException("Couldn't compile fragment program (" + this.fragmentProgram.getName() + ", " + this.name + ") : " + error);
                }

                int i = 0;
                for (String name : this.vertexFormat.getElementAttributeNames()) {
                    Uniform.glBindAttribLocation(this.programId, i, name);
                    i++;
                }

                this.uniformLocations.clear();
                this.samplerLocations.clear();
                this.uniformMap.clear();

                // Force re-link
                ProgramManager.linkShader(this);
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
    public void veil$recompile(boolean vertex, String source) {
        if (vertex) {
            this.veil$vertexSource = source;
        } else {
            this.veil$fragmentSource = source;
        }
    }
}
