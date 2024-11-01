package foundry.veil.impl.client.render.shader.modifier;

import foundry.veil.impl.client.render.shader.transformer.VeilJobParameters;
import io.github.douira.glsl_transformer.ast.data.ChildNodeList;
import io.github.douira.glsl_transformer.ast.node.TranslationUnit;
import io.github.douira.glsl_transformer.ast.node.Version;
import io.github.douira.glsl_transformer.ast.node.VersionStatement;
import io.github.douira.glsl_transformer.ast.node.external_declaration.FunctionDefinition;
import io.github.douira.glsl_transformer.ast.node.statement.Statement;
import io.github.douira.glsl_transformer.ast.query.Root;
import io.github.douira.glsl_transformer.ast.transform.ASTInjectionPoint;
import io.github.douira.glsl_transformer.ast.transform.ASTParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
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
    public void inject(ASTParser parser, TranslationUnit tree, VeilJobParameters parameters) throws IOException {
        if (parameters.applyVersion()) {
            tree.ensureVersionStatement();
            VersionStatement statement = tree.getVersionStatement();
            if (statement.version.number < this.version) {
                statement.version = Version.fromNumber(this.version);
            }
        }

        String[] includes = new String[this.includes.length];
        for (int i = 0; i < this.includes.length; i++) {
            includes[i] = "#custom veil:include " + this.includes[i] + "\n";
        }
        tree.parseAndInjectNodes(parser, ASTInjectionPoint.BEFORE_DECLARATIONS, includes);

        if (!StringUtil.isNullOrEmpty(this.uniform)) {
            tree.parseAndInjectNodes(parser, ASTInjectionPoint.BEFORE_DECLARATIONS, this.fillPlaceholders(this.uniform).split("\n"));
        }

        if (!StringUtil.isNullOrEmpty(this.output)) {
            tree.parseAndInjectNodes(parser, ASTInjectionPoint.BEFORE_DECLARATIONS, this.fillPlaceholders(this.output).split("\n"));
        }

        Root root = tree.getRoot();
        for (Function function : this.functions) {
            String name = function.name();
            ChildNodeList<Statement> statements = root.identifierIndex.getStream(name)
                    .map(id -> id.getBranchAncestor(FunctionDefinition.class, FunctionDefinition::getFunctionPrototype))
                    .filter(definition -> {
                        if (definition == null) {
                            return false;
                        }

                        int paramCount = function.parameters();
                        if (paramCount == -1) {
                            return true;
                        }
                        return definition.getFunctionPrototype().getParameters().size() == paramCount;
                    })
                    .findFirst()
                    .map(FunctionDefinition::getBody).orElseThrow(() -> {
                        int paramCount = function.parameters();
                        if (paramCount == -1) {
                            return new IOException("Unknown function: " + name);
                        }
                        return new IOException("Unknown function with " + paramCount + " parameters: " + name);
                    }).getStatements();

            Statement statement = parser.parseStatement(root, this.fillPlaceholders("{" + function.code() + "}"));
            if (function.head()) {
                statements.add(0, statement);
            } else {
                statements.add(statement);
            }
        }
    }

    public String fillPlaceholders(String code) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(code);
        if (!matcher.find()) {
            return code;
        }

        StringBuilder sb = new StringBuilder();
        matcher.appendReplacement(sb, this.getPlaceholder(matcher.group(1)));
        while (matcher.find()) {
            matcher.appendReplacement(sb, this.getPlaceholder(matcher.group(1)));
        }
        matcher.appendTail(sb);
        return sb.toString();
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
