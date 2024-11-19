package foundry.veil.mixin.client.shader;

import com.mojang.blaze3d.vertex.VertexFormat;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.VeilRenderer;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import foundry.veil.impl.client.render.shader.modifier.ReplaceShaderModification;
import foundry.veil.impl.client.render.shader.modifier.ShaderModification;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.IOException;
import java.util.Collection;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Shadow
    @Final
    static Logger LOGGER;

    /**
     * This is needed to replace the shader instance initializer when shader replacement is used
     *
     * @author Ocelot
     */
    @Redirect(method = "reloadShaders", at = @At(value = "NEW", target = "(Lnet/minecraft/server/packs/resources/ResourceProvider;Ljava/lang/String;Lcom/mojang/blaze3d/vertex/VertexFormat;)Lnet/minecraft/client/renderer/ShaderInstance;"))
    public ShaderInstance veil$replaceShaders(ResourceProvider resourceProvider, String name, VertexFormat vertexFormat) throws IOException {
        ResourceLocation loc = ResourceLocation.parse(name);
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(loc.getNamespace(), "shaders/core/" + loc.getPath());

        VeilRenderer renderer = VeilRenderSystem.renderer();
        Collection<ShaderModification> modifiers = renderer.getShaderModificationManager().getModifiers(id);
        if (modifiers.size() == 1) {
            ShaderModification modification = modifiers.iterator().next();
            if (modification instanceof ReplaceShaderModification replaceModification) {
                ShaderProgram shader = renderer.getShaderManager().getShader(replaceModification.veilShader());
                if (shader == null) {
                    LOGGER.error("Failed to replace vanilla shader '{}' with veil shader: {}", loc, replaceModification.veilShader());
                } else {
                    return shader.toShaderInstance();
                }
            }
        }

        return new ShaderInstance(resourceProvider, name, vertexFormat);
    }
}
