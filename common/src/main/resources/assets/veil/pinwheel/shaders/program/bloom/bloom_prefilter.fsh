#include veil:color_utilities

uniform sampler2D MainSampler;
uniform sampler2D HDRScaleSampler;

in vec2 texCoord;

out vec4 fragColor;

// https://github.com/Unity-Technologies/Graphics/blob/master/Packages/com.unity.render-pipelines.high-definition/Runtime/PostProcessing/Shaders/BloomCommon.hlsl#L4-L19
// Quadratic color thresholding
// curve = (threshold - knee, knee * 2, 0.25 / knee)
vec3 quadraticThreshold(vec3 color, float threshold, vec3 curve)
{
    // Pixel brightness
    float br = max(color.r, max(color.g, color.b));

    // Under-threshold part
    float rq = clamp(br - curve.x, 0.0, curve.y);
    rq = curve.z * rq * rq;

    // Combine and apply the brightness response curve
    color *= max(rq, br - threshold) / max(br, 1e-4);

    return color;
}

void main() {
    vec3 color = texture(MainSampler, texCoord).rgb;
    float HDRScale = texture(HDRScaleSampler, texCoord).r;

    color *= HDRScale;
    const float threshold = 1.0;
    const float knee = threshold * 0.5;
    color = quadraticThreshold(color, threshold, vec3(threshold - knee, knee * 2.0, 0.25 / knee));

    fragColor = vec4(color, 1.0);
}
