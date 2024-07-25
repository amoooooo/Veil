#include veil:tonemap
#include veil:material

uniform sampler2D AlbedoSampler;
uniform usampler2D MaterialSampler;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    uint material = texture(MaterialSampler, texCoord).r;
    vec4 org = texture(AlbedoSampler, texCoord);
    vec3 color = org.rgb;
    //    color = clamp(color, 0.0, 0.9);
    color = aces_min_inv(color);
    if (usesHDR(material)) color = vec3(2.3, 2.3, 0.0);
    fragColor = vec4(color, org.a);
}