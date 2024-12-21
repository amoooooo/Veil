package foundry.veil.mixin.client.shader;

import com.mojang.blaze3d.shaders.Program;
import com.mojang.blaze3d.shaders.Uniform;
import foundry.veil.Veil;
import foundry.veil.impl.client.render.shader.ShaderProgramImpl;
import foundry.veil.impl.client.render.shader.transformer.VanillaShaderProcessor;
import foundry.veil.impl.client.render.wrapper.VanillaUniformWrapper;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.lwjgl.opengl.GL20C.glGetUniformLocation;

@Mixin(ShaderInstance.class)
public class ShaderInstanceMixin {

    @Shadow
    @Final
    private int programId;
    @Shadow
    @Final
    private List<Integer> uniformLocations;
    @Shadow
    @Final
    public Map<String, Uniform> uniformMap;

    @Unique
    private final Set<String> veil$invalidUniforms = new HashSet<>();

    @Inject(method = "getOrCreate", at = @At("HEAD"), cancellable = true)
    private static void veil$cancelDummyProgram(ResourceProvider provider, Program.Type type, String name, CallbackInfoReturnable<Program> cir) {
        if (ShaderProgramImpl.Wrapper.constructingProgram != null) {
            cir.setReturnValue(new ShaderProgramImpl.ShaderWrapper(type, ShaderProgramImpl.Wrapper.constructingProgram));
        }
    }

    @Inject(method = "getOrCreate", at = @At("HEAD"))
    private static void veil$setupFallbackProcessor(ResourceProvider provider, Program.Type type, String name, CallbackInfoReturnable<Program> cir) {
        if (Veil.platform().hasErrors()) {
            return;
        }
        VanillaShaderProcessor.setup(provider);
    }

    @Inject(method = "getOrCreate", at = @At("RETURN"))
    private static void veil$clearFallbackProcessor(ResourceProvider provider, Program.Type type, String name, CallbackInfoReturnable<Program> cir) {
        if (Veil.platform().hasErrors()) {
            return;
        }
        VanillaShaderProcessor.free();
    }

    // Allows users to request uniforms that aren't "registered"
    @Inject(method = "getUniform", at = @At("RETURN"), cancellable = true)
    public void getUniform(String name, CallbackInfoReturnable<Uniform> cir) {
        if (cir.getReturnValue() == null && !this.veil$invalidUniforms.contains(name)) {
            int location = glGetUniformLocation(this.programId, name);
            if (location == -1) {
                this.veil$invalidUniforms.add(name);
                return;
            }

            Uniform uniform = new VanillaUniformWrapper(this.programId, name);
            this.uniformLocations.add(location);
            uniform.setLocation(location);
            this.uniformMap.put(name, uniform);
            cir.setReturnValue(uniform);
        }
    }

    @Inject(method = "updateLocations", at = @At("TAIL"))
    public void updateLocations(CallbackInfo ci) {
        this.veil$invalidUniforms.clear();
    }
}
