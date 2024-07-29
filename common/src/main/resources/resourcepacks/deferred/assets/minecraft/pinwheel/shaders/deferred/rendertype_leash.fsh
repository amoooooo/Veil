#include veil:material
#include veil:deferred_buffers

flat in vec4 color;
in vec2 texCoord2;
in vec4 lightmapColor;

void main() {
    fragAlbedo = color;
    fragNormal = vec4(0.0, 1.0, 0.0, 1.0);
    fragMaterial = ivec4(LEAD, 0, 0, 1);
    VEIL_OPAQUE_USE_DEFINED_HDR_SCALE();
    fragLightSampler = vec4(texCoord2, 0.0, 1.0);
    fragLightMap = lightmapColor;
}
