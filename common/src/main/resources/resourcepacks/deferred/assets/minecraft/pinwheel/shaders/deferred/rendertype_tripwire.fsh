#include veil:material
#include veil:deferred_buffers
#include veil:blend

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;

in vec4 vertexColor;
in vec2 texCoord0;
in vec2 texCoord2;
in vec4 overlayColor;
in vec4 lightmapColor;
in vec3 normal;

void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
    if (color.a < 0.1) {
        discard;
    }
    fragAlbedo = color;
    fragNormal = vec4(normal, 1.0);
    fragMaterial = ivec4(BLOCK_TRANSLUCENT, TRANSLUCENT_TRANSPARENCY, 0, 1);
    VEIL_TRANSPARENT_USE_DEFINED_HDR_SCALE();
    fragLightSampler = vec4(texCoord2, 0.0, 1.0);
    fragLightMap = lightmapColor;
}
