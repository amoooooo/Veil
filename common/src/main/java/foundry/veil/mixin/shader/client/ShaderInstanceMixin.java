package foundry.veil.mixin.shader.client;

import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.shaders.Program;
import com.mojang.blaze3d.shaders.Shader;
import com.mojang.blaze3d.shaders.Uniform;
import foundry.veil.Veil;
import foundry.veil.impl.client.render.shader.program.ShaderProgramImpl;
import foundry.veil.impl.client.render.shader.program.ShaderUniformCache;
import foundry.veil.impl.client.render.shader.transformer.VanillaShaderProcessor;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.IntBuffer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL20C.*;

@Mixin(value = ShaderInstance.class, priority = 800)
public abstract class ShaderInstanceMixin implements Shader {

    @Shadow
    @Final
    private int programId;
    @Shadow
    @Final
    private List<Integer> uniformLocations;
    @Shadow
    @Final
    public Map<String, Uniform> uniformMap;
    @Shadow
    @Final
    private String name;

    @Shadow
    @Final
    private List<String> samplerNames;

    @Unique
    private final Map<String, Uniform> veil$uniforms = new Object2ObjectArrayMap<>();

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
    private static void veil$clearFallbackProcessor(CallbackInfoReturnable<Program> cir) {
        if (Veil.platform().hasErrors()) {
            return;
        }
        VanillaShaderProcessor.free();
    }

    @Inject(method = "close", at = @At("HEAD"))
    public void close(CallbackInfo ci) {
        for (Uniform uniform : this.veil$uniforms.values()) {
            uniform.close();
        }
    }

    @Inject(method = "apply", at = @At("TAIL"))
    public void apply(CallbackInfo ci) {
        for (Uniform uniform : this.veil$uniforms.values()) {
            uniform.upload();
        }
    }

    @Inject(method = "updateLocations", at = @At("TAIL"))
    public void updateLocations(CallbackInfo ci) {
        if ((Object) this instanceof ShaderProgramImpl.Wrapper) {
            return;
        }

        for (Uniform uniform : this.veil$uniforms.values()) {
            uniform.setLocation(-1);
        }

        int uniformCount = glGetProgrami(this.programId, GL_ACTIVE_UNIFORMS);
        int maxUniformLength = glGetProgrami(this.programId, GL_ACTIVE_UNIFORM_MAX_LENGTH);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer size = stack.mallocInt(1);
            IntBuffer type = stack.mallocInt(1);
            for (int i = 0; i < uniformCount; i++) {
                String name = glGetActiveUniform(this.programId, i, maxUniformLength, size, type);

                if (this.uniformMap.containsKey(name) || this.samplerNames.contains(name)) {
                    continue;
                }

                int dataType = type.get(0);
                String typeName = ShaderUniformCache.getName(dataType);
                int length = size.get(0);
                if (ShaderUniformCache.isSampler(dataType)) {
                    for (int j = 0; j < length; j++) {
                        if (length > 1) {
                            name = name.substring(0, name.length() - 3) + '[' + j + ']';
                        }
                        Veil.LOGGER.debug("Shader {} detected sampler: {}", this.name, typeName + " " + name);
                        this.samplerNames.add(name);
                    }
                    continue;
                }

                int minecraftType;
                int minecraftCount;
                switch (dataType) {
                    case GL_INT -> {
                        minecraftType = 0;
                        minecraftCount = 1;
                    }
                    case GL_INT_VEC2 -> {
                        minecraftType = 1;
                        minecraftCount = 2;
                    }
                    case GL_INT_VEC3 -> {
                        minecraftType = 2;
                        minecraftCount = 3;
                    }
                    case GL_INT_VEC4 -> {
                        minecraftType = 3;
                        minecraftCount = 4;
                    }
                    case GL_FLOAT -> {
                        minecraftType = 4;
                        minecraftCount = 1;
                    }
                    case GL_FLOAT_VEC2 -> {
                        minecraftType = 5;
                        minecraftCount = 2;
                    }
                    case GL_FLOAT_VEC3 -> {
                        minecraftType = 6;
                        minecraftCount = 3;
                    }
                    case GL_FLOAT_VEC4 -> {
                        minecraftType = 7;
                        minecraftCount = 4;
                    }
                    case GL_FLOAT_MAT2 -> {
                        minecraftType = 8;
                        minecraftCount = 4;
                    }
                    case GL_FLOAT_MAT3 -> {
                        minecraftType = 9;
                        minecraftCount = 9;
                    }
                    case GL_FLOAT_MAT4 -> {
                        minecraftType = 10;
                        minecraftCount = 16;
                    }
                    default -> {
                        Veil.LOGGER.error("Unsupported Uniform Type: {}", typeName);
                        continue;
                    }
                }

                for (int j = 0; j < length; j++) {
                    if (length > 1) {
                        name = name.substring(0, name.indexOf('[')) + '[' + j + ']';
                    }

                    Veil.LOGGER.debug("Shader {} detected uniform: {}", this.name, typeName + " " + name);
                    Uniform old = this.veil$uniforms.get(name);
                    Uniform uniform;
                    if (old != null) {
                        if (old.getType() != minecraftType) {
                            old.close();
                            this.veil$uniforms.put(name, uniform = new Uniform(name, minecraftType, minecraftCount, this));
                        } else {
                            uniform = old;
                        }
                    } else {
                        this.veil$uniforms.put(name, uniform = new Uniform(name, minecraftType, minecraftCount, this));
                    }

                    int location = Uniform.glGetUniformLocation(this.programId, name);
                    if (location == -1) {
                        Veil.LOGGER.warn("Shader {} could not find uniform named {} in the specified shader program.", this.name, name);
                    } else {
                        this.uniformLocations.add(location);
                        uniform.setLocation(location);
                        this.uniformMap.put(name, uniform);
                    }
                }
            }
        }

        // Clean up invalid uniforms
        this.veil$uniforms.values().removeIf(uniform -> uniform.getLocation() == -1);
    }
}
