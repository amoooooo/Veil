#include veil:common

uniform samplerCube DiffuseSampler0;

in vec2 texCoord;

out vec4 outColor;

void main() {
    float phi = texCoord.s * TWO_PI;
    // #veil:display
    float theta = (0.5 - texCoord.t) * PI;

    // #veil:normal
    vec3 direction = vec3(cos(phi) * cos(theta), sin(theta), sin(phi) * cos(theta));
    outColor = texture(DiffuseSampler0, direction);
}