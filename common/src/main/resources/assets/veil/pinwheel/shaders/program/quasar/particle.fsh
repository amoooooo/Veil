#include veil:fog

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

in float vertexDistance;
in vec2 texCoord0;
in vec4 vertexColor;
// #veil:light_color
in vec4 lightmapColor;

out vec4 fragColor;

float linear_fog_fade(float vertexDistance, float fogStart, float fogEnd) {
    return 0.0;
}

void main() {
    // #veil:albedo
    vec4 color = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
    if (color.a < 0.01) {
        discard;
    }
    fragColor = linear_fog(color * lightmapColor, vertexDistance, FogStart, FogEnd, FogColor);
}

