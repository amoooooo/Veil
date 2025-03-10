#include veil:light
#include veil:fog

layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV0;
layout(location = 2) in vec3 Normal;
layout(location = 3) in uint BoneIndex;
layout(location = 4) in uint PackedOverlay;
layout(location = 5) in uint PackedLight;
layout(location = 6) in vec4 ModelColor;

struct BoneData {
    mat4 Transform;
    mat3 Normal;
};

layout(std140) uniform NecromancerBones {
    BoneData Bones[NECROMANCER_BONE_BUFFER_SIZE];
};

uniform sampler2D Sampler1;
uniform sampler2D Sampler2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat3 NormalMat;
uniform int FogShape;

uniform uint NecromancerBoneCount;

uniform vec3 Light0_Direction;
uniform vec3 Light1_Direction;

out float vertexDistance;
out vec4 vertexColor;
out vec4 lightMapColor;
out vec4 overlayColor;
out vec2 texCoord0;
out vec3 normal;

void main() {
    BoneData data = Bones[BoneIndex + NecromancerBoneCount * gl_InstanceID];
    mat4 transform = mat4(data.Transform);
    transform[3] = vec4(0.0, 0.0, 0.0, 1.0); // Last column is color, so set it to identity
    gl_Position = ProjMat * ModelViewMat * transpose(transform) * vec4(Position, 1.0);

    vertexDistance = fog_distance(ModelViewMat, Position, FogShape);

    vec3 BoneNormal = normalize(data.Normal * Normal);
    vertexColor = ModelColor * data.Transform[3] * minecraft_mix_light(Light0_Direction, Light1_Direction, BoneNormal);

    // #veil:light_uv
    ivec2 UV2 = ivec2(PackedLight & 15u, (PackedLight >> 4u) & 15u);
    ivec2 UV1 = ivec2(PackedOverlay & 15u, (PackedOverlay >> 4u) & 15u);

    // #veil:light_color;
    lightMapColor = texelFetch(Sampler2, UV2, 0);
    overlayColor = texelFetch(Sampler1, UV1, 0);

    texCoord0 = UV0;

    // #veil:normal
    normal = NormalMat * BoneNormal;
}