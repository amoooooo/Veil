package foundry.veil.impl.compat.sodium;

import foundry.veil.api.client.render.dynamicbuffer.DynamicBufferType;
import foundry.veil.api.client.render.shader.processor.ShaderPreProcessor;
import foundry.veil.api.compat.IrisCompat;
import io.github.ocelot.glslprocessor.api.GlslInjectionPoint;
import io.github.ocelot.glslprocessor.api.GlslParser;
import io.github.ocelot.glslprocessor.api.GlslSyntaxException;
import io.github.ocelot.glslprocessor.api.node.GlslNode;
import io.github.ocelot.glslprocessor.api.node.GlslNodeList;
import io.github.ocelot.glslprocessor.api.node.GlslTree;
import io.github.ocelot.glslprocessor.lib.anarres.cpp.LexerException;
import org.jetbrains.annotations.ApiStatus;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@ApiStatus.Internal
public class SodiumShaderPreProcessor implements ShaderPreProcessor {

    @Override
    public void modify(Context ctx, GlslTree tree) throws IOException, GlslSyntaxException, LexerException {
        int activeBuffers = ctx.activeBuffers();
        if (activeBuffers == 0) {
            return;
        }

        DynamicBufferType[] buffers = DynamicBufferType.decode(activeBuffers);
        List<GlslNode> mainBody = Objects.requireNonNull(tree.mainFunction().orElseThrow().getBody());
        GlslNodeList treeBody = tree.getBody();

        boolean modified = false;
        for (int i = 0; i < buffers.length; i++) {
            DynamicBufferType type = buffers[i];
            String sourceName = type.getSourceName();
            String output = "layout(location = " + (1 + i) + ") out " + type.getType().getSourceString() + " " + sourceName;

            switch (type) {
                case ALBEDO -> {
                    if (ctx.isVertex()) {
                        treeBody.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("out vec4 PassVeilVertexColor"));
                        mainBody.add(GlslParser.parseExpression("PassVeilVertexColor = _vert_color"));
                        modified = true;
                    }

                    if (ctx.isFragment()) {
                        treeBody.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("in vec4 PassVeilVertexColor"));
                        treeBody.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression(output));
                        mainBody.add(1, GlslParser.parseExpression(sourceName + " = diffuseColor * PassVeilVertexColor"));
                        modified = true;
                    }
                }
                case NORMAL -> {
                    if (ctx.isVertex()) {
                        boolean iris = IrisCompat.INSTANCE != null && IrisCompat.INSTANCE.areShadersLoaded();
                        if (!iris) {
                            treeBody.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("uniform mat3 VeilNormalMatrix"));
                            treeBody.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("layout(location = 4) in vec3 VeilNormal"));
                        }
                        treeBody.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("out vec3 PassVeilNormal"));
                        mainBody.add(GlslParser.parseExpression("PassVeilNormal = %s * %s".formatted(iris ? "iris_NormalMatrix" : "VeilNormalMatrix", iris ? "iris_Normal" : "VeilNormal")));
                        modified = true;
                    }

                    if (ctx.isFragment()) {
                        treeBody.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("in vec3 PassVeilNormal"));
                        treeBody.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression(output));
                        mainBody.add(GlslParser.parseExpression(sourceName + " = vec4(PassVeilNormal, 1.0)"));
                        modified = true;
                    }
                }
                case LIGHT_UV -> {
                    if (ctx.isVertex()) {
                        treeBody.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("out vec2 PassVeilLightUV"));
                        mainBody.add(GlslParser.parseExpression("PassVeilLightUV = _vert_tex_light_coord"));
                        modified = true;
                    }

                    if (ctx.isFragment()) {
                        treeBody.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("in vec2 PassVeilLightUV"));
                        treeBody.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression(output));
                        mainBody.add(GlslParser.parseExpression(sourceName + " = vec4(PassVeilLightUV, 0.0, 1.0)"));
                        modified = true;
                    }
                }
                case LIGHT_COLOR -> {
                    if (ctx.isVertex()) {
                        treeBody.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("out vec3 PassVeilLightColor"));
                        mainBody.add(GlslParser.parseExpression("PassVeilLightColor = texture(u_LightTex, _vert_tex_light_coord).rgb"));
                        modified = true;
                    }

                    if (ctx.isFragment()) {
                        treeBody.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("in vec3 PassVeilLightColor"));
                        treeBody.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression(output));
                        mainBody.add(GlslParser.parseExpression(sourceName + " = vec4(PassVeilLightColor, 1.0)"));
                        modified = true;
                    }
                }
            }
        }

        if (modified && ctx.isFragment()) {
            tree.markOutputs();
        }
    }
}
