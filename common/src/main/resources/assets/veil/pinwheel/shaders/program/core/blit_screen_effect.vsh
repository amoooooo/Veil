out vec2 texCoord;

uniform vec4 TexOffset;

void main() {
    vec2 uv = vec2(gl_VertexID & 1, gl_VertexID & 2);
    gl_Position = vec4(uv * vec2(3.0) - vec2(1.0), 0.0, 1.0);
    texCoord = uv * vec2(1.5) * TexOffset.zw + TexOffset.xy;
}