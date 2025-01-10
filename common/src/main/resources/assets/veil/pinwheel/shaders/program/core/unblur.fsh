uniform sampler2D DiffuseSampler0;
uniform vec2 OutSize;

in vec2 texCoord;

out vec4 fragColor;

const float offset = 3;

void main() {
    vec2 halfpixel = 0.5 / OutSize;

    vec4 sum = texture(DiffuseSampler0, texCoord + vec2(-halfpixel.x * 2.0, 0.0) * offset);
    sum += texture(DiffuseSampler0, texCoord + vec2(-halfpixel.x, halfpixel.y) * offset) * 2.0;
    sum += texture(DiffuseSampler0, texCoord + vec2(0.0, halfpixel.y * 2.0) * offset);
    sum += texture(DiffuseSampler0, texCoord + vec2(halfpixel.x, halfpixel.y) * offset) * 2.0;
    sum += texture(DiffuseSampler0, texCoord + vec2(halfpixel.x * 2.0, 0.0) * offset);
    sum += texture(DiffuseSampler0, texCoord + vec2(halfpixel.x, -halfpixel.y) * offset) * 2.0;
    sum += texture(DiffuseSampler0, texCoord + vec2(0.0, -halfpixel.y * 2.0) * offset);
    sum += texture(DiffuseSampler0, texCoord + vec2(-halfpixel.x, -halfpixel.y) * offset) * 2.0;
    fragColor = sum / 12.0;
}