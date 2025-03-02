package foundry.veil.api.client.render.shader.processor;

import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.ShaderModificationManager;
import foundry.veil.impl.client.render.shader.modifier.VeilJobParameters;
import io.github.ocelot.glslprocessor.api.GlslSyntaxException;
import io.github.ocelot.glslprocessor.api.node.GlslTree;
import io.github.ocelot.glslprocessor.lib.anarres.cpp.LexerException;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Modifies shader sources with the shader modification system.
 *
 * @author Ocelot
 */
public class ShaderModifyProcessor implements ShaderPreProcessor {

    private final ShaderModificationManager shaderModificationManager;
    private final Set<ResourceLocation> appliedModifications;

    public ShaderModifyProcessor() {
        this.shaderModificationManager = VeilRenderSystem.renderer().getShaderModificationManager();
        this.appliedModifications = new HashSet<>();
    }

    @Override
    public void prepare() {
        this.appliedModifications.clear();
    }

    @Override
    public void modify(Context ctx, GlslTree tree) throws IOException, GlslSyntaxException, LexerException {
        ResourceLocation name = ctx.name();
        if (name == null || !this.appliedModifications.add(name)) {
            return;
        }
        int flags = ctx.isSourceFile() ? VeilJobParameters.APPLY_VERSION | VeilJobParameters.ALLOW_OUT : 0;
        for (ResourceLocation include : ctx.shaderImporter().addedImports()) { // Run include modifiers first
            this.shaderModificationManager.applyModifiers(include, tree, flags);
        }
        this.shaderModificationManager.applyModifiers(name, tree, flags);
    }
}
