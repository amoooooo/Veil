#veil:buffer veil:camera

float getFov() {
    return 2.0 * atan(1.0 / VeilCamera.ProjMat[1][1]);
}

float getAspectRatio() {
    return VeilCamera.ProjMat[1][1] / VeilCamera.ProjMat[0][0];
}

vec3 viewPosFromDepth(float depth, vec2 uv) {
    float z = depth * 2.0 - 1.0;

    vec4 positionCS = vec4(uv * 2.0 - 1.0, z, 1.0);
    vec4 positionVS = VeilCamera.IProjMat * positionCS;
    positionVS /= positionVS.w;

    return positionVS.xyz;
}

vec3 viewToWorldSpaceDirection(vec3 direction) {
    return (VeilCamera.IViewMat * vec4(direction, 0.0)).xyz;
}

vec3 viewToWorldSpace(vec3 positionVS) {
    return VeilCamera.CameraPosition + (VeilCamera.IViewMat * vec4(positionVS, 1.0)).xyz;
}

vec3 viewToPlayerSpace(vec3 positionVS) {
    return (VeilCamera.IViewMat * vec4(positionVS, 1.0)).xyz;
}

vec3 playerSpaceToWorldSpace(vec3 positionPS) {
    return positionPS + VeilCamera.CameraPosition;
}

vec3 worldToViewSpaceDirection(vec3 viewSpace) {
    return (VeilCamera.ViewMat * vec4(viewSpace, 0.0)).xyz;
}

float depthSampleToWorldDepth(float depthSample) {
    float f = depthSample * 2.0 - 1.0;
    return 2.0 * VeilCamera.NearPlane * VeilCamera.FarPlane / (VeilCamera.FarPlane + VeilCamera.NearPlane - f * (VeilCamera.FarPlane - VeilCamera.NearPlane));
}

vec3 viewPosFromDepthSample(float depth, vec2 uv) {
    vec4 positionCS = vec4(uv, depth, 1.0) * 2.0 - 1.0;
    vec4 positionVS = VeilCamera.IProjMat * positionCS;
    positionVS /= positionVS.w;

    return positionVS.xyz;
}

vec3 viewDirFromUv(vec2 uv) {
    return (VeilCamera.IViewMat * vec4(normalize(viewPosFromDepth(1.0, uv)), 0.0)).xyz;
}