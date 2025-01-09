#veil:buffer veil:camera VeilCamera

layout (location = 0) in vec3 Position;
layout (location = 1) in vec3 LightPosition;
layout (location = 2) in vec3 Color;
layout (location = 3) in float Distance;

out vec3 lightPos;
out vec3 lightColor;
out float radius;

void main() {
    vec3 size = Position * Distance;
//    size.x *= length(VeilCamera.ViewMat[0].xyz);// Basis vector X
//    size.y *= length(VeilCamera.ViewMat[1].xyz);// Basis vector Y
//    size.z *= length(VeilCamera.ViewMat[2].xyz);// Basis vector Z

    gl_Position = VeilCamera.ProjMat * (VeilCamera.ViewMat * vec4(LightPosition - VeilCamera.CameraPosition, 1.0) + vec4(size, 0.0));
    lightPos = LightPosition;
    lightColor = Color;
    radius = Distance;
}
