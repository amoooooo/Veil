package foundry.veil.impl.client.render.dynamicbuffer;

import foundry.veil.Veil;
import foundry.veil.api.client.render.dynamicbuffer.DynamicBufferType;
import foundry.veil.api.client.render.shader.processor.ShaderPreProcessor;
import foundry.veil.impl.glsl.GlslInjectionPoint;
import foundry.veil.impl.glsl.GlslParser;
import foundry.veil.impl.glsl.GlslSyntaxException;
import foundry.veil.impl.glsl.grammar.GlslSpecifiedType;
import foundry.veil.impl.glsl.grammar.GlslTypeQualifier;
import foundry.veil.impl.glsl.grammar.GlslTypeSpecifier;
import foundry.veil.impl.glsl.node.GlslNode;
import foundry.veil.impl.glsl.node.GlslTree;
import foundry.veil.impl.glsl.node.expression.GlslAssignmentNode;
import foundry.veil.impl.glsl.node.function.GlslFunctionNode;
import foundry.veil.impl.glsl.node.variable.GlslNewNode;
import foundry.veil.impl.glsl.node.variable.GlslVariableNode;
import foundry.veil.impl.glsl.visitor.GlslStringWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL20C.GL_FRAGMENT_SHADER;

public class DynamicBufferProcessor implements ShaderPreProcessor {

    private static final String[] VECTOR_ELEMENTS = {".x", ".y", ".z", ".w"};

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
                DynamicBufferType bufferType = this.types[i];
                GlslNode node = markers.get("veil:" + bufferType.getName());
                if (node != null && ctx.type() == GL_FRAGMENT_SHADER) {
                    modified = true;

                    GlslSpecifiedType specifiedType = node.getType();
                    if (specifiedType == null || !(specifiedType.getSpecifier() instanceof GlslTypeSpecifier.BuiltinType nodeType) || (!nodeType.isPrimitive() && !nodeType.isVector()) || (!nodeType.isFloat() && !nodeType.isInteger() && !nodeType.isUnsignedInteger())) {
                        Veil.LOGGER.warn("Invalid node marked '#veil:{}' in shader: {}", bufferType.getName(), ctx.name());
                        return source;
                    }

                    String fieldName = bufferType.getSourceName();
                    GlslTypeSpecifier.BuiltinType outType = bufferType.getType();
                    tree.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("layout(location = " + (1 + i) + ") out " + outType.getSourceString() + " " + fieldName));
                    if (node instanceof GlslNewNode newNode) {
                        GlslNode expression;
                        String cast = switch (outType) {
                            case FLOAT, VEC2, VEC3, VEC4 -> !nodeType.isFloat() ? "float" : null;
                            case INT, IVEC2, IVEC3, IVEC4 -> !nodeType.isInteger() ? "int" : null;
                            case UINT, UVEC2, UVEC3, UVEC4 -> !nodeType.isUnsignedInteger() ? "uint" : null;
                            default -> null;
                        };

                        String name = newNode.getName();
                        if (nodeType == outType) {
                            expression = new GlslVariableNode(name);
                        } else if (nodeType.getComponents() < outType.getComponents()) {
                            // Not enough components, so pad
                            StringBuilder builder = new StringBuilder(outType.getSourceString()).append("(");
                            String padding = outType.isFloat() ? "0.0" : outType.isUnsignedInteger() ? "0u" : "0";
                            switch (outType.getComponents()) {
                                case 2 -> {
                                    builder.append(cast != null ? cast + "(" + name + ".x), " : (name + ".x, "));
                                    builder.append(padding);
                                }
                                case 3 -> {
                                    for (int j = 0; j < nodeType.getComponents(); j++) {
                                        builder.append(cast != null ? cast + "(" + name + VECTOR_ELEMENTS[j] + "), " : (name + VECTOR_ELEMENTS[j] + ", "));
                                    }
                                    for (int j = nodeType.getComponents(); j < 3; j++) {
                                        builder.append(padding).append(", ");
                                    }
                                    builder.delete(builder.length() - 2, builder.length());
                                    builder.append(')');
                                }
                                case 4 -> {
                                    for (int j = 0; j < nodeType.getComponents(); j++) {
                                        builder.append(cast != null ? cast + "(" + name + VECTOR_ELEMENTS[j] + "), " : (name + VECTOR_ELEMENTS[j] + ", "));
                                    }
                                    for (int j = nodeType.getComponents(); j < 3; j++) {
                                        builder.append(padding).append(", ");
                                    }
                                    builder.append(outType.isFloat() ? "1.0" : outType.isUnsignedInteger() ? "1u" : "1");
                                    builder.append(')');
                                }
                            }
                            expression = GlslParser.parseExpression(builder.toString());
                        } else if (nodeType.getComponents() > outType.getComponents()) {
                            // Too many components, so truncate
                            StringBuilder builder = new StringBuilder(outType.getSourceString()).append("(");
                            for (int j = 0; j < outType.getComponents(); j++) {
                                builder.append(cast != null ? cast + "(" + name + VECTOR_ELEMENTS[j] + "), " : (name + VECTOR_ELEMENTS[j] + ", "));
                            }
                            builder.delete(builder.length() - 2, builder.length());
                            builder.append(')');
                            expression = GlslParser.parseExpression(builder.toString());
                        } else {
                            expression = GlslParser.parseExpression((cast != null ? outType.getSourceString() : "") + '(' + name + ')');
                        }

                        mainFunction.getBody().add(new GlslAssignmentNode(new GlslVariableNode(fieldName), expression, GlslAssignmentNode.Operand.EQUAL));
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
        } catch (GlslSyntaxException e) {
            Veil.LOGGER.error("Failed to transform shader: {}", ctx.name(), e);
        }
        return source;
    }
}
