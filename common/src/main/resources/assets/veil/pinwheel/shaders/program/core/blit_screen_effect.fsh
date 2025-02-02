uniform sampler2D DiffuseSampler;
uniform vec4 ColorModulator;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    fragColor = texture(DiffuseSampler, texCoord) * ColorModulator;
}