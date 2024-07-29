#include veil:material
#include veil:deferred_buffers

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;

in vec4 vertexColor;
in vec2 texCoord0;
in vec2 texCoord2;
in vec4 lightmapColor;
in vec3 normal;

void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
    if (VEIL_HDR_SCALE == 1.0)
        color.rgb = vec3(0.3, 0.05, 1.0) * 5.;
    fragAlbedo = vec4(color.rgb, 1.0);
    fragNormal = vec4(normal, 1.0);
    fragMaterial = ivec4(BLOCK_SOLID, 0, 0, 1);
    VEIL_OPAQUE_USE_DEFINED_HDR_SCALE();
    fragLightSampler = vec4(texCoord2, 0.0, 1.0);
    fragLightMap = lightmapColor;
}
