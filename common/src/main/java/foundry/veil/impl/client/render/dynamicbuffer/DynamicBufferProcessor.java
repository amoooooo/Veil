package foundry.veil.impl.client.render.dynamicbuffer;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import foundry.veil.Veil;
import foundry.veil.api.client.render.dynamicbuffer.DynamicBufferType;
import foundry.veil.api.client.render.shader.processor.ShaderPreProcessor;
import foundry.veil.impl.glsl.GlslInjectionPoint;
import foundry.veil.impl.glsl.GlslParser;
import foundry.veil.impl.glsl.grammar.GlslSpecifiedType;
import foundry.veil.impl.glsl.grammar.GlslTypeQualifier;
import foundry.veil.impl.glsl.grammar.GlslTypeSpecifier;
import foundry.veil.impl.glsl.grammar.GlslVersion;
import foundry.veil.impl.glsl.node.GlslConstantNode;
import foundry.veil.impl.glsl.node.GlslNode;
import foundry.veil.impl.glsl.node.GlslTree;
import foundry.veil.impl.glsl.node.expression.GlslAssignmentNode;
import foundry.veil.impl.glsl.node.expression.GlslOperationNode;
import foundry.veil.impl.glsl.node.function.GlslFunctionNode;
import foundry.veil.impl.glsl.node.function.GlslInvokeFunctionNode;
import foundry.veil.impl.glsl.node.variable.GlslNewNode;
import foundry.veil.impl.glsl.node.variable.GlslVariableNode;
import foundry.veil.impl.glsl.visitor.GlslStringWriter;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;

import java.io.IOException;
import java.util.*;

import static org.lwjgl.opengl.GL20C.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20C.GL_VERTEX_SHADER;

public class DynamicBufferProcessor implements ShaderPreProcessor {

    private static final String[] VECTOR_ELEMENTS = {".x", ".y", ".z", ".w"};
    private static final Set<String> BLOCK_SHADERS = Set.of("rendertype_solid", "rendertype_cutout", "rendertype_cutout_mipped", "rendertype_translucent");

    private final DynamicBufferType[] types;
    private final Object2IntMap<String> validBuffers;

    public DynamicBufferProcessor(DynamicBufferType[] types) {
        this.types = types;
        this.validBuffers = new Object2IntArrayMap<>();
    }

    @Override
    public void prepare() {
        this.validBuffers.clear();
    }

    @Override
    public String modify(Context ctx, String source) throws IOException {
        VertexFormat vertexFormat = ctx.vertexFormat();
        if (ctx.definition() == null && (vertexFormat == null || ctx.name() == null)) {
            return source;
        }

        try {
            GlslTree tree = GlslParser.parse(source);
            Map<String, GlslNode> markers = tree.getMarkers();
            GlslFunctionNode mainFunction = tree.mainFunction().orElseThrow();
            List<GlslNode> mainFunctionBody = Objects.requireNonNull(mainFunction.getBody());

            GlslVersion version = tree.getVersion();
            if (version.getVersion() < 330) {
                version.setVersion(330);
            }
            version.setCore(true);

            // Check if there is any lightmap to pull out
            GlslNode sampler = null;
            GlslNode lightmapUV = null;
            boolean blockLightmap = false;
            boolean injectLightmap = !markers.containsKey("veil:" + DynamicBufferType.LIGHT_COLOR.getName()) || !markers.containsKey("veil:" + DynamicBufferType.LIGHT_UV.getName());
            boolean vertexShader = ctx.type() == GL_VERTEX_SHADER;
            boolean fragmentShader = ctx.type() == GL_FRAGMENT_SHADER;

            // must be a vanilla shader, so attempt to extract data from attributes
            boolean modified = false;
            if (vertexFormat != null) {
                if (vertexShader && injectLightmap) {
                    Optional<GlslNode> sampleLightmapOptional = mainFunction.stream().filter(node -> {
                        if (!(node instanceof GlslInvokeFunctionNode invokeFunctionNode) || invokeFunctionNode.getParameters().size() != 2) {
                            return false;
                        }
                        return invokeFunctionNode.getHeader() instanceof GlslVariableNode variableNode && ("minecraft_sample_lightmap".equals(variableNode.getName()));
                    }).findFirst();

                    if (sampleLightmapOptional.isPresent()) {
                        List<GlslNode> parameters = ((GlslInvokeFunctionNode) sampleLightmapOptional.get()).getParameters();
                        sampler = parameters.get(0);
                        lightmapUV = parameters.get(1);
                        blockLightmap = true;
                    } else if (vertexFormat.contains(VertexFormatElement.UV2)) {
                        Optional<GlslNode> texelFetchOptional = mainFunction.stream().filter(node -> {
                            if (!(node instanceof GlslInvokeFunctionNode invokeFunctionNode) || invokeFunctionNode.getParameters().size() != 3) {
                                return false;
                            }
                            List<GlslNode> parameters = invokeFunctionNode.getParameters();
                            return invokeFunctionNode.getHeader() instanceof GlslVariableNode functionName &&
                                    "texelFetch".equals(functionName.getName()) &&
                                    parameters.get(1) instanceof GlslOperationNode operation &&
                                    operation.getFirst() instanceof GlslVariableNode variableNode &&
                                    operation.getSecond() instanceof GlslConstantNode constantNode &&
                                    constantNode.intValue() == 16 &&
                                    operation.getOperand() == GlslOperationNode.Operand.DIVIDE &&
                                    vertexFormat.getElementName(VertexFormatElement.UV2).equals(variableNode.getName());
                        }).findFirst();

                        if (texelFetchOptional.isPresent()) {
                            List<GlslNode> parameters = ((GlslInvokeFunctionNode) texelFetchOptional.get()).getParameters();
                            sampler = parameters.get(0);
                            lightmapUV = ((GlslOperationNode) parameters.get(1)).getFirst();
                        }
                    }
                }

                for (int i = 0; i < this.types.length; i++) {
                    DynamicBufferType type = this.types[i];
                    String sourceName = type.getSourceName();
                    String output = "layout(location = " + (1 + i) + ") out " + type.getType().getSourceString() + " " + sourceName;

                    if (injectLightmap) {
                        if (type == DynamicBufferType.LIGHT_UV) {
                            if (markers.containsKey("veil:" + DynamicBufferType.LIGHT_UV.getName())) {
                                continue;
                            }

                            if (vertexShader) {
                                if (lightmapUV != null) {
                                    tree.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("out vec2 Pass" + type.getSourceName()));
                                    if (blockLightmap) {
                                        mainFunctionBody.add(GlslParser.parseExpression("vec2 veilTexCoord2 = clamp(" + lightmapUV.getSourceString() + " / 256.0, vec2(0.5 / 16.0), vec2(15.5 / 16.0))"));
                                        mainFunctionBody.add(new GlslAssignmentNode(new GlslVariableNode("Pass" + type.getSourceName()), new GlslVariableNode("veilTexCoord2"), GlslAssignmentNode.Operand.EQUAL));
                                    } else {
                                        mainFunctionBody.add(new GlslAssignmentNode(new GlslVariableNode("Pass" + type.getSourceName()), GlslParser.parseExpression("vec2(" + lightmapUV.getSourceString() + " / 256.0)"), GlslAssignmentNode.Operand.EQUAL));
                                    }
                                    modified = true;
                                    this.validBuffers.computeInt(ctx.shaderInstance(), (unused, mask) -> (mask != null ? mask : 0) | type.getMask());
                                }
                            } else if ((this.validBuffers.getInt(ctx.shaderInstance()) & type.getMask()) != 0) {
                                tree.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("in vec2 Pass" + type.getSourceName()));
                                tree.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression(output));
                                mainFunctionBody.add(new GlslAssignmentNode(new GlslVariableNode(type.getSourceName()), GlslParser.parseExpression("vec4(Pass" + type.getSourceName() + ", 0.0, 1.0)"), GlslAssignmentNode.Operand.EQUAL));
                                modified = true;
                            }
                        } else if (type == DynamicBufferType.LIGHT_COLOR) {
                            if (markers.containsKey("veil:" + DynamicBufferType.LIGHT_COLOR.getName())) {
                                continue;
                            }

                            if (vertexShader) {
                                if (lightmapUV != null && sampler != null) {
                                    tree.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("out vec3 Pass" + type.getSourceName()));
                                    if (blockLightmap) {
                                        mainFunctionBody.add(new GlslAssignmentNode(new GlslVariableNode("Pass" + type.getSourceName()), GlslParser.parseExpression("texture(" + sampler.getSourceString() + ", veilTexCoord2).rgb"), GlslAssignmentNode.Operand.EQUAL));
                                    } else {
                                        mainFunctionBody.add(new GlslAssignmentNode(new GlslVariableNode("Pass" + type.getSourceName()), GlslParser.parseExpression("texelFetch(" + sampler.getSourceString() + ", " + lightmapUV.getSourceString() + " / 16, 0).rgb"), GlslAssignmentNode.Operand.EQUAL));
                                    }
                                    modified = true;
                                    this.validBuffers.computeInt(ctx.shaderInstance(), (unused, mask) -> (mask != null ? mask : 0) | type.getMask());
                                }
                            } else if ((this.validBuffers.getInt(ctx.shaderInstance()) & type.getMask()) != 0) {
                                tree.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("in vec3 Pass" + type.getSourceName()));
                                tree.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression(output));
                                mainFunctionBody.add(new GlslAssignmentNode(new GlslVariableNode(type.getSourceName()), GlslParser.parseExpression("vec4(Pass" + type.getSourceName() + ", 1.0)"), GlslAssignmentNode.Operand.EQUAL));
                                modified = true;
                            }
                        }
                    }

                    // Inject Normal passthrough into vertex and fragment shaders
                    if (type == DynamicBufferType.NORMAL && !markers.containsKey("veil:" + DynamicBufferType.NORMAL.getName())) {
                        // Inject a normal output into the particle, lead, and text fragment shaders
                        if (fragmentShader && ("particle".equals(ctx.shaderInstance()) || "rendertype_leash".equals(ctx.shaderInstance()) || "rendertype_text".equals(ctx.shaderInstance()))) {
                            tree.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression(output));
                            mainFunctionBody.add(new GlslAssignmentNode(new GlslVariableNode(type.getSourceName()), GlslParser.parseExpression("vec4(0.0, 0.0, 1.0, 1.0)"), GlslAssignmentNode.Operand.EQUAL));
                            modified = true;
                        }

                        if (vertexFormat.contains(VertexFormatElement.NORMAL)) {
                            if (vertexShader) {
                                Optional<GlslNewNode> fieldOptional = tree.field(vertexFormat.getElementName(VertexFormatElement.NORMAL));
                                if (fieldOptional.isPresent()) {
                                    tree.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("uniform mat3 NormalMat"));
                                    tree.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("out vec3 Pass" + type.getSourceName()));
                                    mainFunctionBody.add(new GlslAssignmentNode(new GlslVariableNode("Pass" + type.getSourceName()), GlslParser.parseExpression("NormalMat * " + fieldOptional.get().getName()), GlslAssignmentNode.Operand.EQUAL));
                                    modified = true;
                                    this.validBuffers.computeInt(ctx.shaderInstance(), (unused, mask) -> (mask != null ? mask : 0) | type.getMask());
                                }
                            } else if (fragmentShader) {
                                if ((this.validBuffers.getInt(ctx.shaderInstance()) & type.getMask()) != 0) {
                                    tree.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("in vec3 Pass" + type.getSourceName()));
                                    tree.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression(output));
                                    mainFunctionBody.add(new GlslAssignmentNode(new GlslVariableNode(type.getSourceName()), GlslParser.parseExpression("vec4(Pass" + type.getSourceName() + ", 1.0)"), GlslAssignmentNode.Operand.EQUAL));
                                    modified = true;
                                }
                            }
                        }
                    }

                    // Inject Color passthrough if necessary
                    if (type == DynamicBufferType.ALBEDO && !markers.containsKey("veil:" + DynamicBufferType.ALBEDO.getName())) {
                        if (vertexShader) {
                            if (BLOCK_SHADERS.contains(ctx.shaderInstance())) {
                                // TODO remove face shading
                            }

                            Optional<GlslNode> mixLightOptional = mainFunction.stream().filter(node -> {
                                if (!(node instanceof GlslInvokeFunctionNode invokeFunctionNode) || invokeFunctionNode.getParameters().size() != 4) {
                                    return false;
                                }
                                return invokeFunctionNode.getHeader() instanceof GlslVariableNode variableNode && "minecraft_mix_light".equals(variableNode.getName());
                            }).findFirst();
                            if (mixLightOptional.isPresent()) {
                                GlslNode color = ((GlslInvokeFunctionNode) mixLightOptional.get()).getParameters().get(3);
                                tree.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("out vec4 Pass" + type.getSourceName()));
                                mainFunctionBody.add(new GlslAssignmentNode(new GlslVariableNode("Pass" + type.getSourceName()), color, GlslAssignmentNode.Operand.EQUAL));
                                modified = true;
                                this.validBuffers.computeInt(ctx.shaderInstance(), (unused, mask) -> (mask != null ? mask : 0) | type.getMask());
                            } else if (vertexFormat.contains(VertexFormatElement.COLOR)) {
                                Optional<GlslNewNode> fieldOptional = tree.field(vertexFormat.getElementName(VertexFormatElement.COLOR));
                                if (fieldOptional.isPresent()) {
                                    tree.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("out vec4 Pass" + type.getSourceName()));
                                    mainFunctionBody.add(new GlslAssignmentNode(new GlslVariableNode("Pass" + type.getSourceName()), new GlslVariableNode(fieldOptional.get().getName()), GlslAssignmentNode.Operand.EQUAL));
                                    modified = true;
                                    this.validBuffers.computeInt(ctx.shaderInstance(), (unused, mask) -> (mask != null ? mask : 0) | type.getMask());
                                }
                            }
                        } else if ((this.validBuffers.getInt(ctx.shaderInstance()) & type.getMask()) != 0) {
                            tree.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("in vec4 Pass" + type.getSourceName()));
                            tree.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression(output));

                            boolean hasColorModulator = tree.field("ColorModulator").isPresent();
                            boolean inserted = false;
                            for (int j = 0; j < mainFunctionBody.size(); j++) {
                                GlslNode body = mainFunctionBody.get(i);
                                Optional<GlslNode> textureOptional = body.stream().filter(node -> {
                                    if (!(node instanceof GlslInvokeFunctionNode invokeFunctionNode) || invokeFunctionNode.getParameters().size() != 2) {
                                        return false;
                                    }
                                    return invokeFunctionNode.getHeader() instanceof GlslVariableNode variableNode &&
                                            "texture".equals(variableNode.getName()) &&
                                            invokeFunctionNode.getParameters().getFirst() instanceof GlslVariableNode textureSampler &&
                                            "Sampler0".equals(textureSampler.getName());
                                }).findFirst();

                                if (textureOptional.isPresent()) {
                                    if (hasColorModulator) {
                                        mainFunctionBody.add(new GlslAssignmentNode(new GlslVariableNode(type.getSourceName()), GlslParser.parseExpression(textureOptional.get().getSourceString() + " * ColorModulator * Pass" + type.getSourceName()), GlslAssignmentNode.Operand.EQUAL));
                                    } else {
                                        mainFunctionBody.add(new GlslAssignmentNode(new GlslVariableNode(type.getSourceName()), GlslParser.parseExpression(textureOptional.get().getSourceString() + " * Pass" + type.getSourceName()), GlslAssignmentNode.Operand.EQUAL));
                                    }
                                    inserted = true;
                                    break;
                                }
                            }
                            if (!inserted) {
                                if (hasColorModulator) {
                                    mainFunctionBody.add(new GlslAssignmentNode(new GlslVariableNode(type.getSourceName()), GlslParser.parseExpression("Pass" + type.getSourceName() + " * ColorModulator"), GlslAssignmentNode.Operand.EQUAL));
                                } else {
                                    mainFunctionBody.add(new GlslAssignmentNode(new GlslVariableNode(type.getSourceName()), new GlslVariableNode("Pass" + type.getSourceName()), GlslAssignmentNode.Operand.EQUAL));
                                }
                            }
                            modified = true;
                        }
                    }
                }
            }

            if (fragmentShader) {
                for (int i = 0; i < this.types.length; i++) {
                    DynamicBufferType bufferType = this.types[i];
                    GlslNode node = markers.get("veil:" + bufferType.getName());
                    if (node == null) {
                        continue;
                    }

                    GlslSpecifiedType specifiedType = node.getType();
                    if (specifiedType == null || !(specifiedType.getSpecifier() instanceof GlslTypeSpecifier.BuiltinType nodeType) || (!nodeType.isPrimitive() && !nodeType.isVector()) || (!nodeType.isFloat() && !nodeType.isInteger() && !nodeType.isUnsignedInteger())) {
                        Veil.LOGGER.warn("Invalid node marked '#veil:{}' in shader: {}", bufferType.getName(), ctx.name());
                        continue;
                    }

                    modified = true;
                    String fieldName = bufferType.getSourceName();
                    GlslTypeSpecifier.BuiltinType outType = bufferType.getType();
                    tree.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("layout(location = " + (1 + i) + ") out " + outType.getSourceString() + " " + fieldName));
                    if (node instanceof GlslNewNode newNode) {
                        String cast = switch (outType) {
                            case FLOAT, VEC2, VEC3, VEC4 -> !nodeType.isFloat() ? "float" : null;
                            case INT, IVEC2, IVEC3, IVEC4 -> !nodeType.isInteger() ? "int" : null;
                            case UINT, UVEC2, UVEC3, UVEC4 -> !nodeType.isUnsignedInteger() ? "uint" : null;
                            default -> null;
                        };

                        GlslNode expression;
                        String name = newNode.getName();
                        if (nodeType == outType) {
                            expression = new GlslVariableNode(name);
                        } else if (nodeType.getComponents() < outType.getComponents()) {
                            // Not enough components, so pad
                            StringBuilder builder = new StringBuilder(outType.getSourceString()).append("(");
                            String padding = outType.isFloat() ? "0.0" : outType.isUnsignedInteger() ? "0u" : "0";
                            if (nodeType.getComponents() == 1) {
                                builder.append(cast != null ? cast + "(" + name + "), " : (name + ", "));
                            } else {
                                for (int j = 0; j < nodeType.getComponents(); j++) {
                                    builder.append(cast != null ? cast + "(" + name + VECTOR_ELEMENTS[j] + "), " : (name + VECTOR_ELEMENTS[j] + ", "));
                                }
                            }
                            for (int j = nodeType.getComponents(); j < 3; j++) {
                                builder.append(padding).append(", ");
                            }
                            builder.append(outType.isFloat() ? "1.0" : outType.isUnsignedInteger() ? "1u" : "1");
                            builder.append(')');
                            expression = GlslParser.parseExpression(builder.toString());
                        } else {
                            expression = GlslParser.parseExpression((cast != null ? outType.getSourceString() : "") + '(' + name + ')');
                        }

                        mainFunctionBody.add(new GlslAssignmentNode(new GlslVariableNode(fieldName), expression, GlslAssignmentNode.Operand.EQUAL));
                    }
                }
            }

            if (modified) {
                if (fragmentShader) {
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
                            if (qualifier instanceof GlslTypeQualifier.Layout(
                                    List<GlslTypeQualifier.LayoutId> layoutIds
                            )) {
                                for (GlslTypeQualifier.LayoutId layoutId : layoutIds) {
                                    if (!"location".equals(layoutId.identifier())) {
                                        continue;
                                    }

                                    GlslNode expression = layoutId.expression();
                                    if (expression == null) {
                                        continue;
                                    }

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

                        if (valid) {
                            outputs.add(node);
                        }
                    });

                    for (GlslNewNode output : outputs) {
                        output.getType().addLayoutId("location", GlslNode.intConstant(0));
                    }
                }

                GlslStringWriter writer = new GlslStringWriter();
                tree.visit(writer);
                return writer.toString();
            }
        } catch (Throwable t) {
            Veil.LOGGER.error("Failed to transform shader: {}", ctx.name(), t);
        }
        return source;
    }
}
