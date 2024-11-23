package foundry.veil.impl.client.render.dynamicbuffer;

import foundry.veil.Veil;
import foundry.veil.api.client.render.dynamicbuffer.DynamicBufferType;
import foundry.veil.api.client.render.shader.processor.ShaderPreProcessor;
import foundry.veil.impl.glsl.GlslInjectionPoint;
import foundry.veil.impl.glsl.GlslParser;
import foundry.veil.impl.glsl.GlslSyntaxException;
import foundry.veil.impl.glsl.grammar.GlslSpecifiedType;
import foundry.veil.impl.glsl.grammar.GlslTypeQualifier;
import foundry.veil.impl.glsl.node.GlslNode;
import foundry.veil.impl.glsl.node.GlslTree;
import foundry.veil.impl.glsl.node.function.GlslFunctionNode;
import foundry.veil.impl.glsl.node.variable.GlslNewNode;
import foundry.veil.impl.glsl.visitor.GlslStringWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL20C.GL_FRAGMENT_SHADER;

public class DynamicBufferProcessor implements ShaderPreProcessor {

    private final DynamicBufferType[] types;

    public DynamicBufferProcessor(DynamicBufferType[] types) {
        this.types = types;
    }

    @Override
    public String modify(Context ctx, String source) throws IOException {
        try {
            GlslTree tree = GlslParser.parse(source);
            Map<String, GlslNode> markers = tree.getMarkers();
            GlslFunctionNode mainFunction = tree.mainFunction().orElseThrow();

            boolean modified = false;
            for (int i = 0; i < this.types.length; i++) {
                DynamicBufferType type = this.types[i];
                GlslNode node = markers.get("veil:" + type.getName());
                if (node != null && ctx.type() == GL_FRAGMENT_SHADER) {
                    modified = true;

                    String fieldName = type.getSourceName();
                    tree.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("layout(location = " + (1 + i) + ") out " + type.getType() + " " + fieldName));
                    if (node instanceof GlslNewNode newNode) {
                        mainFunction.getBody().add(GlslParser.parseExpression(fieldName + " = " + newNode.getName()));
                    }
                }
            }

            if (modified) {
                List<GlslNewNode> outputs = new ArrayList<>();
                tree.fields().forEach(node -> {
                    GlslSpecifiedType type = node.getType();
                    boolean valid = false;
                    for (GlslTypeQualifier qualifier : type.getQualifiers()) {
                        if (qualifier == GlslTypeQualifier.StorageType.OUT) {
                            valid = true;
                            break;
                        }
                    }

                    if (!valid) {
                        return;
                    }

                    for (GlslTypeQualifier qualifier : type.getQualifiers()) {
                        if (qualifier instanceof GlslTypeQualifier.Layout(List<GlslTypeQualifier.LayoutId> layoutIds)) {
                            for (GlslTypeQualifier.LayoutId layoutId : layoutIds) {
                                if ("location".equals(layoutId.identifier())) {
                                    GlslNode expression = layoutId.expression();
                                    if (expression != null) {
                                        try {
                                            int location = Integer.parseInt(expression.getSourceString());
                                            if (location == 0) {
                                                outputs.clear();
                                                return;
                                            }
                                            valid = false;
                                            break;
                                        } catch (NumberFormatException ignored) {
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (valid) {
                        outputs.add(node);
                    }
                });

                for (GlslNewNode output : outputs) {
                    output.getType().addLayoutId("location", GlslNode.intConstant(0));
                }

                GlslStringWriter writer = new GlslStringWriter();
                tree.visit(writer);
                return writer.toString();
            }
        } catch (
                GlslSyntaxException e) {
            Veil.LOGGER.error("Failed to transform shader: {}", ctx.name(), e);
        }
        return source;
    }
}
