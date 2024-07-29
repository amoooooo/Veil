#include veil:color_utilities
#include veil:blend

in vec2 texCoord;

uniform sampler2D AlbedoSampler;
uniform sampler2D CompatibilitySampler;
uniform sampler2D LightSampler;
uniform sampler2D HDRScaleSampler;

layout(location = 0) out vec4 fragColor;
layout(location = 1) out vec4 fragHDRScale;

void main() {
    float albedoAlpha = texture(AlbedoSampler, texCoord).a;
    vec4 diffuse = texture(LightSampler, texCoord);
    vec4 compatibility = texture(CompatibilitySampler, texCoord);
    float HDRScale = texture(HDRScaleSampler, texCoord).r;
    diffuse.rgb /= diffuse.a;
    fragColor = vec4(blend(vec4(diffuse.rgb, albedoAlpha), compatibility), albedoAlpha + compatibility.a * (1.0 - albedoAlpha));
    fragHDRScale = vec4(compatibility.a < 1.0 ? HDRScale : 0.0, 0.0, 0.0, 1.0);
}
