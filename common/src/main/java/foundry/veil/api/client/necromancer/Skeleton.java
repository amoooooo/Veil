package foundry.veil.api.client.necromancer;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import org.joml.*;

import java.lang.Math;
import java.nio.ByteBuffer;
import java.util.*;

public abstract class Skeleton {

    public static final int MAX_BONES = 256;
    public static final int UNIFORM_STRIDE = 32 * Float.BYTES;

    public List<Bone> roots;
    public Map<String, Bone> bones;
    private int maxDepth;

    public Skeleton() {
        this.roots = new ArrayList<>();
        this.bones = new HashMap<>();
        this.maxDepth = 1;
    }

    public void tick() {
        for (Bone part : this.bones.values()) {
            part.updatePreviousAttributes();
        }
        for (Bone bone : this.bones.values()) {
            bone.tick(1.0F / 20.0F);
        }
    }

    public void addBone(Bone part) {
        this.bones.put(part.identifier, part);
    }

    public void buildRoots() {
        for (Bone part : this.bones.values()) {
            if (part.parent == null) {
                this.roots.add(part);
                continue;
            }

            Set<Bone> closedSet = new HashSet<>();
            Bone parentBone = part.parent;
            while (parentBone != null) {
                if (!closedSet.add(parentBone)) {
                    throw new IllegalStateException("Circular reference in bone: " + parentBone.identifier);
                }
                part.parentChain.add(parentBone);
                parentBone = parentBone.parent;
            }
            Collections.reverse(part.parentChain);
            this.maxDepth = Math.max(part.parentChain.size() + 1, this.maxDepth);
        }
    }

    /**
     * @return The maximum number of bone layers in this skeleton
     */
    public int getMaxDepth() {
        return this.maxDepth;
    }

    /**
     * Steps through the entire skeleton to store in the specified bone buffer.
     * @param buffer
     * @param bones
     * @param boneIds
     * @param depth
     * @param matrixStack
     * @param orientationStack
     */
    public void storeInstancedData(ByteBuffer buffer, Collection<Bone> bones, Object2IntMap<String> boneIds, int depth, Vector4f color, Matrix3f normalMatrix, Matrix4x3f[] matrixStack, Quaternionf[] orientationStack, float partialTicks) {
        for (Bone bone : bones) {
            int id = boneIds.getInt(bone.identifier);
            boolean hasChildren = !bone.children.isEmpty();

            Matrix4x3f matrix = matrixStack[depth];
            Quaternionf orientation = orientationStack[depth];
            if (id != -1 || hasChildren) {
                bone.getLocalTransform(matrix, orientation, partialTicks);
                if (id != -1) {
                    // Turns this into 3 columns, so in the shader we can use the last column for colors
                    matrix.getTransposed(id * UNIFORM_STRIDE, buffer);
                    bone.getColor(color, partialTicks);
                    color.get(id * UNIFORM_STRIDE + 12 * Float.BYTES, buffer);
                    // Workaround for a JOML bug with get3x4
                    new Matrix4f().set(matrix.normal(normalMatrix)).get(id * UNIFORM_STRIDE + 16 * Float.BYTES, buffer);
                }
            }

            if (hasChildren) {
                // Copy from current depth to the next
                matrixStack[depth + 1].set(matrix);
                orientationStack[depth + 1].set(orientation);
                this.storeInstancedData(buffer, bones, boneIds, depth + 1, color, normalMatrix, matrixStack, orientationStack, partialTicks);
            }
        }
    }
}
