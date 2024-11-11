import foundry.veil.impl.glsl.GlslLexer;
import foundry.veil.impl.glsl.GlslParser;
import foundry.veil.impl.glsl.GlslSyntaxException;
import foundry.veil.impl.glsl.grammar.GlslVersion;
import foundry.veil.impl.glsl.node.GlslTree;
import foundry.veil.impl.glsl.visitor.GlslStringWriter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class GlslTest {

    private String toString(GlslLexer.Token[] tokens) {
        StringBuilder build = new StringBuilder();
        for (GlslLexer.Token token : tokens) {
            build.append(token.value());
            if (token.type() == GlslLexer.TokenType.COMMENT || token.type() == GlslLexer.TokenType.SEMICOLON) {
                build.append('\n');
            } else {
                build.append(' ');
            }
        }
        return build.toString();
    }

    @Test
    void testLexer() throws GlslSyntaxException {
        GlslLexer.Token[] tokens = GlslLexer.createTokens("float a = 4e2; // comment");
        Assertions.assertEquals("float a = 4e2 ;\n", this.toString(tokens));
    }

    @Test
    void testParser() throws GlslSyntaxException {
        GlslLexer.Token[] tokens = GlslLexer.createTokens("#version 330 core\nfloat a = 1.0 + 3.0;");
        GlslTree tree = GlslParser.parse(tokens);
        Assertions.assertEquals(new GlslVersion(330, true), tree.getVersion());
        GlslStringWriter stringWriter = new GlslStringWriter();
        tree.visit(stringWriter);

        System.out.println(stringWriter);
    }

    @Test
    void testSet() throws GlslSyntaxException {
        this.testSpeed("""
                #version 330 core
                
                uniform vec4 color;
                
                void main() {
                    highp float test;
                    test = 2.0;
                }
                """);
    }

    @Test
    void testCall() throws GlslSyntaxException {
        this.testSpeed("""
                void main() {
                    vec4 baseColor = texture(DiffuseSampler0, texCoord);
                }
                """);
    }

    @Test
    void testShader() throws GlslSyntaxException {
        this.testSpeed("""
                #version 430 core
                
                uniform sampler2D DiffuseSampler0;
                uniform sampler2D DiffuseDepthSampler;
                
                const float FogStart = 0;
                const float FogEnd = 100;
                uniform vec4 FogColor;
                uniform int FogShape;
                
                in vec2 texCoord;
                
                out vec4 fragColor;
                
                vec3 test(int) {
                    return normalize(vec3(7.0, 0.0, 1.0));
                }
                
                void main() {
                    vec4 baseColor = texture(DiffuseSampler0, texCoord);
                    float depthSample = texture(DiffuseDepthSampler, texCoord).r;
                    vec3 pos = viewPosFromDepthSample(depthSample, texCoord);
                
                    float vertexDistance = fog_distance(pos, FogShape);
                    fragColor = linear_fog(baseColor, vertexDistance, FogStart, FogEnd, FogColor);
                }
                """);
    }

    private void testSpeed(String source) throws GlslSyntaxException {
        // Load classes
        for (int i = 0; i < 3; i++) {
            GlslLexer.Token[] tokens = GlslLexer.createTokens(source);
            GlslTree tree = GlslParser.parse(tokens);
            GlslStringWriter stringWriter = new GlslStringWriter();
            tree.visit(stringWriter);
        }

        long start = System.nanoTime();
        GlslLexer.Token[] tokens = GlslLexer.createTokens(source);

        long parseStart = System.nanoTime();
        GlslTree tree = GlslParser.parse(tokens);
        long parseEnd = System.nanoTime();

        GlslStringWriter stringWriter = new GlslStringWriter();
        tree.visit(stringWriter);
        long end = System.nanoTime();

        System.out.println(stringWriter);
        System.out.printf("Took %.1fms to tokenize, %.1fms to parse, %.1fms to stringify%n", (parseStart - start) / 1_000_000.0F, (parseEnd - parseStart) / 1_000_000.0F, (end - parseEnd) / 1_000_000.0F);
    }

    @Test
    void testReturn() throws GlslSyntaxException {
        this.testSpeed("""
                #version 430 core
                
                vec3 test(inout vec3 test, int) {
                    return normalize(vec3(7.0, 0.0, 1.0));
                }
                """);
    }
}
