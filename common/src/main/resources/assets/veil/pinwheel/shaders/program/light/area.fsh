#include veil:common
#include veil:deferred_utils
#include veil:color_utilities
#include veil:light

in mat4 lightMat;
in vec3 lightColor;
in vec2 size;
in float maxAngle;
in float maxDistance;

uniform sampler2D VeilDynamicAlbedoSampler;
uniform sampler2D VeilDynamicNormalSampler;
uniform sampler2D DiffuseDepthSampler;

uniform vec2 ScreenSize;

out vec4 fragColor;

// acos approximation
// faster and also doesn't flicker weirdly
float sacos(float x)
{
    float y = abs(clamp(x, -1.0, 1.0));
    float z = (-0.168577*y + 1.56723) * sqrt(1.0 - y);
    return mix(0.5*3.1415927, z, sign(x));
}

struct AreaLightResult { vec3 position; float angle; };
AreaLightResult closestPointOnPlaneAndAngle(vec3 point, mat4 planeMatrix, vec2 planeSize) {
    // no idea why i need to do this
    planeMatrix[3].xyz *= -1.0;
    // transform the point to the plane's local space
    vec3 localSpacePoint = (planeMatrix * vec4(point, 1.0)).xyz;
    // clamp position
    vec3 localSpacePointOnPlane = vec3(clamp(localSpacePoint.xy, -planeSize, planeSize), 0);

    // calculate the angles
    vec3 direction = normalize(localSpacePoint - localSpacePointOnPlane);
    float angle = sacos(dot(direction, vec3(0.0, 0.0, 1.0)));

    // transform back to global space
    return AreaLightResult((inverse(planeMatrix) * vec4(localSpacePointOnPlane, 1.0)).xyz, angle);
}

void main() {
    vec2 screenUv = gl_FragCoord.xy / ScreenSize;

    vec4 albedoColor = texture(VeilDynamicAlbedoSampler, screenUv);
    if (albedoColor.a == 0) {
        discard;
    }

    vec3 normalVS = texture(VeilDynamicNormalSampler, screenUv).xyz;
    float depth = texture(DiffuseDepthSampler, screenUv).r;
    vec3 viewPos = viewPosFromDepth(depth, screenUv);
    vec3 pos = viewToWorldSpace(viewPos);

    // lighting calculation
    AreaLightResult areaLightInfo = closestPointOnPlaneAndAngle(pos, lightMat, size);
    vec3 lightPos = areaLightInfo.position;
    float angle = areaLightInfo.angle;

    vec3 offset = lightPos - pos;
    vec3 lightDirection = (VeilCamera.ViewMat * vec4(normalize(offset), 0.0)).xyz;
    float diffuse = (dot(normalVS, lightDirection) + 1.0) * 0.5;
    diffuse = (diffuse + MINECRAFT_AMBIENT_LIGHT) / (1.0 + MINECRAFT_AMBIENT_LIGHT);
    diffuse *= attenuate_no_cusp(length(offset), maxDistance);
    // angle falloff
    float angleFalloff = clamp(angle, 0.0, maxAngle) / maxAngle;
    angleFalloff = smoothstep(1.0, 0.0, angleFalloff);
    diffuse *= angleFalloff;

    float reflectivity = 0.1;
    vec3 diffuseColor = diffuse * lightColor;

    fragColor = vec4(albedoColor.rgb * diffuseColor * (1.0 - reflectivity) + diffuseColor * reflectivity, 1.0);
}

