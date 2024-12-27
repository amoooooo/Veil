#include veil:light
#include veil:fog

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec2 UV0;
layout(location = 3) in ivec2 UV1;
layout(location = 4) in ivec2 UV2;
layout(location = 5) in vec3 Normal;
layout(location = 6) in uint BoneIndex;

uniform mat4[16] BoneTransforms;
uniform vec4[16] BoneColors;
uniform bool[16] BoneEnabled;

uniform sampler2D Sampler1;
uniform sampler2D Sampler2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat3 NormalMat;
uniform mat3 IViewRotMat;
uniform int FogShape;

uniform vec3 Light0_Direction;
uniform vec3 Light1_Direction;

out float vertexDistance;
out vec4 vertexColor;
out vec4 lightMapColor;
out vec4 overlayColor;
out vec2 texCoord0;
out vec3 normal;

void main() {
    gl_Position = ProjMat * ModelViewMat * BoneTransforms[BoneIndex] * vec4(Position, 1.0);

    vertexDistance = fog_distance(ModelViewMat, IViewRotMat * Position, FogShape);

    vertexColor = Color * BoneColors[BoneIndex] * minecraft_mix_light(Light0_Direction, Light1_Direction, normal);

    lightMapColor = texelFetch(Sampler2, UV2 / 16, 0);
    overlayColor = texelFetch(Sampler1, UV1, 0);

    texCoord0 = UV0;

    normal = NormalMat * Normal;
}