package foundry.veil.mixin.client.shader;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.VertexFormat;
import foundry.veil.Veil;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.VeilRenderer;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import foundry.veil.impl.client.render.shader.modifier.ReplaceShaderModification;
import foundry.veil.impl.client.render.shader.modifier.ShaderModification;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Collection;
import java.util.List;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @WrapOperation(method = "reloadShaders", at = @At(value = "NEW", target = "(Lnet/minecraft/server/packs/resources/ResourceProvider;Ljava/lang/String;Lcom/mojang/blaze3d/vertex/VertexFormat;)Lnet/minecraft/client/renderer/ShaderInstance;"))
    private ShaderInstance veil$replaceShaders(ResourceProvider resourceProvider, String name, VertexFormat vertexFormat, Operation<ShaderInstance> original) {
        if (Veil.platform().hasErrors()) {
            return original.call(resourceProvider, name, vertexFormat);
        }

        ResourceLocation loc = ResourceLocation.parse(name);
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(loc.getNamespace(), "shaders/core/" + loc.getPath());

        VeilRenderer renderer = VeilRenderSystem.renderer();
        List<ShaderModification> modifiers = renderer.getShaderModificationManager().getModifiers(id);
        if (modifiers.size() == 1 && modifiers.getFirst() instanceof ReplaceShaderModification replaceModification) {
            ShaderProgram shader = renderer.getShaderManager().getShader(replaceModification.veilShader());
            if (shader != null) {
                return shader.toShaderInstance();
            }

            Veil.LOGGER.error("Failed to replace vanilla shader '{}' with veil shader: {}", loc, replaceModification.veilShader());
        }

        return original.call(resourceProvider, name, vertexFormat);
    }
}
