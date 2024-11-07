#include veil:camera

layout (location = 0) in vec3 Position;
layout (location = 1) in mat4 LightMatrix;
layout (location = 5) in vec3 Color;
layout (location = 6) in vec2 Size;
layout (location = 7) in float NormalizedAngle;
layout (location = 8) in float Distance;

out mat4 lightMat;
out vec3 lightColor;
out vec2 size;
out float maxAngle;
out float maxDistance;

void main() {
    vec3 vertexPos = Position;
    float Angle = NormalizedAngle * 6.28318530718;
    vertexPos.z = clamp(vertexPos.z, min(cos(Angle), 0), 1);
    float angleTerm = sin(min(Angle, 1.57079633)) * Distance;
    vertexPos *= vec3(Size.x + angleTerm, Size.y + angleTerm, Distance);

    // awful fix but not sure why just multiplying the matrix doesnt work? it does what it should in
    // all the branch calculations. really weird!
    vec3 lightPos = LightMatrix[3].xyz;
    mat3 rotationMatrix = mat3(LightMatrix);
    lightPos = inverse(rotationMatrix) * lightPos;
    vertexPos = inverse(rotationMatrix) * vertexPos;
    vertexPos += lightPos;
    gl_Position = VeilCamera.ProjMat * VeilCamera.ViewMat * vec4(vertexPos - VeilCamera.CameraPosition, 1.0);

    lightMat = LightMatrix;
    lightColor = Color;
    size = Size;
    maxAngle = Angle;
    maxDistance = Distance;
}
