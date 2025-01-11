uniform sampler2D DiffuseDepthSampler;

in vec2 texCoord;

out vec4 OutColor;

void main() {
    gl_FragDepth = texture(DiffuseDepthSampler, texCoord).r;
    OutColor = vec4(0.0);
}
