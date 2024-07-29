#include veil:tonemapping

uniform sampler2D MainSampler;
uniform sampler2D BloomSampler;
uniform sampler2D HDRScaleSampler;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 main = texture(MainSampler, texCoord);
    vec3 bloom = texture(BloomSampler, texCoord).rgb;
    float HDRScale = texture(HDRScaleSampler, texCoord).r;
    vec3 color;
    if (HDRScale > 0.0) {
        color = aces_full(main.rgb + bloom);
    } else {
        color = main.rgb + aces_full(main.rgb + bloom) - aces_full(main.rgb);
    }
    fragColor = vec4(color, main.a);
}