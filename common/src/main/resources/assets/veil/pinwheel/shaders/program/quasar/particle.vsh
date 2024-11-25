#include veil:fog

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec2 UV0;
layout(location = 3) in ivec2 UV2;
layout(location = 4) in vec3 Normal;

uniform sampler2D Sampler2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat3 NormalMat;

out float vertexDistance;
out vec2 texCoord0;
out vec2 texCoord2;
out vec4 vertexColor;
out vec4 lightmapColor;
out vec3 normal;

void main() {
    vec4 WorldPosition = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * WorldPosition;
    vertexDistance = length(WorldPosition.xyz);
    texCoord0 = UV0;
    texCoord2 = vec2(UV2 / 256.0);
    vertexColor = Color;
    lightmapColor = texelFetch(Sampler2, UV2 / 16, 0);
    normal = NormalMat * Normal;
}
