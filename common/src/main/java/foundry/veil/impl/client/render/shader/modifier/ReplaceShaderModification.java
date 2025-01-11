package foundry.veil.impl.client.render.shader.modifier;

import foundry.veil.api.glsl.GlslSyntaxException;
import foundry.veil.api.glsl.node.GlslTree;
import foundry.veil.impl.client.render.shader.transformer.VeilJobParameters;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public record ReplaceShaderModification(int priority, ResourceLocation veilShader) implements ShaderModification {

    @Override
    public void inject(GlslTree tree, VeilJobParameters parameters) throws GlslSyntaxException {
        throw new UnsupportedOperationException("Replace modification replaces file");
    }
}
