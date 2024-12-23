package foundry.veil.mixin.client.pipeline;

import com.mojang.blaze3d.systems.RenderSystem;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.impl.client.render.shader.program.ShaderProgramImpl;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(RenderSystem.class)
public class RenderSystemMixin {

    @Inject(method = "setShader", at = @At("TAIL"), remap = false)
    private static void setShader(Supplier<ShaderInstance> shader, CallbackInfo ci) {
        if (!(shader.get() instanceof ShaderProgramImpl.Wrapper)) {
            VeilRenderSystem.shaderUpdate();
        }
    }

    @Inject(method = "setShaderLights", at = @At("HEAD"), remap = false)
    private static void setShaderLights(Vector3f light0, Vector3f light1, CallbackInfo ci) {
        VeilRenderSystem.setShaderLights(light0, light1);
    }
}
