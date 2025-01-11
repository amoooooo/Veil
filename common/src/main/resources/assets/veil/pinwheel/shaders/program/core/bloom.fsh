#include veil:color_utilities

uniform sampler2D DiffuseSampler0;
uniform sampler2D BloomSampler;
uniform sampler2D BlurFinal;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    fragColor = texture(DiffuseSampler0, texCoord);
    vec4 bloomBase = texture(BloomSampler, texCoord);
    float factor = 1.0 - step(0.9, bloomBase.a) * 0.95;
    fragColor.rgb += acesToneMapping(texture(BlurFinal, texCoord).rgb) * factor;
}