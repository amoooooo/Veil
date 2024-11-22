package foundry.veil.api.client.necromancer.animation.keyframed;

import foundry.veil.api.client.necromancer.Bone;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

public record BoneTransform(float x, float y, float z, float qx, float qy, float qz, float qw, float sx, float sy, float sz) {
    private static final Quaternionf tempA = new Quaternionf();
    private static final Quaternionf tempB = new Quaternionf();
    void apply(Bone bone, float factor, boolean additive) {
        if (additive) {
            bone.x += this.x * factor;
            bone.y += this.y * factor;
            bone.z += this.z * factor;
            bone.xSize *= Mth.lerp(factor, 1, this.sx);
            bone.ySize *= Mth.lerp(factor, 1, this.sy);
            bone.zSize *= Mth.lerp(factor, 1, this.sz);
            tempA.set(this.qx, this.qy, this.qz, this.qw);
            tempA.slerp(tempB.identity(), 1 - factor);
            bone.rotation.premul(tempA);
        } else {
            bone.x = Mth.lerp(factor, bone.x, this.x);
            bone.y = Mth.lerp(factor, bone.y, this.y);
            bone.z = Mth.lerp(factor, bone.z, this.z);
            bone.xSize = Mth.lerp(factor, bone.xSize, this.sx);
            bone.ySize = Mth.lerp(factor, bone.ySize, this.sy);
            bone.zSize = Mth.lerp(factor, bone.zSize, this.sz);
            tempA.set(this.qx, this.qy, this.qz, this.qw);
            bone.rotation.slerp(tempA, factor);
        }
    }

    static BoneTransform compose(BoneTransform a, BoneTransform b, float factor) {
        float x = Mth.lerp(factor, a.x, b.x);
        float y = Mth.lerp(factor, a.y, b.y);
        float z = Mth.lerp(factor, a.z, b.z);
        float sx = Mth.lerp(factor, a.sx, b.sx);
        float sy = Mth.lerp(factor, a.sy, b.sy);
        float sz = Mth.lerp(factor, a.sz, b.sz);
        tempA.set(a.qx, a.qy, a.qz, a.qw);
        tempB.set(b.qx, b.qy, b.qz, b.qw);
        tempA.slerp(tempB, factor);

        return new BoneTransform(x, y, z, tempA.x, tempA.y, tempA.z, tempA.w, sx, sy, sz);
    }
}
