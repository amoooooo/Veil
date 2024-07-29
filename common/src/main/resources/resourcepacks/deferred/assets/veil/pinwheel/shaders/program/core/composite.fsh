#include veil:blend

uniform sampler2D OpaqueSampler;
uniform sampler2D OpaqueHDRScaleSampler;
uniform sampler2D OpaqueDepthSampler;
uniform sampler2D MainSampler;
uniform sampler2D MainDepthSampler;
uniform sampler2D TransparentSampler;
uniform usampler2D TransparentMaterialSampler;
uniform sampler2D TransparentHDRScaleSampler;
uniform sampler2D TransparentDepthSampler;

uniform vec4 ColorModulator;

in vec2 texCoord;

layout(location = 0) out vec4 fragColor;
layout(location = 1) out vec4 fragHDRScale;

void main() {
    vec4 main = texture(MainSampler, texCoord);
    float mainDepth = texture(MainDepthSampler, texCoord).r;
    vec4 opaque = texture(OpaqueSampler, texCoord);
    float opaqueHDRScale = texture(OpaqueHDRScaleSampler, texCoord).r;
    float opaqueDepth = texture(OpaqueDepthSampler, texCoord).r;
    vec4 transparent = texture(TransparentSampler, texCoord);
    uint transparentBlend = texture(TransparentMaterialSampler, texCoord).g;
    float transparentHDRScale = texture(TransparentHDRScaleSampler, texCoord).r;
    float transparentDepth = texture(TransparentDepthSampler, texCoord).r;

    fragColor = vec4(main.rgb, 1.0) * ColorModulator;
    fragColor.rgb = blend(fragColor, opaque);
    fragColor.rgb = blend(transparentBlend, fragColor, transparent);

    gl_FragDepth = min(mainDepth, min(opaqueDepth, transparentDepth));

    if (mainDepth < opaqueDepth)
        opaqueHDRScale = 0.0;
    if (mainDepth < transparentDepth)
        transparentHDRScale = 0.0;
    float HDRScale;
    if (transparentBlend == ADDITIVE_TRANSPARENCY) {
        HDRScale = max(opaqueHDRScale, transparentHDRScale);
    } else if (transparentBlend == NO_TRANSPARENCY) {
        HDRScale = transparent.a == 1.0 ? transparentHDRScale : opaqueHDRScale;
    } else {
        HDRScale = mix(opaqueHDRScale, transparentHDRScale, transparent.a);
    }
    fragHDRScale = vec4(HDRScale, 0.0, 0.0, 1.0);
}
