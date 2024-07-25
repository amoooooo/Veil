uniform sampler2D DestSampler;
uniform sampler2D DiffuseSampler0; // bloom

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 color = texture(DestSampler, texCoord);
    color.rgb += texture(DiffuseSampler0, texCoord).rgb;
    fragColor = color;
}
