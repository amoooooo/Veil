package foundry.veil.api.client.util;

import com.mojang.blaze3d.platform.Window;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@Deprecated
public class SpaceHelper {
    //    public static Vector3f getNormalizedDeviceCoordsFromViewport(float x, float y, float z, float width, float height) {
//        float rx = (2.0f * x) / width - 1.0f;
//        float ry = 1.0f - (2.0f * y) / height;
//        return new Vector3f(rx, ry, z);
//    }
//
//    public static Vector3f getViewPortCoordFromNDC(Vector3f ndc) {
//        return new Vector3f((ndc.x() + 1.0f) / 2.0f, (1.0f - ndc.y()) / 2.0f, ndc.z());
//    }
//
//    public static Vector4f getClipCoordsFromNDC(Vector3f ndc) {
//        return new Vector4f(ndc.x(), ndc.y(), -1.0f, 1.0f);
//    }
//
//    public static Vector4f getNDCFromClip(Vector4f clipCoords) {
//        clipCoords.perspectiveDivide();
//        return clipCoords;
//
//    }
//
//    public static Vector4f getClipFromEye(Vector4f eyeCoords) {
//        // transform by projection matrix
//        Matrix4f projMatrix = RenderSystem.getProjectionMatrix().copy();
//        eyeCoords.transform(projMatrix);
//        return eyeCoords;
//    }
//
//    public static Vector4f getEyeFromWorld(Vector4f worldCoords) {
//        // transform by view matrix
//        Matrix4f viewMatrix = RenderSystem.getModelViewMatrix().copy();
//        worldCoords.transform(viewMatrix);
//        return worldCoords;
//    }
//
//    public static Vector4f getEyeCoordsFromClip(Vector4f clipCoords) {
//        Matrix4f invProjMatrix = RenderSystem.getProjectionMatrix().copy();
//        invProjMatrix.invert();
//        clipCoords.transform(invProjMatrix);
//        return new Vector4f(clipCoords.x(), clipCoords.y(), -1.0f, 0.0f);
//    }
//
//    public static Vector3f getWorldCoordsFromEye(Vector4f eyeCoords) {
//        Matrix4f invViewMatrix = RenderSystem.getModelViewMatrix().copy();
//        invViewMatrix.invert();
//        eyeCoords.transform(invViewMatrix);
//        return new Vector3f(eyeCoords.x(), eyeCoords.y(), eyeCoords.z());
//    }
//
//    public static Vector3f screenToWorldCoords(Vector3f worldCoords, float width, float height) {
//        Vector3f ndc = getNormalizedDeviceCoordsFromViewport(worldCoords.x(), worldCoords.y(), worldCoords.z(), width, height);
//        Vector4f clipCoords = getClipCoordsFromNDC(ndc);
//        Vector4f eyeCoords = getEyeCoordsFromClip(clipCoords);
//        Vector3f screenCoords = getWorldCoordsFromEye(eyeCoords);
//        return new Vector3f(screenCoords.x(), screenCoords.y(), screenCoords.z());
//    }
//
//    public static Vector3f worldToScreenCoords(Vector3f worldCoords) {
//        Vector4f worldCoords4f = new Vector4f(worldCoords.x(), worldCoords.y(), worldCoords.z(), 1.0f);
//        Vector4f eyeCoords = getEyeFromWorld(worldCoords4f);
//        Vector4f clipCoords = getClipFromEye(eyeCoords);
//        Vector4f ndc = getNDCFromClip(clipCoords);
//        Vector3f screenCoords = getViewPortCoordFromNDC(new Vector3f(ndc.x(), ndc.y(), ndc.z()));
//        return new Vector3f(screenCoords.x(), screenCoords.y(), screenCoords.z());
//    }
//
//    public Vector4f convertMatrixToVector4f(Matrix4f matrix) {
//        return new Vector4f(matrix.m03, matrix.m13, matrix.m23, matrix.m33);
//    }
//
//    public static Matrix4f invertProjection(Matrix4f input) {
//        Matrix4f result = input.copy();
////        Matrix4fAccessor inputAccessor = (Matrix4fAccessor) (Object) Objects.requireNonNull(input);
////        Matrix4fAccessor resultAccessor = (Matrix4fAccessor) (Object) Objects.requireNonNull(result);
////        float a = 1.0f / (inputAccessor.m00() * inputAccessor.m11());
////        float l = -1.0f / (inputAccessor.m23() * inputAccessor.m32());
////        resultAccessor.m00(inputAccessor.m11() * a);
////        resultAccessor.m01(0);
////        resultAccessor.m02(0);
////        resultAccessor.m03(0);
////        resultAccessor.m10(0);
////        resultAccessor.m11(inputAccessor.m00() * a);
////        resultAccessor.m12(0);
////        resultAccessor.m13(0);
////        resultAccessor.m20(0);
////        resultAccessor.m21(0);
////        resultAccessor.m22(-inputAccessor.m32() * l);
////        resultAccessor.m23(inputAccessor.m22() * l);
//        return result;
//    }
//
//    public static Matrix4f invertGeneric(Matrix4f input) {
//        Matrix4f result = input.copy();
////        Matrix4fAccessor inputAccessor = (Matrix4fAccessor) (Object) Objects.requireNonNull(input);
////        Matrix4fAccessor resultAccessor = (Matrix4fAccessor) (Object) Objects.requireNonNull(result);
////        float a = inputAccessor.m00() * inputAccessor.m11() - inputAccessor.m01() * inputAccessor.m10();
////        float b = inputAccessor.m00() * inputAccessor.m12() - inputAccessor.m02() * inputAccessor.m10();
////        float c = inputAccessor.m00() * inputAccessor.m13() - inputAccessor.m03() * inputAccessor.m10();
////        float d = inputAccessor.m01() * inputAccessor.m12() - inputAccessor.m02() * inputAccessor.m11();
////        float e = inputAccessor.m01() * inputAccessor.m13() - inputAccessor.m03() * inputAccessor.m11();
////        float f = inputAccessor.m02() * inputAccessor.m13() - inputAccessor.m03() * inputAccessor.m12();
////        float g = inputAccessor.m20() * inputAccessor.m31() - inputAccessor.m21() * inputAccessor.m30();
////        float h = inputAccessor.m20() * inputAccessor.m32() - inputAccessor.m22() * inputAccessor.m30();
////        float i = inputAccessor.m20() * inputAccessor.m33() - inputAccessor.m23() * inputAccessor.m30();
////        float j = inputAccessor.m21() * inputAccessor.m32() - inputAccessor.m22() * inputAccessor.m31();
////        float k = inputAccessor.m21() * inputAccessor.m33() - inputAccessor.m23() * inputAccessor.m31();
////        float l = inputAccessor.m22() * inputAccessor.m33() - inputAccessor.m23() * inputAccessor.m32();
////        float det = a * l - b * k + c * j + d * i - e * h + f * g;
////        det = 1.0f / det;
////        resultAccessor.m00(fma(inputAccessor.m11(), l, fma(-inputAccessor.m12(), k, inputAccessor.m13() * j)) * det);
////        resultAccessor.m01(fma(-inputAccessor.m01(), l, fma(inputAccessor.m02(), k, -inputAccessor.m03() * j)) * det);
////        resultAccessor.m02(fma(inputAccessor.m31(), f, fma(-inputAccessor.m32(), e, inputAccessor.m33() * d)) * det);
////        resultAccessor.m03(fma(-inputAccessor.m21(), f, fma(inputAccessor.m22(), e, -inputAccessor.m23() * d)) * det);
////        resultAccessor.m10(fma(-inputAccessor.m10(), l, fma(inputAccessor.m12(), i, -inputAccessor.m13() * h)) * det);
////        resultAccessor.m11(fma(inputAccessor.m00(), l, fma(-inputAccessor.m02(), i, inputAccessor.m03() * h)) * det);
////        resultAccessor.m12(fma(-inputAccessor.m30(), f, fma(inputAccessor.m32(), c, -inputAccessor.m33() * b)) * det);
////        resultAccessor.m13(fma(inputAccessor.m20(), f, fma(-inputAccessor.m22(), c, inputAccessor.m23() * b)) * det);
////        resultAccessor.m20(fma(inputAccessor.m10(), k, fma(-inputAccessor.m11(), i, inputAccessor.m13() * g)) * det);
////        resultAccessor.m21(fma(-inputAccessor.m00(), k, fma(inputAccessor.m01(), i, -inputAccessor.m03() * g)) * det);
////        resultAccessor.m22(fma(inputAccessor.m30(), e, fma(-inputAccessor.m31(), c, inputAccessor.m33() * a)) * det);
////        resultAccessor.m23(fma(-inputAccessor.m20(), e, fma(inputAccessor.m21(), c, -inputAccessor.m23() * a)) * det);
////        resultAccessor.m30(fma(-inputAccessor.m10(), j, fma(inputAccessor.m11(), h, -inputAccessor.m12() * g)) * det);
////        resultAccessor.m31(fma(inputAccessor.m00(), j, fma(-inputAccessor.m01(), h, inputAccessor.m02() * g)) * det);
////        resultAccessor.m32(fma(-inputAccessor.m30(), d, fma(inputAccessor.m31(), b, -inputAccessor.m32() * a)) * det);
////        resultAccessor.m33(fma(inputAccessor.m20(), d, fma(-inputAccessor.m21(), b, inputAccessor.m22() * a)) * det);
//        return result;
//    }
//
//    private static float fma(float a, float b, float c) {
//        return a * b + c;
//    }
//
//    public static Quaternion restrictAxis(Vec3 v, Quaternion q) {
//        Quaternion q2 = q.copy();
//        q2.normalize();
//        Vec3 v2 = new Vec3(q2.i(), q2.j(), q2.k());
//        double D = v.dot(v2);
//        double den = Math.sqrt(D * D + q2.r() * q2.r());
//        double real = q2.r() / den;
//        double imag = D / den;
//        Vec3 f = v.scale(imag);
//        return new Quaternion((float) f.x, (float) f.y, (float) f.z, (float) real);
//    }
//
    public static Vector3f worldToScreenSpace(Vec3 pos, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPosition = camera.getPosition();

        Vector3f position = new Vector3f((float) (cameraPosition.x - pos.x), (float) (cameraPosition.y - pos.y), (float) (cameraPosition.z - pos.z));
        Quaternionf cameraRotation = camera.rotation();
        cameraRotation.conjugate();
        //cameraRotation = restrictAxis(new Vec3(1, 1, 0), cameraRotation);
        cameraRotation.transform(position);

        // Account for view bobbing
        if (mc.options.bobView().get() && mc.getCameraEntity() instanceof Player) {
            Player player = (Player) mc.getCameraEntity();
            float playerStep = player.walkDist - player.walkDistO;
            float stepSize = -(player.walkDist + playerStep * partialTicks);
            float viewBob = Mth.lerp(partialTicks, player.oBob, player.bob);

            Quaternionf bobXRotation = Axis.XP.rotationDegrees(Math.abs(Mth.cos(stepSize * (float) Math.PI - 0.2f) * viewBob) * 5f);
            Quaternionf bobZRotation = Axis.ZP.rotationDegrees(Mth.sin(stepSize * (float) Math.PI) * viewBob * 3f);
            bobXRotation.conjugate();
            bobZRotation.conjugate();
            bobXRotation.transform(position);
            bobZRotation.transform(position);
            position.add(Mth.sin(stepSize * (float) Math.PI) * viewBob * 0.5f, Math.abs(Mth.cos(stepSize * (float) Math.PI) * viewBob), 0f);
        }

        Window window = mc.getWindow();
        float screenSize = window.getGuiScaledHeight() / 2f / position.z() / (float) Math.tan(Math.toRadians(mc.gameRenderer.getFov(camera, partialTicks, true) / 2f));
        position.mul(-screenSize, -screenSize, 1f);
        position.add(window.getGuiScaledWidth() / 2f, window.getGuiScaledHeight() / 2f, 0f);

        return position;
    }
//
//    public static Vec3 clipNormalizedDir(Vec3 normalizeDir) {
//        // remove the lowest axis
//        return new Vec3(normalizeDir.x, 0, normalizeDir.z);
//    }
}
