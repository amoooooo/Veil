package foundry.veil.api.client.necromancer.animation.keyframed;

import net.minecraft.util.Mth;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;

public class Interpolations {
    private static final Quaternionf qTempA = new Quaternionf(), qTempB = new Quaternionf(), qTempC = new Quaternionf(), qTempD = new Quaternionf(), qTempE = new Quaternionf();
    private static final AxisAngle4f aaTempA = new AxisAngle4f(), aaTempB = new AxisAngle4f(), aaTempC = new AxisAngle4f(), aaTempD = new AxisAngle4f();

    private interface InterpolationFunction {
        BoneTransform compose(float delta, BoneTransform prevPrev, BoneTransform prev, BoneTransform next, BoneTransform nextNext);
    }
    public enum InterpolationType {
        // todo: all interpolation types current assume uniform spacing...
        // additionally - scaling should be multiplicative, but isn't yet.
        STEP((delta, prevPrev, prev, next, nextNext) -> delta > 0.5F ? next : prev),
        LINEAR((delta, prevPrev, prev, next, nextNext) -> {
            float x = Mth.lerp(delta, prev.x(), next.x());
            float y = Mth.lerp(delta, prev.y(), next.y());
            float z = Mth.lerp(delta, prev.z(), next.z());
            float sx = Mth.lerp(delta, prev.sx(), next.sx());
            float sy = Mth.lerp(delta, prev.sy(), next.sy());
            float sz = Mth.lerp(delta, prev.sz(), next.sz());
            qTempA.set(prev.qx(), prev.qy(), prev.qz(), prev.qw());
            qTempB.set(next.qx(), next.qy(), next.qz(), next.qw());
            qTempA.slerp(qTempB, delta);
            return new BoneTransform(x, y, z, qTempA.x, qTempA.y, qTempA.z, qTempA.w, sx, sy, sz);
        }),
        CUBIC((delta, prevPrev, prev, next, nextNext) -> {
            float x = Mth.catmullrom(delta, prevPrev.x(), prev.x(), next.x(), nextNext.x());
            float y = Mth.catmullrom(delta, prevPrev.y(), prev.y(), next.y(), nextNext.y());
            float z = Mth.catmullrom(delta, prevPrev.z(), prev.z(), next.z(), nextNext.z());
            float sx = Mth.catmullrom(delta, prevPrev.sx(), prev.sx(), next.sx(), nextNext.sx());
            float sy = Mth.catmullrom(delta, prevPrev.sy(), prev.sy(), next.sy(), nextNext.sy());
            float sz = Mth.catmullrom(delta, prevPrev.sz(), prev.sz(), next.sz(), nextNext.sz());
            qTempA.set(prevPrev.qx(), prevPrev.qy(), prevPrev.qz(), prevPrev.qw());
            qTempB.set(prev.qx(), prev.qy(), prev.qz(), prev.qw());
            qTempC.set(next.qx(), next.qy(), next.qz(), next.qw());
            qTempD.set(nextNext.qx(), nextNext.qy(), nextNext.qz(), nextNext.qw());
            qTempE.set(quaternionCatmullRom(delta, qTempA, qTempB, qTempC, qTempD, qTempE));
            return new BoneTransform(x, y, z, qTempE.x, qTempE.y, qTempE.z, qTempE.w, sx, sy, sz);
        });

        final InterpolationFunction function;
        InterpolationType(InterpolationFunction function) {
            this.function = function;
        }
    }

    public static AxisAngle4f aaMul(AxisAngle4f aa, float scalar) {
        return aa.set(aa.angle * scalar, aa.x, aa.y, aa.z);
    }

    public static AxisAngle4f aaAdd(AxisAngle4f... aas) {
        float angle = 0, x = 0, y = 0, z = 0;
        for (AxisAngle4f aa : aas) {
            angle += aa.angle;
            x += aa.x;
            y += aa.y;
            z += aa.z;
        }
        return aas[0].set(angle, x, y, z).normalize();
    }

    // source : https://theorangeduck.com/page/cubic-interpolation-quaternions
    public static Quaternionf quaternionHermite(float delta, Quaternionfc r0, AxisAngle4f v0, Quaternionfc r1, AxisAngle4f v1, Quaternionf dest) {
        float w1 = 3 * delta * delta - 2 * delta * delta * delta;
        float w2 = delta * delta * delta - 2 * delta * delta + delta;
        float w3 = delta * delta * delta - delta * delta;
        AxisAngle4f r1_sub_r0 = aaTempA.set(r1.mul(r0.invert(dest), dest));
        return dest.set(aaAdd(aaMul(r1_sub_r0, w1), aaMul(v0, w2), aaMul(v1, w3))).mul(r0);
    }
    public static Quaternionf quaternionCatmullRom(float delta, Quaternionfc r0, Quaternionfc r1, Quaternionfc r2, Quaternionfc r3, Quaternionf dest) {
        AxisAngle4f r1_sub_r0 = aaTempB.set(r1.mul(r0.invert(dest), dest));
        AxisAngle4f r2_sub_r1 = aaTempC.set(r2.mul(r1.invert(dest), dest));
        AxisAngle4f r3_sub_r2 = aaTempD.set(r3.mul(r2.invert(dest), dest));
        AxisAngle4f v1 = aaMul(aaAdd(r1_sub_r0, r2_sub_r1), 0.5F);
        AxisAngle4f v2 = aaMul(aaAdd(r2_sub_r1, r3_sub_r2), 0.5F);
        return quaternionHermite(delta, r1, v1, r2, v2, dest);
    }
}

