package foundry.veil.mixin.client.shader;

import com.mojang.blaze3d.shaders.EffectProgram;
import com.mojang.blaze3d.shaders.Program;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.resource.VeilResource;
import foundry.veil.impl.client.render.shader.SimpleShaderProcessor;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collections;

@Mixin(EffectInstance.class)
public class EffectInstanceMixin {

    @Inject(method = "getOrCreate", at = @At("HEAD"))
    private static void veil$setupFallbackProcessor(ResourceProvider pResourceProvider, Program.Type pType, String pName, CallbackInfoReturnable<EffectProgram> cir) {
        SimpleShaderProcessor.setup(pResourceProvider, VeilRenderSystem.renderer().getDynamicBufferManger().getActiveBuffers(), Collections.emptySet());
    }

    @Inject(method = "getOrCreate", at = @At("RETURN"))
    private static void veil$clearFallbackProcessor(ResourceProvider pResourceProvider, Program.Type pType, String pName, CallbackInfoReturnable<EffectProgram> cir) {
        SimpleShaderProcessor.free();
    }
}
