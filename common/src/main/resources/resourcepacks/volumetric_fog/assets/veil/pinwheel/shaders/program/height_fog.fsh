#include veil:fog
#include veil:deferred_utils

#define FOG_Y 64.99
#define THICKNESS 0.1

uniform sampler2D DiffuseSampler0;
uniform sampler2D DiffuseDepthSampler;
uniform sampler2D Noise;

uniform vec4 FogColor;
//const vec4 FogColor = vec4(0.2, 0.2, 0.2, 1.0);

in vec2 texCoord;

out vec4 fragColor;

struct Plane{
    vec3 position;
    vec3 normal;
};

struct Intersection{
    float t;
    float hit;
    vec3  hitPoint;
};

const Plane PLANE = Plane(vec3(0.0, FOG_Y, 0.0), vec3(0.0, 1.0, 0.0));

void intersectPlane(vec3 ray, Plane p, inout Intersection i){
    float d = -dot(p.position, p.normal);
    float v = dot(ray, p.normal);
    float t = -(dot(VeilCamera.CameraPosition, p.normal) + d) / v;
    if (t > 0.0 && t < i.t){
        i.t = t;
        i.hit = 1.0;
        i.hitPoint = VeilCamera.CameraPosition + vec3(t * ray.x, t * ray.y, t * ray.z);
    }
}

void main() {
    vec4 baseColor = texture(DiffuseSampler0, texCoord);
    float depthSample = texture(DiffuseDepthSampler, texCoord).r;
    vec3 viewPos = viewPosFromDepthSample(depthSample, texCoord);
    vec3 pos = viewToWorldSpace(viewPos);

    float dist;

    if (VeilCamera.CameraPosition.y < FOG_Y) {
        dist = length(viewPos);

        Intersection i;
        i.t = length(viewPos);
        intersectPlane(viewDirFromUv(texCoord), PLANE, i);

        if (i.hit != 0) {
            dist = i.t;
        }
    } else {
        Intersection i;
        i.t = length(viewPos);
        intersectPlane(viewDirFromUv(texCoord), PLANE, i);
        if (i.hit == 0) {
            fragColor = baseColor;
            return;
        }

        dist = length(viewPos) - i.t;
    }

    float distance = dist;//(pos.y - FOG_Y) / FOG_HEIGHT;
    float thickness = clamp(exp(THICKNESS * -distance), 0.0, 1.0);
    fragColor = mix(FogColor, baseColor, thickness);
}

