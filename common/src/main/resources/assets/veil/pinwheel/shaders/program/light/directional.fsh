#include veil:common
#include veil:deferred_utils
#include veil:color_utilities

in vec2 texCoord;

uniform sampler2D VeilDynamicAlbedoSampler;
uniform sampler2D VeilDynamicNormalSampler;

uniform vec3 LightColor;
uniform vec3 LightDirection;

out vec4 fragColor;

void main() {
    vec4 albedoColor = texture(VeilDynamicAlbedoSampler, texCoord);
    if(albedoColor.a == 0) {
        discard;
    }

    vec3 normalVS = texture(VeilDynamicNormalSampler, texCoord).xyz;
    vec3 lightDirectionVS = worldToViewSpaceDirection(LightDirection);

    // lighting calculation
    float diffuse = clamp(smoothstep(-0.2, 0.2, -dot(normalVS, lightDirectionVS)), 0.0, 1.0);

    float reflectivity = 0.05;
    vec3 diffuseColor = diffuse * LightColor;
    fragColor = vec4(albedoColor.rgb * diffuseColor * (1.0 - reflectivity) + diffuseColor * reflectivity, 1.0);
}
