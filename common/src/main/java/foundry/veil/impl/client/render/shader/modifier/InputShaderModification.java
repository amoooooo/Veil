package foundry.veil.impl.client.render.shader.modifier;

import foundry.veil.impl.client.render.shader.transformer.VeilJobParameters;
import io.github.douira.glsl_transformer.ast.node.TranslationUnit;
import io.github.douira.glsl_transformer.ast.transform.ASTInjectionPoint;
import io.github.douira.glsl_transformer.ast.transform.ASTParser;
import org.jetbrains.annotations.ApiStatus;

import java.io.IOException;

@ApiStatus.Internal
public class InputShaderModification implements ShaderModification {

    private final int priority;
    private final String input;

    public InputShaderModification(int priority, String input) {
        this.priority = priority;
        this.input = input;
    }

    @Override
    public void inject(ASTParser parser, TranslationUnit tree, VeilJobParameters parameters) throws IOException {
        tree.parseAndInjectNodes(parser, ASTInjectionPoint.BEFORE_DECLARATIONS, this.input.split("\n"));
    }

    @Override
    public int priority() {
        return this.priority;
    }
}
