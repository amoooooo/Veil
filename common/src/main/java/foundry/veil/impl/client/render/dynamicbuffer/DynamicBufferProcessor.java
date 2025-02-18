package foundry.veil.impl.client.render.dynamicbuffer;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import foundry.veil.Veil;
import foundry.veil.api.client.render.dynamicbuffer.DynamicBufferType;
import foundry.veil.api.client.render.shader.processor.ShaderPreProcessor;
import io.github.ocelot.glslprocessor.api.GlslInjectionPoint;
import io.github.ocelot.glslprocessor.api.GlslParser;
import io.github.ocelot.glslprocessor.api.GlslSyntaxException;
import io.github.ocelot.glslprocessor.api.grammar.GlslSpecifiedType;
import io.github.ocelot.glslprocessor.api.grammar.GlslTypeSpecifier;
import io.github.ocelot.glslprocessor.api.grammar.GlslVersionStatement;
import io.github.ocelot.glslprocessor.api.node.GlslConstantNode;
import io.github.ocelot.glslprocessor.api.node.GlslNode;
import io.github.ocelot.glslprocessor.api.node.GlslNodeList;
import io.github.ocelot.glslprocessor.api.node.GlslTree;
import io.github.ocelot.glslprocessor.api.node.expression.GlslAssignmentNode;
import io.github.ocelot.glslprocessor.api.node.expression.GlslOperationNode;
import io.github.ocelot.glslprocessor.api.node.function.GlslFunctionNode;
import io.github.ocelot.glslprocessor.api.node.function.GlslInvokeFunctionNode;
import io.github.ocelot.glslprocessor.api.node.variable.GlslNewNode;
import io.github.ocelot.glslprocessor.api.node.variable.GlslVariableNode;
import io.github.ocelot.glslprocessor.lib.anarres.cpp.LexerException;

import java.io.IOException;
import java.util.*;

public class DynamicBufferProcessor implements ShaderPreProcessor {

    private static final String[] VECTOR_ELEMENTS = {".x", ".y", ".z", ".w"};
    private static final Set<String> BLOCK_SHADERS = Set.of("rendertype_solid", "rendertype_cutout", "rendertype_cutout_mipped", "rendertype_translucent");

    public DynamicBufferProcessor() {
    }

    @Override
    public void modify(Context ctx, GlslTree tree) throws IOException, GlslSyntaxException, LexerException {
        DynamicBufferType[] types = DynamicBufferType.decode(ctx.activeBuffers());

        Map<String, GlslNode> markers = tree.getMarkers();
        GlslFunctionNode mainFunction = tree.mainFunction().orElseThrow();
        GlslNodeList mainFunctionBody = Objects.requireNonNull(mainFunction.getBody());
        GlslNodeList treeBody = tree.getBody();

        GlslVersionStatement version = tree.getVersionStatement();
        if (version.getVersion() < 330) {
            version.setVersion(330);
        }
        version.setCore(true);

        // Check if there is any lightmap to pull out
        GlslNode sampler = null;
        GlslNode lightmapUV = null;
        boolean blockLightmap = false;
        boolean injectLightmap = !markers.containsKey("veil:" + DynamicBufferType.LIGHT_COLOR.getName()) || !markers.containsKey("veil:" + DynamicBufferType.LIGHT_UV.getName());
        Map<String, Object> data = ctx.customProgramData();

        // must be a vanilla shader, so attempt to extract data from attributes
        boolean modified = false;
        if (ctx instanceof ShaderPreProcessor.MinecraftContext minecraftContext) {
            VertexFormat vertexFormat = minecraftContext.vertexFormat();
            if (ctx.isVertex() && injectLightmap) {
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

            for (int i = 0; i < types.length; i++) {
                DynamicBufferType type = types[i];
                String sourceName = type.getSourceName();
                String output = "layout(location = " + (1 + i) + ") out " + type.getType().getSourceString() + " " + sourceName;

                String shaderName = minecraftContext.shaderInstance();
                if ("rendertype_lines".equals(shaderName)) {
                    if (type == DynamicBufferType.NORMAL && ctx.isFragment()) {
                        treeBody.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression(output));
                        mainFunctionBody.add(new GlslAssignmentNode(new GlslVariableNode(type.getSourceName()), GlslParser.parseExpression("vec4(0.0, 0.0, 0.0, 1.0)"), GlslAssignmentNode.Operand.EQUAL));
                    }
                    if (type != DynamicBufferType.ALBEDO) {
                        continue;
                    }
                }

                boolean inVertex = data.containsKey("mask") && ((Integer) data.get("mask") & type.getMask()) != 0;
                if (injectLightmap) {
                    if (type == DynamicBufferType.LIGHT_UV) {
                        if (markers.containsKey("veil:" + DynamicBufferType.LIGHT_UV.getName())) {
                            continue;
                        }

                        if (ctx.isVertex()) {
                            if (lightmapUV != null) {
                                treeBody.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("out vec2 Pass" + type.getSourceName()));
                                if (blockLightmap) {
                                    mainFunctionBody.add(GlslParser.parseExpression("vec2 veilTexCoord2 = clamp(" + lightmapUV.getSourceString() + " / 256.0, vec2(0.5 / 16.0), vec2(15.5 / 16.0))"));
                                    mainFunctionBody.add(new GlslAssignmentNode(new GlslVariableNode("Pass" + type.getSourceName()), new GlslVariableNode("veilTexCoord2"), GlslAssignmentNode.Operand.EQUAL));
                                } else {
                                    mainFunctionBody.add(new GlslAssignmentNode(new GlslVariableNode("Pass" + type.getSourceName()), GlslParser.parseExpression("vec2(" + lightmapUV.getSourceString() + " / 256.0)"), GlslAssignmentNode.Operand.EQUAL));
                                }
                                modified = true;
                                data.compute("mask", (s, o) -> (o instanceof Integer val ? val : 0) | type.getMask());
                            }
                        } else if (inVertex) {
                            treeBody.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("in vec2 Pass" + type.getSourceName()));
                            treeBody.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression(output));
                            mainFunctionBody.add(new GlslAssignmentNode(new GlslVariableNode(type.getSourceName()), GlslParser.parseExpression("vec4(Pass" + type.getSourceName() + ", 0.0, 1.0)"), GlslAssignmentNode.Operand.EQUAL));
                            modified = true;
                        }
                    } else if (type == DynamicBufferType.LIGHT_COLOR) {
                        if (markers.containsKey("veil:" + DynamicBufferType.LIGHT_COLOR.getName())) {
                            continue;
                        }

                        if (ctx.isVertex()) {
                            if (lightmapUV != null && sampler != null) {
                                treeBody.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("out vec3 Pass" + type.getSourceName()));
                                if (blockLightmap) {
                                    mainFunctionBody.add(new GlslAssignmentNode(new GlslVariableNode("Pass" + type.getSourceName()), GlslParser.parseExpression("texture(" + sampler.getSourceString() + ", veilTexCoord2).rgb"), GlslAssignmentNode.Operand.EQUAL));
                                } else {
                                    mainFunctionBody.add(new GlslAssignmentNode(new GlslVariableNode("Pass" + type.getSourceName()), GlslParser.parseExpression("texelFetch(" + sampler.getSourceString() + ", " + lightmapUV.getSourceString() + " / 16, 0).rgb"), GlslAssignmentNode.Operand.EQUAL));
                                }
                                modified = true;
                                data.compute("mask", (s, o) -> (o instanceof Integer val ? val : 0) | type.getMask());
                            }
                        } else if (inVertex) {
                            treeBody.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("in vec3 Pass" + type.getSourceName()));
                            treeBody.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression(output));
                            mainFunctionBody.add(new GlslAssignmentNode(new GlslVariableNode(type.getSourceName()), GlslParser.parseExpression("vec4(Pass" + type.getSourceName() + ", 1.0)"), GlslAssignmentNode.Operand.EQUAL));
                            modified = true;
                        }
                    }
                }

                // Inject Normal passthrough into vertex and fragment shaders
                if (type == DynamicBufferType.NORMAL && !markers.containsKey("veil:" + DynamicBufferType.NORMAL.getName())) {
                    // Inject a normal output into the particle, lead, and text fragment shaders
                    if (ctx.isFragment() && ("particle".equals(shaderName) || "rendertype_leash".equals(shaderName) || "rendertype_text".equals(shaderName))) {
                        treeBody.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression(output));
                        mainFunctionBody.add(new GlslAssignmentNode(new GlslVariableNode(type.getSourceName()), GlslParser.parseExpression("vec4(0.0, 0.0, 1.0, 1.0)"), GlslAssignmentNode.Operand.EQUAL));
                        modified = true;
                    }

                    if (vertexFormat.contains(VertexFormatElement.NORMAL)) {
                        if (ctx.isVertex()) {
                            Optional<GlslNewNode> fieldOptional = tree.field(vertexFormat.getElementName(VertexFormatElement.NORMAL));
                            if (fieldOptional.isPresent()) {
                                treeBody.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("uniform mat3 NormalMat"));
                                treeBody.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("out vec3 Pass" + type.getSourceName()));
                                mainFunctionBody.add(new GlslAssignmentNode(new GlslVariableNode("Pass" + type.getSourceName()), GlslParser.parseExpression("NormalMat * " + fieldOptional.get().getName()), GlslAssignmentNode.Operand.EQUAL));
                                modified = true;
                                data.compute("mask", (s, o) -> (o instanceof Integer val ? val : 0) | type.getMask());
                            }
                        } else if (ctx.isFragment()) {
                            if (inVertex) {
                                treeBody.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("in vec3 Pass" + type.getSourceName()));
                                treeBody.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression(output));
                                mainFunctionBody.add(new GlslAssignmentNode(new GlslVariableNode(type.getSourceName()), GlslParser.parseExpression("vec4(Pass" + type.getSourceName() + ", 1.0)"), GlslAssignmentNode.Operand.EQUAL));
                                modified = true;
                            }
                        }
                    }
                }

                // Inject Color passthrough if necessary
                if (type == DynamicBufferType.ALBEDO && !markers.containsKey("veil:" + DynamicBufferType.ALBEDO.getName())) {
                    if (ctx.isVertex()) {
                        if (BLOCK_SHADERS.contains(shaderName)) {
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
                            treeBody.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("out vec4 Pass" + type.getSourceName()));
                            mainFunctionBody.add(new GlslAssignmentNode(new GlslVariableNode("Pass" + type.getSourceName()), color, GlslAssignmentNode.Operand.EQUAL));
                            modified = true;
                            data.compute("mask", (s, o) -> (o instanceof Integer val ? val : 0) | type.getMask());
                        } else if (vertexFormat.contains(VertexFormatElement.COLOR)) {
                            Optional<GlslNewNode> fieldOptional = tree.field(vertexFormat.getElementName(VertexFormatElement.COLOR));
                            if (fieldOptional.isPresent()) {
                                treeBody.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("out vec4 Pass" + type.getSourceName()));
                                mainFunctionBody.add(new GlslAssignmentNode(new GlslVariableNode("Pass" + type.getSourceName()), new GlslVariableNode(fieldOptional.get().getName()), GlslAssignmentNode.Operand.EQUAL));
                                modified = true;
                                data.compute("mask", (s, o) -> (o instanceof Integer val ? val : 0) | type.getMask());
                            }
                        }
                    } else if (inVertex) {
                        treeBody.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("in vec4 Pass" + type.getSourceName()));
                        treeBody.addFirst(GlslParser.parseExpression(output));

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

        for (int i = 0; i < types.length; i++) {
            DynamicBufferType bufferType = types[i];
            String typeName = bufferType.getName();
            GlslTypeSpecifier.BuiltinType outType = bufferType.getType();
            GlslNode node = markers.get("veil:" + typeName);

            boolean vertexPassthrough = data.containsKey("passmask") && ((Integer) data.get("passmask") & bufferType.getMask()) != 0;
            if (node == null) {
                if (vertexPassthrough) {
                    String sourceName = bufferType.getSourceName();
                    treeBody.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("in " + outType.getSourceString() + " Pass" + sourceName));
                    treeBody.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("layout(location = " + (1 + i) + ") out " + outType.getSourceString() + " " + sourceName));
                    mainFunctionBody.add(new GlslAssignmentNode(new GlslVariableNode(sourceName), new GlslVariableNode("Pass" + sourceName), GlslAssignmentNode.Operand.EQUAL));
                }
                continue;
            }

            if (vertexPassthrough) {
                throw new IOException("Node marked '#veil:" + typeName + "' in both vertex and fragment shader");
            }

            GlslSpecifiedType specifiedType = null;
            String copyName = null;
            List<GlslNode> body = null;
            int index = 0;
            if (node instanceof GlslNewNode newNode) {
                Optional<GlslTree.GlslBlock> block = tree.containingBlock(newNode);
                if (block.isPresent()) {
                    copyName = newNode.getName();
                    specifiedType = newNode.getType();
                    GlslTree.GlslBlock pair = block.get();
                    body = pair.body();
                    index = pair.index() + 1;
//                            pair.getFirst().add(pair.getSecond() + 1, GlslParser.parseExpression(copyName + " = " + sourceName));
                }
            } else if (node instanceof GlslAssignmentNode assignmentNode && assignmentNode.getFirst() instanceof GlslVariableNode variableNode) {
                Optional<GlslTree.GlslBlock> block = tree.containingBlock(assignmentNode);
                if (block.isPresent()) {
                    copyName = variableNode.getName();

                    List<GlslNewNode> fields = tree.searchField(copyName).toList();
                    if (fields.size() == 1) {
                        specifiedType = fields.getFirst().getType();
                        GlslTree.GlslBlock pair = block.get();
                        body = pair.body();
                        index = pair.index() + 1;
                    }
                }
            }

            if (copyName == null || specifiedType == null || !(specifiedType.getSpecifier() instanceof GlslTypeSpecifier.BuiltinType nodeType) || (!nodeType.isPrimitive() && !nodeType.isVector()) || (!nodeType.isFloat() && !nodeType.isInteger() && !nodeType.isUnsignedInteger())) {
                Veil.LOGGER.warn("Invalid node marked '#veil:{}' in {} shader: {}", typeName, ctx.typeName(), ctx.name());
                continue;
            }

            modified = true;
            String sourceName;
            if (ctx.isVertex()) {
                sourceName = "Pass" + bufferType.getSourceName();
                treeBody.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("out " + outType.getSourceString() + " " + sourceName));
                data.compute("passmask", (s, o) -> (o instanceof Integer val ? val : 0) | bufferType.getMask());
            } else {
                sourceName = bufferType.getSourceName();
                treeBody.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("layout(location = " + (1 + i) + ") out " + outType.getSourceString() + " " + sourceName));
            }

            String cast = switch (outType) {
                case FLOAT, VEC2, VEC3, VEC4 -> !nodeType.isFloat() ? "float" : null;
                case INT, IVEC2, IVEC3, IVEC4 -> !nodeType.isInteger() ? "int" : null;
                case UINT, UVEC2, UVEC3, UVEC4 -> !nodeType.isUnsignedInteger() ? "uint" : null;
                default -> null;
            };

            GlslNode expression;
            if (nodeType == outType) {
                expression = new GlslVariableNode(copyName);
            } else if (nodeType.getComponents() < outType.getComponents()) {
                // Not enough components, so pad
                StringBuilder builder = new StringBuilder(outType.getSourceString()).append("(");
                String padding = outType.getConstant(0);
                if (nodeType.getComponents() == 1) {
                    builder.append(cast != null ? cast + "(" + copyName + "), " : (copyName + ", "));
                } else {
                    for (int j = 0; j < nodeType.getComponents(); j++) {
                        builder.append(cast != null ? cast + "(" + copyName + VECTOR_ELEMENTS[j] + "), " : (copyName + VECTOR_ELEMENTS[j] + ", "));
                    }
                }
                for (int j = nodeType.getComponents(); j < 3; j++) {
                    builder.append(padding).append(", ");
                }
                builder.append(outType.getConstant(1));
                builder.append(')');
                expression = GlslParser.parseExpression(builder.toString());
            } else {
                expression = GlslParser.parseExpression((cast != null ? outType.getSourceString() : "") + '(' + copyName + ')');
            }

            body.add(index, new GlslAssignmentNode(new GlslVariableNode(sourceName), expression, GlslAssignmentNode.Operand.EQUAL));
        }

        if (modified && ctx.isFragment()) {
            tree.markOutputs();
        }
    }
}
