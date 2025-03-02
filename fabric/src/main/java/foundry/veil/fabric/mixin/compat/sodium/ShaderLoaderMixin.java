package foundry.veil.fabric.mixin.compat.sodium;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import foundry.veil.Veil;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.impl.client.render.shader.processor.SodiumShaderProcessor;
import net.caffeinemc.mods.sodium.client.gl.shader.GlShader;
import net.caffeinemc.mods.sodium.client.gl.shader.ShaderConstants;
import net.caffeinemc.mods.sodium.client.gl.shader.ShaderLoader;
import net.caffeinemc.mods.sodium.client.gl.shader.ShaderType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShaderLoader.class)
public class ShaderLoaderMixin {

    @ModifyArg(method = "loadShader", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/gl/shader/GlShader;<init>(Lnet/caffeinemc/mods/sodium/client/gl/shader/ShaderType;Lnet/minecraft/resources/ResourceLocation;Ljava/lang/String;)V"), index = 2)
    private static String modifySource(String src, @Share("shaderType") LocalIntRef shaderTypeRef, @Share("shaderName") LocalRef<ResourceLocation> shaderNameRef) {
        int shaderType = shaderTypeRef.get();
        ResourceLocation shaderName = shaderNameRef.get();

        try {
            int activeBuffers = VeilRenderSystem.renderer().getDynamicBufferManger().getActiveBuffers();
            SodiumShaderProcessor.setup(Minecraft.getInstance().getResourceManager());
            return SodiumShaderProcessor.modify(shaderName.withPrefix("shaders/"), activeBuffers, shaderType, src);
        } catch (Exception e) {
            Veil.LOGGER.error("Failed to apply Veil shader modifiers to shader: {}", shaderName, e);
            return src;
        } finally {
            SodiumShaderProcessor.free();
        }
    }

    @Inject(method = "loadShader", at = @At("HEAD"))
    private static void preLoadShader(ShaderType type, ResourceLocation name, ShaderConstants constants, CallbackInfoReturnable<GlShader> cir, @Share("shaderType") LocalIntRef shaderType, @Share("shaderName") LocalRef<ResourceLocation> shaderName) {
        shaderType.set(type.id);
        shaderName.set(name);
    }
}
