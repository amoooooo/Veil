package foundry.veil.api.client.necromancer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import foundry.veil.api.client.necromancer.render.Skin;
import foundry.veil.api.client.necromancer.render.mesh.Mesh;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class Bone {
    public float x, y, z, pX, pY, pZ;
    public Quaternionf rotation, pRotation;
    protected Quaternionf currentRotation;
    public float xSize, ySize, zSize, pXSize, pYSize, pZSize;

    public float initialX, initialY, initialZ;
    public Quaternionf initialRotation;
    public float initialXSize, initialYSize, initialZSize;

    @Nullable
    public Bone parent;
    public List<Bone> children;

    public final String identifier;
    public boolean shouldRender = true;

    // list of all parents, starting from the root and going down
    public List<Bone> parentChain;

    public Bone(String identifier) {
        this.identifier = identifier;

        this.rotation = new Quaternionf();
        this.pRotation = new Quaternionf();
        this.currentRotation = new Quaternionf();
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
        this.currentRotation.set(this.initialRotation);
    }

    public void reset() {
        this.x = this.initialX;
        this.y = this.initialY;
        this.z = this.initialZ;
        this.rotation.set(this.initialRotation);
        this.xSize = this.initialXSize;
        this.ySize = this.initialYSize;
        this.zSize = this.initialZSize;
    }

    protected void updatePreviousPosition() {
        this.pX = this.x;
        this.pY = this.y;
        this.pZ = this.z;
        this.pRotation.set(this.rotation);
        this.pXSize = this.xSize;
        this.pYSize = this.ySize;
        this.pZSize = this.zSize;
    }

    public void setGlobalSpaceRotation(Quaternionf globalSpaceRotation) {
        Quaternionf parentRotation = new Quaternionf();

        //add together the rotations of all parents.
        for (Bone bone : this.parentChain) {
            parentRotation.mul(bone.rotation);
        }

        //subtract that from the global space rotation
        //and set the current rotation to the result of that.
        parentRotation.difference(globalSpaceRotation, this.rotation);
    }

    public void setModelSpaceTransform(Vector3f position, Quaternionf rotation) {
        Matrix4f parentTransform = new Matrix4f();

        for (Bone bone : this.parentChain) {
            parentTransform.translate(bone.x, bone.y, bone.z);
            parentTransform.rotate(bone.rotation);
            parentTransform.scale(bone.xSize, bone.ySize, bone.zSize);
        }

        Matrix4f modelSpaceBoneTransform = new Matrix4f().translate(position).rotate(rotation);
        modelSpaceBoneTransform.mul(parentTransform.invert());

        // position = modelSpaceBoneTransform.transformPosition(new Vector3f(0, 0, 0));
        // rotation = modelSpaceBoneTransform.getNormalizedRotation(new Quaternionf());

    }

    protected void tick(float deltaTime) {}

    public void transform(PoseStack pPoseStack, float partialTick) {
        pPoseStack.translate(Mth.lerp(partialTick, pX, x), Mth.lerp(partialTick, pY, y), Mth.lerp(partialTick, pZ, z));
        this.currentRotation = pRotation.slerp(rotation, partialTick, currentRotation);
        this.currentRotation.normalize();
        pPoseStack.mulPose(this.currentRotation);
        pPoseStack.scale(Mth.lerp(partialTick, pXSize, xSize), Mth.lerp(partialTick, pYSize, ySize), Mth.lerp(partialTick, pZSize, zSize));
    }

    public void transform(Matrix4f matrix4f, float partialTick) {
        matrix4f.translate(Mth.lerp(partialTick, pX, x), Mth.lerp(partialTick, pY, y), Mth.lerp(partialTick, pZ, z));
        this.currentRotation = pRotation.slerp(rotation, partialTick, currentRotation);
        this.currentRotation.normalize();
        matrix4f.rotate(this.currentRotation);
        matrix4f.scale(Mth.lerp(partialTick, pXSize, xSize), Mth.lerp(partialTick, pYSize, ySize), Mth.lerp(partialTick, pZSize, zSize));
    }

    public void render(Skin skin, float partialTick, PoseStack pPoseStack, VertexConsumer pVertexConsumer, int pPackedLight, int pPackedOverlay, float pRed, float pGreen, float pBlue, float pAlpha, boolean drawChildren) {
        if (!shouldRender) return;
        Mesh mesh = skin.getMesh(this);

        pPoseStack.pushPose();

        this.transform(pPoseStack, partialTick);
        mesh.render(pPoseStack, pVertexConsumer, pPackedLight, pPackedOverlay, pRed, pGreen, pBlue, pAlpha);

        if (drawChildren) {
            for (Bone child : this.children) {
                child.render(skin, partialTick, pPoseStack, pVertexConsumer, pPackedLight, pPackedOverlay, pRed, pGreen, pBlue, pAlpha, true);
            }
        }

        pPoseStack.popPose();
    }

    public void addChild(Bone children) {
        if (children.parent != null) {
            children.parent.children.remove(children);
        }

        this.children.add(children);
        children.parent = this;
    }

    public void setParent(Bone parent) {
        this.parent = parent;
        parent.children.add(this);
    }

    public Matrix4f getModelSpaceTransformMatrix(PoseStack pPoseStack, float partialTick) {
        Bone parent = this.parent;
        if (parent != null) {
            parent.getModelSpaceTransformMatrix(pPoseStack, partialTick);
        }
        this.transform(pPoseStack, partialTick);

        return pPoseStack.last().pose();
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
