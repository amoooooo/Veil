package foundry.veil.fabric.mixin.compat.sodium;

import foundry.veil.Veil;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.impl.client.render.shader.SodiumShaderProcessor;
import net.caffeinemc.mods.sodium.client.gl.shader.GlShader;
import net.caffeinemc.mods.sodium.client.gl.shader.ShaderConstants;
import net.caffeinemc.mods.sodium.client.gl.shader.ShaderLoader;
import net.caffeinemc.mods.sodium.client.gl.shader.ShaderType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;

@Mixin(ShaderLoader.class)
public class ShaderLoaderMixin {

    @Unique
    private static int veil$shaderType;
    @Unique
    private static ResourceLocation veil$shaderName;

    @ModifyArg(method = "loadShader", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/gl/shader/GlShader;<init>(Lnet/caffeinemc/mods/sodium/client/gl/shader/ShaderType;Lnet/minecraft/resources/ResourceLocation;Ljava/lang/String;)V"), index = 2)
    private static String modifySource(String src) {
        try {
            SodiumShaderProcessor.setup(Minecraft.getInstance().getResourceManager(), VeilRenderSystem.renderer().getDynamicBufferManger().getActiveBuffers());
            return SodiumShaderProcessor.modify(ResourceLocation.fromNamespaceAndPath(veil$shaderName.getNamespace(), "shaders/" + veil$shaderName.getPath()), veil$shaderType, src);
        } catch (Exception e) {
            Veil.LOGGER.error("Failed to apply Veil shader modifiers to shader: {}", veil$shaderName, e);
            return src;
        } finally {
            SodiumShaderProcessor.free();
        }
    }

    @Inject(method = "loadShader", at = @At("HEAD"))
    private static void preLoadShader(ShaderType type, ResourceLocation name, ShaderConstants constants, CallbackInfoReturnable<GlShader> cir) throws IOException {
        veil$shaderType = type.id;
        veil$shaderName = name;
    }

    @Inject(method = "loadShader", at = @At("RETURN"))
    private static void postLoadShader(ShaderType type, ResourceLocation name, ShaderConstants constants, CallbackInfoReturnable<GlShader> cir) throws IOException {
        veil$shaderName = null;
    }
}
