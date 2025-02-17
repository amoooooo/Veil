vec4 toGamma(vec4 linear) {
    return vec4(pow(linear.rgb, vec3(1.0 / 2.2)), linear.a);
}

vec3 toGamma(vec3 linear) {
    return pow(linear, vec3(1.0 / 2.2));
}

float toGamma(float linear) {
    return pow(linear, 1.0 / 2.2);
}

vec4 toLinear(vec4 gamma) {
    return vec4(pow(gamma.rgb, vec3(2.2)), gamma.a);
}

vec3 toLinear(vec3 gamma) {
    return pow(gamma, vec3(2.2));
}

float toLinear(float gamma) {
    return pow(gamma, 2.2);
}

vec3 linearToLogC(vec3 color) {
    return 0.386036 + 0.244161 * log(5.555556 * color + 0.047996) / log(10.0);
}

vec4 linearToLogC(vec4 color) {
    return vec4(linearToLogC(color.rgb), color.a);
}

vec3 logCToLinear(vec3 color) {
    return (pow(vec3(10.0), (color - 0.386036) / 0.244161) - 0.047996) / 5.555556;
}

vec4 logCToLinear(vec4 color) {
    return vec4(logCToLinear(color.rgb), color.a);
}

vec3 rgbToHsv(vec3 c) {
    const vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));

    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
}

vec3 hsvToRgb(vec3 c) {
    const vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

float luminance(vec3 c) {
    return dot(c, vec3(0.2, 0.7, 0.1));
}

vec3 acesToneMapping(vec3 color) {
    color = (color * (2.51 * color + 0.03)) / (color * (2.43 * color  + 0.59) + 0.14);
    return clamp(color, 0.0, 1.0);
}

vec3 reverseAces(vec3 color) {
    color = clamp(color, 0.01, 0.99);
    return (-sqrt(-0.0428 * color * color + 0.0555 * color) - 0.1214 * color + 0.006) / (color - 1.0);
}