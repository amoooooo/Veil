#veil:buffer veil:camera VeilCamera

float getFov() {
    return 2.0 * atan(1.0 / VeilCamera.ProjMat[1][1]);
}

float getAspectRatio() {
    return VeilCamera.ProjMat[1][1] / VeilCamera.ProjMat[0][0];
}

// World Space

vec4 worldToViewSpace(vec4 pos) {
    vec4 viewSpacePos = VeilCamera.ViewMat * (pos - vec4(VeilCamera.CameraPosition, 0.0));
    return viewSpacePos / viewSpacePos.w;
}

vec4 worldToLocalSpace(vec4 pos) {
    return pos - vec4(VeilCamera.CameraPosition, 0.0);
}

vec4 worldToClipSpace(vec4 pos) {
    vec4 viewSpacePos = VeilCamera.ViewMat * (pos - vec4(VeilCamera.CameraPosition, 0.0));
    return VeilCamera.ProjMat * (viewSpacePos / viewSpacePos.w);
}

vec3 worldToScreenSpace(vec4 pos) {
    vec4 viewSpacePos = VeilCamera.ViewMat * (pos - vec4(VeilCamera.CameraPosition, 0.0));
    vec4 clipSpace = VeilCamera.ProjMat * (viewSpacePos / viewSpacePos.w);
    clipSpace.xyz /= clipSpace.w;
    return vec3((clipSpace.xy + vec2(1.0)) / 2.0, clipSpace.z);
}

#define worldToViewSpacePosition(pos) worldToViewSpace(vec4(pos, 1.0)).xyz
#define worldToLocalSpacePosition(pos) worldToLocalSpace(vec4(pos, 1.0)).xyz
#define worldToClipSpacePosition(pos) worldToClipSpace(vec4(pos, 1.0)).xyz
#define worldToScreenSpacePosition(pos) worldToScreenSpace(vec4(pos, 1.0))
#define worldToViewSpaceDirection(pos) worldToViewSpace(vec4(pos, 0.0)).xyz
#define worldToLocalSpaceDirection(pos) worldToLocalSpace(vec4(pos, 0.0)).xyz
#define worldToClipSpaceDirection(pos) worldToClipSpace(vec4(pos, 0.0)).xyz
#define worldToScreenSpaceDirection(pos) worldToScreenSpace(vec4(pos, 0.0))

// View Space

vec4 viewToWorldSpace(vec4 pos) {
    return vec4(VeilCamera.CameraPosition, 0.0) + VeilCamera.IViewMat * pos;
}

vec4 viewToLocalSpace(vec4 pos) {
    return VeilCamera.IViewMat * pos;
}

vec4 viewToClipSpace(vec4 pos) {
    return VeilCamera.ProjMat * pos;
}

vec3 viewToScreenSpace(vec4 pos) {
    vec4 clipSpace = VeilCamera.ProjMat * pos;
    clipSpace.xyz /= clipSpace.w;
    return vec3((clipSpace.xy + vec2(1.0)) / 2.0, clipSpace.z);
}

#define viewToWorldSpacePosition(pos) viewToWorldSpace(vec4(pos, 1.0)).xyz
#define viewToLocalSpacePosition(pos) viewToLocalSpace(vec4(pos, 1.0)).xyz
#define viewToClipSpacePosition(pos) viewToClipSpace(vec4(pos, 1.0)).xyz
#define viewToScreenSpacePosition(pos) viewToScreenSpace(vec4(pos, 1.0))
#define viewToWorldSpaceDirection(pos) viewToWorldSpace(vec4(pos, 0.0)).xyz
#define viewToLocalSpaceDirection(pos) viewToLocalSpace(vec4(pos, 0.0)).xyz
#define viewToClipSpaceDirection(pos) viewToClipSpace(vec4(pos, 0.0)).xyz
#define viewToScreenSpaceDirection(pos) viewToScreenSpace(vec4(pos, 0.0))

// Clip Space

vec4 clipToWorldSpace(vec4 pos) {
    return vec4(VeilCamera.CameraPosition, 0.0) + VeilCamera.IViewMat * VeilCamera.IProjMat * pos;
}

vec4 clipToLocalSpace(vec4 pos) {
    return VeilCamera.IViewMat * VeilCamera.IProjMat * pos;
}

vec4 clipToViewSpace(vec4 pos) {
    return VeilCamera.IProjMat * pos;
}

vec3 clipToScreenSpace(vec4 pos) {
    pos.xyz /= pos.w;
    return vec3((pos.xy + vec2(1.0)) / 2.0, pos.z);
}

#define clipToWorldSpacePosition(pos) clipToWorldSpace(vec4(pos, 1.0)).xyz
#define clipToLocalSpacePosition(pos) clipToLocalSpace(vec4(pos, 1.0)).xyz
#define clipToViewSpacePosition(pos) clipToViewSpace(vec4(pos, 1.0)).xyz
#define clipToScreenSpacePosition(pos) clipToScreenSpace(vec4(pos, 1.0))
#define clipToWorldSpaceDirection(pos) clipToWorldSpace(vec4(pos, 0.0)).xyz
#define clipToLocalSpaceDirection(pos) clipToLocalSpace(vec4(pos, 0.0)).xyz
#define clipToViewSpaceDirection(pos) clipToViewSpace(vec4(pos, 0.0)).xyz
#define clipToScreenSpaceDirection(pos) clipToScreenSpace(vec4(pos, 0.0))

// Screen Space

vec4 screenToWorldSpace(vec3 pos) {
    vec4 viewSpacePos = VeilCamera.IProjMat * (vec4(pos.xy, pos.z, 1.0) * 2.0 - 1.0);
    return vec4(VeilCamera.CameraPosition, 0.0) + VeilCamera.IViewMat * (viewSpacePos / viewSpacePos.w);
}

vec4 screenToLocalSpace(vec3 pos) {
    vec4 viewSpacePos = VeilCamera.IProjMat * (vec4(pos.xy, pos.z, 1.0) * 2.0 - 1.0);
    return VeilCamera.IViewMat * (viewSpacePos / viewSpacePos.w);
}

vec4 screenToViewSpace(vec3 pos) {
    vec4 viewSpacePos = VeilCamera.IProjMat * (vec4(pos.xy, pos.z, 1.0) * 2.0 - 1.0);
    return viewSpacePos / viewSpacePos.w;
}

vec4 screenToClipSpace(vec3 pos) {
    return vec4(pos.xy, pos.z, 1.0) * 2.0 - 1.0;
}

vec4 screenToWorldSpace(vec2 uv, float depth) {
    vec4 viewSpacePos = VeilCamera.IProjMat * (vec4(uv, depth, 1.0) * 2.0 - 1.0);
    return vec4(VeilCamera.CameraPosition, 0.0) + VeilCamera.IViewMat * (viewSpacePos / viewSpacePos.w);
}

vec4 screenToLocalSpace(vec2 uv, float depth) {
    vec4 viewSpacePos = VeilCamera.IProjMat * (vec4(uv, depth, 1.0) * 2.0 - 1.0);
    return VeilCamera.IViewMat * (viewSpacePos / viewSpacePos.w);
}

vec4 screenToViewSpace(vec2 uv, float depth) {
    vec4 viewSpacePos = VeilCamera.IProjMat * (vec4(uv, depth, 1.0) * 2.0 - 1.0);
    return viewSpacePos / viewSpacePos.w;
}

vec4 screenToClipSpace(vec2 uv, float depth) {
    return vec4(uv, depth, 1.0) * 2.0 - 1.0;
}

// Space Util

vec3 viewDirFromUv(vec2 uv) {
    return (VeilCamera.IViewMat * vec4(normalize(screenToViewSpace(uv, 1.0).xyz), 0.0)).xyz;
}
