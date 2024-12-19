package foundry.veil.impl.compat;

import foundry.veil.Veil;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.dynamicbuffer.DynamicBufferType;
import foundry.veil.api.client.render.shader.processor.ShaderPreProcessor;
import foundry.veil.impl.glsl.GlslInjectionPoint;
import foundry.veil.impl.glsl.GlslParser;
import foundry.veil.impl.glsl.node.GlslNode;
import foundry.veil.impl.glsl.node.GlslTree;
import foundry.veil.impl.glsl.visitor.GlslStringWriter;
import org.jetbrains.annotations.ApiStatus;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.lwjgl.opengl.GL20C.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20C.GL_VERTEX_SHADER;

@ApiStatus.Internal
public class SodiumShaderPreProcessor implements ShaderPreProcessor {

    @Override
    public String modify(Context ctx, String source) {
        int activeBuffers = VeilRenderSystem.renderer().getDynamicBufferManger().getActiveBuffers();
        if (activeBuffers == 0) {
            return source;
        }

        DynamicBufferType[] buffers = DynamicBufferType.decode(activeBuffers);
        boolean vertex = ctx.type() == GL_VERTEX_SHADER;
        boolean fragment = ctx.type() == GL_FRAGMENT_SHADER;

        try {
            GlslTree tree = GlslParser.preprocessParse(source, Collections.emptyMap());
            List<GlslNode> mainBody = Objects.requireNonNull(tree.mainFunction().orElseThrow().getBody());

            boolean modified = false;
            for (int i = 0; i < buffers.length; i++) {
                DynamicBufferType type = buffers[i];
                String sourceName = type.getSourceName();
                String output = "layout(location = " + (1 + i) + ") out " + type.getType().getSourceString() + " " + sourceName;

                switch (type) {
                    case ALBEDO -> {
                        if (vertex) {
                            tree.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("out vec4 PassVeilVertexColor"));
                            mainBody.add(GlslParser.parseExpression("PassVeilVertexColor = _vert_color"));
                            modified = true;
                        }

                        if (fragment) {
                            tree.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("in vec4 PassVeilVertexColor"));
                            tree.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression(output));
                            mainBody.add(1, GlslParser.parseExpression(sourceName + " = diffuseColor * PassVeilVertexColor"));
                            modified = true;
                        }
                    }
                    case NORMAL -> {
                        if (vertex) {
                            boolean iris = IrisCompat.INSTANCE != null && IrisCompat.INSTANCE.areShadersLoaded();
                            if (!iris) {
                                tree.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("uniform mat3 VeilNormalMatrix"));
                                tree.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("layout(location = 4) in vec3 VeilNormal"));
                            }
                            tree.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("out vec3 PassVeilNormal"));
                            mainBody.add(GlslParser.parseExpression("PassVeilNormal = %s * %s".formatted(iris ? "iris_NormalMatrix" : "VeilNormalMatrix", iris ? "iris_Normal" : "VeilNormal")));
                            modified = true;
                        }

                        if (fragment) {
                            tree.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("in vec3 PassVeilNormal"));
                            tree.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression(output));
                            mainBody.add(GlslParser.parseExpression(sourceName + " = vec4(PassVeilNormal, 1.0)"));
                            modified = true;
                        }
                    }
                    case LIGHT_UV -> {
                        if (vertex) {
                            tree.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("out vec2 PassVeilLightUV"));
                            mainBody.add(GlslParser.parseExpression("PassVeilLightUV = _vert_tex_light_coord"));
                            modified = true;
                        }

                        if (fragment) {
                            tree.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("in vec2 PassVeilLightUV"));
                            tree.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression(output));
                            mainBody.add(GlslParser.parseExpression(sourceName + " = vec4(PassVeilLightUV, 0.0, 1.0)"));
                            modified = true;
                        }
                    }
                    case LIGHT_COLOR -> {
                        if (vertex) {
                            tree.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("out vec3 PassVeilLightColor"));
                            mainBody.add(GlslParser.parseExpression("PassVeilLightColor = texture(u_LightTex, _vert_tex_light_coord).rgb"));
                            modified = true;
                        }

                        if (fragment) {
                            tree.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("in vec3 PassVeilLightColor"));
                            tree.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression(output));
                            mainBody.add(GlslParser.parseExpression(sourceName + " = vec4(PassVeilLightColor, 1.0)"));
                            modified = true;
                        }
                    }
                }
            }

            if (modified) {
                if (fragment) {
                    tree.markOutputs();
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
