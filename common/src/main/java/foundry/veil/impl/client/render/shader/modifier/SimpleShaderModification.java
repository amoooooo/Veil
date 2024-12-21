package foundry.veil.impl.client.render.shader.modifier;

import foundry.veil.impl.client.render.shader.transformer.VeilJobParameters;
import foundry.veil.impl.glsl.GlslParser;
import foundry.veil.impl.glsl.GlslSyntaxException;
import foundry.veil.impl.glsl.grammar.GlslVersionStatement;
import foundry.veil.impl.glsl.node.GlslNode;
import foundry.veil.impl.glsl.node.GlslTree;
import foundry.veil.impl.glsl.node.function.GlslFunctionNode;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;

@ApiStatus.Internal
public class SimpleShaderModification implements ShaderModification {

    private final int version;
    private final int priority;
    private final ResourceLocation[] includes;
    private final String output;
    private final String uniform;
    private final Function[] functions;

    public SimpleShaderModification(int version, int priority, ResourceLocation[] includes, @Nullable String output, @Nullable String uniform, Function[] functions) {
        this.version = version;
        this.priority = priority;
        this.includes = includes;
        this.output = output;
        this.uniform = uniform;
        this.functions = functions;
    }

    @Override
    public void inject(GlslTree tree, VeilJobParameters parameters) throws GlslSyntaxException, IOException {
        if (parameters.applyVersion()) {
            GlslVersionStatement version = tree.getVersionStatement();
            if (version.getVersion() < this.version) {
                version.setVersion(this.version);
            }
        }

        List<String> directives = tree.getDirectives();
        for (ResourceLocation include : this.includes) {
            directives.add("#include " + include);
        }

        if (this.output != null && !this.output.isEmpty()) {
            tree.getBody().addAll(0, GlslParser.parse(this.fillPlaceholders(this.output)).getBody());
        }

        if (this.uniform != null && !this.uniform.isEmpty()) {
            tree.getBody().addAll(0, GlslParser.parse(this.fillPlaceholders(this.uniform)).getBody());
        }

        for (Function function : this.functions) {
            String name = function.name();
            List<GlslNode> body = tree.functions().filter(definition -> {
                        if (definition == null) {
                            return false;
                        }

                        if (definition.getBody() == null) {
                            return false;
                        }

                        int paramCount = function.parameters();
                        if (paramCount == -1) {
                            return true;
                        }
                        return definition.getHeader().getParameters().size() == paramCount;
                    })
                    .findFirst()
                    .map(GlslFunctionNode::getBody)
                    .orElseThrow(() -> {
                        int paramCount = function.parameters();
                        if (paramCount == -1) {
                            return new IOException("Unknown function: " + name);
                        }
                        return new IOException("Unknown function with " + paramCount + " parameters: " + name);
                    });

            GlslNode insert = GlslNode.compound(GlslParser.parseExpressionList(this.fillPlaceholders(function.code())));
            if (function.head()) {
                body.addFirst(insert);
            } else {
                body.add(insert);
            }
        }
    }

    public String fillPlaceholders(String code) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(code);
        if (!matcher.find()) {
            return code;
        }

        StringBuilder builder = new StringBuilder();
        do {
            matcher.appendReplacement(builder, this.getPlaceholder(matcher.group(1)));
        } while (matcher.find());
        matcher.appendTail(builder);
        return builder.toString();
    }

    protected String getPlaceholder(String key) {
        return key;
    }

    @Override
    public int priority() {
        return this.priority;
    }

    public String getOutput() {
        return this.output;
    }
}
