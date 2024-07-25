#include veil:tonemap

uniform sampler2D DiffuseSampler0;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 org = texture(DiffuseSampler0, texCoord);
    vec3 color = org.rgb;
//    color.rgb *= 10.0;
//    fragColor = vec4(aces_full(color.rgb), color.a);
//    color = aces_min_inv(color.rgb);
    color = aces_min(color.rgb);
//    color = aces_full(color.rgb);
    fragColor = vec4(color, org.a);
//    fragColor = color;
}