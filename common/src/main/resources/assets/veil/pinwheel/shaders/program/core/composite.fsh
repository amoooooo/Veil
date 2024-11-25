uniform sampler2D MainSampler;
uniform sampler2D LightSampler;

uniform vec4 ColorModulator;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 main = texture(MainSampler, texCoord);
    vec3 light = texture(LightSampler, texCoord).rgb;
    fragColor = vec4(main.rgb + light, main.a);
}
