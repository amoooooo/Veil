#include veil:fog
#include veil:deferred_utils

uniform sampler2D DiffuseSampler0;
uniform sampler2D DiffuseDepthSampler;

const float FogStart = -10;
const float FogEnd = 100;
uniform vec4 FogColor;
uniform int FogShape;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 baseColor = texture(DiffuseSampler0, texCoord);
    float depthSample = texture(DiffuseDepthSampler, texCoord).r;
    vec3 pos = viewPosFromDepthSample(depthSample, texCoord);

    float vertexDistance = fog_distance(pos, FogShape);
    fragColor = linear_fog(baseColor, vertexDistance, FogStart, FogEnd, FogColor);
}
