import foundry.veil.impl.glsl.GlslLexer;
import foundry.veil.impl.glsl.GlslParser;
import foundry.veil.impl.glsl.GlslSyntaxException;
import foundry.veil.impl.glsl.grammar.GlslVersion;
import foundry.veil.impl.glsl.node.GlslNode;
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

    private void testSpeed(String source) throws GlslSyntaxException {
        // Load classes
        for (int i = 0; i < 10; i++) {
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
    void testLexer() throws GlslSyntaxException {
        GlslLexer.Token[] tokens = GlslLexer.createTokens("float a = 4e2; // comment");
        Assertions.assertEquals("float a = 4e2 ;\n", this.toString(tokens));
    }

    @Test
    void testParser() throws GlslSyntaxException {
        GlslTree tree = GlslParser.parse("#version 330 core\nfloat a = 1.0 + 3.0;");
        Assertions.assertEquals(new GlslVersion(330, true), tree.getVersion());

        GlslNode replace = GlslParser.parseExpression("2 + 7 / 3 + max(gl_Position.x, 2.0);");

        tree.fields().forEach(node -> node.setInitializer(replace));

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
                
                    {
                        int a = 4;
                        float b = 7.0;
                        {
                            float h = 7.0;
                            int w = 1;
                        }
                    }
                
                    for (int i = 0; i < 7; i++) {
                        float g = float(i);
                        int k = 1;
                    }
                
                    float vertexDistance = fog_distance(pos, FogShape);
                    fragColor = linear_fog(baseColor, vertexDistance, FogStart, FogEnd, FogColor);
                }
                """);
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

    @Test
    void testStruct() throws GlslSyntaxException {
        GlslTree tree = GlslParser.parse("""
                struct Test {
                    float a;
                };
                """);

        GlslStringWriter writer = new GlslStringWriter();
        tree.visit(writer);
        System.out.println(writer);
    }

    @Test
    void testCompute() throws GlslSyntaxException {
        GlslTree tree = GlslParser.parse("""
                #version 430 core
                
                #extension GL_ARB_compute_shader : enable
                #extension GL_ARB_shader_atomic_counters : enable
                #extension GL_ARB_shader_storage_buffer_object : enable
                
                layout(std140) uniform CameraMatrices {
                    mat4 ProjMat;
                    mat4 IProjMat;
                    mat4 ViewMat;
                    mat4 IViewMat;
                    mat3 IViewRotMat;
                    vec3 CameraPosition;
                    float NearPlane;
                    float FarPlane;
                } VeilCamera;
                
                layout(std430) readonly buffer VeilLightInstanced {
                    float data[];
                };
                
                layout(std430) writeonly buffer VeilLightIndirect {
                    int commands[];
                };
                
                layout(binding = 0) uniform atomic_uint VeilLightCount;
                
                uniform int HighResSize;
                uniform int LowResSize;
                uniform int LightSize;
                uniform int PositionOffset;
                uniform int RangeOffset;
                uniform float FrustumPlanes[24];
                
                bool testSphere(float x, float y, float z, float r) {
                    return FrustumPlanes[0] * x + FrustumPlanes[1] * y + FrustumPlanes[2] * z + FrustumPlanes[3] >= -r &&
                           FrustumPlanes[4] * x + FrustumPlanes[5] * y + FrustumPlanes[6] * z + FrustumPlanes[7] >= -r &&
                           FrustumPlanes[8] * x + FrustumPlanes[9] * y + FrustumPlanes[10] * z + FrustumPlanes[11] >= -r &&
                           FrustumPlanes[12] * x + FrustumPlanes[13] * y + FrustumPlanes[14] * z + FrustumPlanes[15] >= -r &&
                           FrustumPlanes[16] * x + FrustumPlanes[17] * y + FrustumPlanes[18] * z + FrustumPlanes[19] >= -r &&
                           FrustumPlanes[20] * x + FrustumPlanes[21] * y + FrustumPlanes[22] * z + FrustumPlanes[23] >= -r;
                }
                
                layout (local_size_x = 1, local_size_y = 1, local_size_z = 1) in;
                void main() {
                    uint lightId = gl_GlobalInvocationID.x;
                    uint lightDataIndex = lightId * LightSize;
                
                    float x = data[lightDataIndex + PositionOffset];
                    float y = data[lightDataIndex + PositionOffset + 1];
                    float z = data[lightDataIndex + PositionOffset + 2];
                    float range = data[lightDataIndex + RangeOffset];
                    float dx = x - VeilCamera.CameraPosition.x;
                    float dy = y - VeilCamera.CameraPosition.y;
                    float dz = z - VeilCamera.CameraPosition.z;
                
                    bool visible = testSphere(dx, dy, dz, range * 1.414);
                    if (visible) {
                        uint i = atomicCounterIncrement(VeilLightCount) * 5;
                        bool highRes = dx * dx + dy * dy + dz * dz <= range * range;
                        commands[i] = highRes ? HighResSize : LowResSize;
                        commands[i + 1] = 1;
                        commands[i + 2] = !highRes ? HighResSize : 0;
                        commands[i + 3] = 0;
                        commands[i + 4] = int(lightId);
                    }
                }""");

        GlslStringWriter writer = new GlslStringWriter();
        tree.visit(writer);
        System.out.println(writer);
    }

    @Test
    void testArray() throws GlslSyntaxException {
        GlslTree tree = GlslParser.parse("""
                void main() {
                    float fragmentDistance = -ProjMat[3].z / ((gl_FragCoord.z) * -2.0 + 1.0 - ProjMat[2].z);
                }
                """);

        GlslStringWriter writer = new GlslStringWriter();
        tree.visit(writer);
        System.out.println(writer);
    }

    @Test
    void testPrimitiveArray() throws GlslSyntaxException {
        GlslTree tree = GlslParser.parse("""
                #version 150
                
                const vec3[] COLORS = vec3[](
                    vec3(0.022087, 0.098399, 0.110818),
                    vec3(0.011892, 0.095924, 0.089485),
                    vec3(0.027636, 0.101689, 0.100326),
                    vec3(0.046564, 0.109883, 0.114838),
                    vec3(0.064901, 0.117696, 0.097189),
                    vec3(0.063761, 0.086895, 0.123646),
                    vec3(0.084817, 0.111994, 0.166380),
                    vec3(0.097489, 0.154120, 0.091064),
                    vec3(0.106152, 0.131144, 0.195191),
                    vec3(0.097721, 0.110188, 0.187229),
                    vec3(0.133516, 0.138278, 0.148582),
                    vec3(0.070006, 0.243332, 0.235792),
                    vec3(0.196766, 0.142899, 0.214696),
                    vec3(0.047281, 0.315338, 0.321970),
                    vec3(0.204675, 0.390010, 0.302066),
                    vec3(0.080955, 0.314821, 0.661491)
                );
                
                const mat4 SCALE_TRANSLATE = mat4(
                    0.5, 0.0, 0.0, 0.25,
                    0.0, 0.5, 0.0, 0.25,
                    0.0, 0.0, 1.0, 0.0,
                    0.0, 0.0, 0.0, 1.0
                );
                """);

        GlslStringWriter writer = new GlslStringWriter();
        tree.visit(writer);
        System.out.println(writer);
    }
}
