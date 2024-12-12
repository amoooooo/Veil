package foundry.veil.mixin.client.dynamicbuffer;

import foundry.veil.Veil;
import foundry.veil.api.client.render.VeilRenderSystem;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Shadow
    @Final
    private Map<String, ShaderInstance> shaders;

    @Inject(method = "reloadShaders", at = @At("RETURN"))
    public void reloadShaders(ResourceProvider resourceProvider, CallbackInfo ci) {
        if (Veil.platform().hasErrors()) {
            return;
        }

        VeilRenderSystem.renderer().getVanillaShaderCompiler().reload(this.shaders.values());
    }
}
