#buffer veil:camera

uniform sampler2D DiffuseSampler0;
uniform sampler2D DiffuseDepthSampler;
uniform sampler2D LightSampler;

uniform vec4 ColorModulator;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 main = texture(DiffuseSampler0, texCoord);
    float mainDepth = texture(DiffuseDepthSampler, texCoord).r;
    vec3 light = texture(LightSampler, texCoord).rgb;
    fragColor = vec4(main.rgb + light, main.a);
    gl_FragDepth = mainDepth;
}
