#include veil:common
#include veil:deferred_utils
#include veil:color_utilities
#include veil:light

in vec3 lightPos;
in vec3 lightColor;
in float radius;

uniform sampler2D VeilDynamicAlbedoSampler;
uniform sampler2D VeilDynamicNormalSampler;
uniform sampler2D DiffuseDepthSampler;

uniform vec2 ScreenSize;

out vec4 fragColor;

void main() {
    vec2 screenUv = gl_FragCoord.xy / ScreenSize;

    vec4 albedoColor = texture(VeilDynamicAlbedoSampler, screenUv);
    if(albedoColor.a == 0) {
        discard;
    }

    float depth = texture(DiffuseDepthSampler, screenUv).r;
    vec3 pos = viewToWorldSpace(viewPosFromDepth(depth, screenUv));

    // lighting calculation
    vec3 offset = lightPos - pos;

    vec3 normalVS = texture(VeilDynamicNormalSampler, screenUv).xyz;
    vec3 lightDirection = (VeilCamera.ViewMat * vec4(normalize(offset), 0.0)).xyz;
    float diffuse = clamp(0.0, 1.0, dot(normalVS, lightDirection));
    diffuse = (diffuse + MINECRAFT_AMBIENT_LIGHT) / (1.0 + MINECRAFT_AMBIENT_LIGHT);
    diffuse *= attenuate_no_cusp(length(offset), radius);

    float reflectivity = 0.1;
    vec3 diffuseColor = diffuse * lightColor;
    fragColor = vec4(albedoColor.rgb * diffuseColor * (1.0 - reflectivity) + diffuseColor * reflectivity, 1.0);
}
