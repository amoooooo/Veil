#include veil:color_utilities

uniform sampler2D DiffuseSampler0;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec3 color = texture(DiffuseSampler0, texCoord).rgb;
    float lum = luminance(color);
    color *= pow(lum, 2.) /20.;
    fragColor = vec4(color, 1.0);
}
