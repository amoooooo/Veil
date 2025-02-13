package foundry.veil.impl.client.render.shader.modifier;

import foundry.veil.impl.client.render.shader.transformer.VeilJobParameters;
import io.github.ocelot.glslprocessor.api.GlslSyntaxException;
import io.github.ocelot.glslprocessor.api.node.GlslTree;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public record ReplaceShaderModification(int priority, ResourceLocation veilShader) implements ShaderModification {

    @Override
    public void inject(GlslTree tree, VeilJobParameters parameters) throws GlslSyntaxException {
        throw new UnsupportedOperationException("Replace modification replaces file");
    }
}
