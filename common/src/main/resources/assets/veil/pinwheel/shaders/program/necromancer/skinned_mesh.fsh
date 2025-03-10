#include veil:fog

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

in float vertexDistance;
in vec4 vertexColor;
in vec4 lightMapColor;
in vec4 overlayColor;
in vec2 texCoord0;
in vec3 normal;

out vec4 fragColor;

void main() {
    // #veil:albedo
    vec4 albedoColor = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
    vec4 color = albedoColor * lightMapColor;
    color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);
    if (color.a < 0.05) discard;

    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}