#include veil:color_utilities
#define SMOOTHING 0.3

uniform sampler2D DiffuseSampler0;
uniform sampler2D BloomSampler;
uniform sampler2D BlurFinal;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    fragColor = texture(DiffuseSampler0, texCoord);

    vec4 bloomBase = texture(BloomSampler, texCoord);
    vec4 bloomBlur = texture(BlurFinal, texCoord) * vec4(vec3(0.8), 1.0);

    float factor = 1.0 - step(0.9, bloomBase.a);
    float baseLum = luminance(bloomBase.rgb);
    factor = max(factor, smoothstep(baseLum - SMOOTHING, baseLum + SMOOTHING, luminance(bloomBlur.rgb)));

    fragColor.rgb += acesToneMapping(bloomBlur.rgb) * factor * 0.5;
}