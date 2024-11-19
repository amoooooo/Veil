#include veil:material
#include veil:deferred_buffers
#include veil:blend

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;

in vec4 vertexColor;
in vec2 texCoord0;
in vec3 normal;

void main() {
    vec4 color = texture(Sampler0, texCoord0) * vec4(1.0, 1.0, 1.0, vertexColor.a) * ColorModulator;
    if (color.a < 0.1) {
        discard;
    }
    fragAlbedo = color;
    fragNormal = vec4(normal, 1.0);
    fragMaterial = ivec4(CLOUD, TRANSLUCENT_TRANSPARENCY, 0, 1);
    fragLightSampler = vec4(0.0, 1.0, 0.0, 1.0);
    fragLightMap = vec4(vertexColor.rgb, 1.0);
}
