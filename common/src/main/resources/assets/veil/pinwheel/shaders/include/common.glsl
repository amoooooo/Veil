#define SQRT_TWO 1.41421356237
#define PI 3.14159265359
#define TWO_PI 6.28318530718

float fresnel(vec3 n, vec3 v, float r0) {
    float f = 1.0 - dot(n, v);
    return r0 + (1.0 - r0) * f * f * f * f *f;
}

float random(vec2 seed) {
    return fract(sin(dot(seed, vec2(12.9898, 78.233))) * 43758.5453);
}

vec3 spherePoint(vec2 seed) {
    float theta = TWO_PI * random(seed);
    float phi = acos(1.0 - 2.0 * random(seed + 0.2434));
    float f = sin(phi);
    return vec3(f * cos(theta), f * sin(theta), cos(phi));
}

// Cosine distribution picking by iq
vec3 hemiSpherePointCos(vec2 seed, vec3 normal)
{
    float u = random(seed);
    float v = random(seed + 5.236234);
    float a = 6.2831853 * v;
    u = 2.0*u - 1.0;
    return normalize(normal + vec3(sqrt(1.0-u*u) * vec2(cos(a), sin(a)), u));
}

float map(float value, float min1, float max1, float min2, float max2) {
    return min2 + (value - min1) * (max2 - min2) / (max1 - min1);
}

float mapClamped(float value, float min1, float max1, float min2, float max2) {
    return clamp(min2 + (value - min1) * (max2 - min2) / (max1 - min1), min2, max2);
}