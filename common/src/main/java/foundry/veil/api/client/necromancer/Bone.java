package foundry.veil.api.client.necromancer;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.*;

import java.util.ArrayList;
import java.util.List;

public class Bone {

    public Vector3f position, previousPosition, basePosition;
    public Quaternionf rotation, previousRotation, baseRotation;
    public Vector3f size, previousSize, baseSize;
    public Vector4f color, previousColor, baseColor;

    @Nullable
    public Bone parent;
    public List<Bone> children;

    public final String identifier;

    // list of all parents, starting from the root and going down
    public List<Bone> parentChain;

    public Bone(String identifier) {
        this.identifier = identifier;

        this.position = new Vector3f(0.0F);
        this.previousPosition = new Vector3f(0.0F);
        this.basePosition = new Vector3f(0.0F);

        this.rotation = new Quaternionf();
        this.previousRotation = new Quaternionf();
        this.baseRotation = new Quaternionf();

        this.size = new Vector3f(1.0F);
        this.previousSize = new Vector3f(1.0F);
        this.baseSize = new Vector3f(1.0F);

        this.color = new Vector4f(1.0F);
        this.previousColor = new Vector4f(1.0F);
        this.baseColor = new Vector4f(1.0F);

        this.children = new ArrayList<>();
        this.parentChain = new ArrayList<>();
    }

    public void setBaseAttributes(Vector3fc pos, Quaternionfc rotation, Vector3fc scale, Vector4fc color) {
        this.basePosition.set(pos);
        this.position.set(this.basePosition);
        this.previousPosition.set(this.basePosition);

        this.baseSize.set(scale);
        this.size.set(this.baseSize);
        this.previousSize.set(this.baseSize);

        this.baseRotation.set(rotation);
        this.rotation.set(this.baseRotation);
        this.previousRotation.set(this.baseRotation);

        this.baseColor.set(color);
        this.color.set(this.baseColor);
        this.previousColor.set(this.baseColor);
    }

    public void reset() {
        this.position.set(this.basePosition);
        this.rotation.set(this.baseRotation);
        this.size.set(this.baseSize);
        this.color.set(this.baseColor);
    }

    protected void updatePreviousAttributes() {
        this.previousPosition.set(this.position);
        this.previousRotation.set(this.rotation);
        this.previousSize.set(this.size);
        this.previousColor.set(this.color);
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
        matrix.translate(
                Mth.lerp(partialTicks, this.previousPosition.x, this.position.x),
                Mth.lerp(partialTicks, this.previousPosition.y, this.position.y),
                Mth.lerp(partialTicks, this.previousPosition.z, this.position.z)
        );

        this.previousRotation.slerp(this.rotation, partialTicks, orientation);
        matrix.rotate(orientation.normalize());

        // technically wrong but whatever
        matrix.scale(
                Mth.lerp(partialTicks, this.previousSize.x, this.size.x),
                Mth.lerp(partialTicks, this.previousSize.y, this.size.y),
                Mth.lerp(partialTicks, this.previousSize.z, this.size.z)
        );
    }

    public void getLocalTransform(Matrix4x3f matrix, float partialTicks) {
        this.getLocalTransform(matrix, new Quaternionf(), partialTicks);
    }

    public void getColor(Vector4f color, float partialTicks) {
        this.previousColor.lerp(this.color, partialTicks, color);
    }

    protected void tick(float deltaTime) {}

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
