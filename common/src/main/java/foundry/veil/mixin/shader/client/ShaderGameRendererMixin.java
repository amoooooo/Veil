package foundry.veil.mixin.shader.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.util.Pair;
import foundry.veil.Veil;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.VeilRenderer;
import foundry.veil.api.client.render.shader.ShaderModificationManager;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import foundry.veil.impl.client.render.shader.modifier.ReplaceShaderModification;
import foundry.veil.impl.client.render.shader.modifier.ShaderModification;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Consumer;

@Mixin(GameRenderer.class)
public class ShaderGameRendererMixin {

    @Inject(method = "reloadShaders", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;shutdownShaders()V"))
    public void replaceShaders(CallbackInfo ci, @Local(ordinal = 1) List<Pair<ShaderInstance, Consumer<ShaderInstance>>> loadedShaders) {
        if (Veil.platform().hasErrors()) {
            return;
        }

        VeilRenderer renderer = VeilRenderSystem.renderer();
        ShaderModificationManager modificationManager = renderer.getShaderModificationManager();
        for (Pair<ShaderInstance, Consumer<ShaderInstance>> pair : loadedShaders) {
            ResourceLocation loc = ResourceLocation.tryParse(pair.getFirst().getName());
            if (loc == null) {
                Veil.LOGGER.error("Failed to replace vanilla shader '{}' with veil shader: Malformed name", pair.getFirst().getName());
                continue;
            }

            List<ShaderModification> modifiers = modificationManager.getModifiers(loc.withPrefix("shaders/core/"));
            if (modifiers.size() == 1 && modifiers.getFirst() instanceof ReplaceShaderModification replaceModification) {
                ShaderProgram shader = renderer.getShaderManager().getShader(replaceModification.veilShader());
                if (shader != null) {
                    pair.getSecond().accept(shader.toShaderInstance());
                    continue;
                }

                Veil.LOGGER.error("Failed to replace vanilla shader '{}' with veil shader: {}", loc, replaceModification.veilShader());
            }
        }
    }
}
