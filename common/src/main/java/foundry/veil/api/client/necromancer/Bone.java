package foundry.veil.api.client.necromancer;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4x3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

public class Bone {

    public float x, y, z, pX, pY, pZ;
    public Quaternionf rotation, pRotation;
    public float xSize, ySize, zSize, pXSize, pYSize, pZSize;

    public float initialX, initialY, initialZ;
    public Quaternionf initialRotation;
    public float initialXSize, initialYSize, initialZSize;

    public Vector4f pColor;
    public Vector4f color;
    public Vector4f initialColor;

    @Nullable
    public Bone parent;
    public List<Bone> children;

    public final String identifier;

    // list of all parents, starting from the root and going down
    public List<Bone> parentChain;

    public Bone(String identifier) {
        this.identifier = identifier;

        this.rotation = new Quaternionf();
        this.pRotation = new Quaternionf();
        this.initialRotation = new Quaternionf();

        this.xSize = 1.0F;
        this.ySize = 1.0F;
        this.zSize = 1.0F;
        this.pXSize = 1.0F;
        this.pYSize = 1.0F;
        this.pZSize = 1.0F;
        this.initialXSize = 1.0F;
        this.initialYSize = 1.0F;
        this.initialZSize = 1.0F;

        this.pColor = new Vector4f(1.0F);
        this.color = new Vector4f(1.0F);
        this.initialColor = new Vector4f(1.0F);

        this.children = new ArrayList<>();
        this.parentChain = new ArrayList<>();
    }

    public void setInitialTransform(float x, float y, float z, Quaternionf rotation) {
        this.initialX = x;
        this.initialY = y;
        this.initialZ = z;
        this.x = this.initialX;
        this.y = this.initialY;
        this.z = this.initialZ;
        this.pX = this.initialX;
        this.pY = this.initialY;
        this.pZ = this.initialZ;
        this.initialRotation.set(rotation);
        this.rotation.set(this.initialRotation);
        this.pRotation.set(this.initialRotation);
    }

    public void reset() {
        this.x = this.initialX;
        this.y = this.initialY;
        this.z = this.initialZ;
        this.rotation.set(this.initialRotation);
        this.xSize = this.initialXSize;
        this.ySize = this.initialYSize;
        this.zSize = this.initialZSize;
        this.color.set(this.initialColor);
    }

    protected void updatePreviousPosition() {
        this.pX = this.x;
        this.pY = this.y;
        this.pZ = this.z;
        this.pRotation.set(this.rotation);
        this.pXSize = this.xSize;
        this.pYSize = this.ySize;
        this.pZSize = this.zSize;
        this.pColor.set(this.color);
    }

    public Matrix4x3f getModelTransform(Matrix4x3f matrix, Quaternionf orientation, float partialTicks) {
        for (Bone bone : this.parentChain) {
            bone.getLocalTransform(matrix, orientation, partialTicks);
        }
        this.getLocalTransform(matrix, orientation, partialTicks);
        return matrix;
    }

    public Matrix4x3f getModelTransform(Matrix4x3f matrix, float partialTicks) {
        return this.getModelTransform(matrix, new Quaternionf(), partialTicks);
    }

    public void getLocalTransform(Matrix4x3f matrix, Quaternionf orientation, float partialTicks) {
        matrix.translate(Mth.lerp(partialTicks, this.pX, this.x), Mth.lerp(partialTicks, this.pY, this.y), Mth.lerp(partialTicks, this.pZ, this.z));
        this.pRotation.slerp(this.rotation, partialTicks, orientation);
        matrix.rotate(orientation.normalize());
        matrix.scale(Mth.lerp(partialTicks, this.pXSize, this.xSize), Mth.lerp(partialTicks, this.pYSize, this.ySize), Mth.lerp(partialTicks, this.pZSize, this.zSize));
    }

    public void getLocalTransform(Matrix4x3f matrix, float partialTicks) {
        this.getLocalTransform(matrix, new Quaternionf(), partialTicks);
    }

    public void getColor(Vector4f color, float partialTicks) {
        this.pColor.lerp(this.color, partialTicks, color);
    }

    protected void tick(float deltaTime) {
    }

//    public void transform(Matrix4f matrix4f, float partialTick) {
//        matrix4f.translate(Mth.lerp(partialTick, this.pX, this.x), Mth.lerp(partialTick, this.pY, this.y), Mth.lerp(partialTick, this.pZ, this.z));
//        this.currentRotation = this.pRotation.slerp(this.rotation, partialTick, this.currentRotation);
//        this.currentRotation.normalize();
//        matrix4f.rotate(this.currentRotation);
//        matrix4f.scale(Mth.lerp(partialTick, this.pXSize, this.xSize), Mth.lerp(partialTick, this.pYSize, this.ySize), Mth.lerp(partialTick, this.pZSize, this.zSize));
//    }

//    public void render(Skin skin, float partialTick, PoseStack pPoseStack, VertexConsumer pVertexConsumer, int pPackedLight, int pPackedOverlay, float pRed, float pGreen, float pBlue, float pAlpha, boolean drawChildren) {
//        if (!shouldRender) return;
//        Mesh mesh = skin.getMesh(this);
//
//        pPoseStack.pushPose();
//
//        this.transform(pPoseStack, partialTick);
//        mesh.render(pPoseStack, pVertexConsumer, pPackedLight, pPackedOverlay, pRed, pGreen, pBlue, pAlpha);
//
//        if (drawChildren) {
//            for (Bone child : this.children) {
//                child.render(skin, partialTick, pPoseStack, pVertexConsumer, pPackedLight, pPackedOverlay, pRed, pGreen, pBlue, pAlpha, true);
//            }
//        }
//
//        pPoseStack.popPose();
//    }

    public void addChild(Bone child) {
        if (child.parent != null) {
            child.parent.children.remove(child);
        }

        this.children.add(child);
        child.parent = this;
    }

    public void setParent(Bone parent) {
        this.parent = parent;
        parent.children.add(this);
    }

    public void rotate(float angle, Direction.Axis axis) {
        switch (axis) {
            case X -> this.rotation.rotateX(angle);
            case Y -> this.rotation.rotateY(angle);
            case Z -> this.rotation.rotateZ(angle);
        }
    }

    public void rotateDeg(float angle, Direction.Axis axis) {
        switch (axis) {
            case X -> this.rotation.rotateX(angle * Mth.DEG_TO_RAD);
            case Y -> this.rotation.rotateY(angle * Mth.DEG_TO_RAD);
            case Z -> this.rotation.rotateZ(angle * Mth.DEG_TO_RAD);
        }
    }
}
