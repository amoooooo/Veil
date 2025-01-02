package foundry.veil.api.client.necromancer.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import foundry.veil.api.client.necromancer.Bone;
import foundry.veil.api.client.necromancer.Skeleton;
import foundry.veil.api.client.necromancer.render.mesh.Mesh;

import java.util.HashMap;
import java.util.Map;

// TEMPORARY STUFF
// HOLDOVER FROM OLD GRAVEYARD
// probably won't exist in the future - will just be a Mesh.
public class Skin<T extends Skeleton<?>> {
    final Map<String, Mesh> meshes;

    public Skin() {
        this.meshes = new HashMap<>();
    }

    public void addMesh(String boneID, Mesh mesh) {
        this.meshes.put(boneID, mesh);
    }

    public Mesh getMesh(Bone bone) {
        return this.meshes.getOrDefault(bone.identifier, Mesh.EMPTY);
    }

    public void render(T skeleton, int ticksExisted, float partialTick, PoseStack pPoseStack, VertexConsumer pVertexConsumer, int pPackedLight, int pPackedOverlay, float pRed, float pGreen, float pBlue, float pAlpha) {
        for (Map.Entry<String, Mesh> stringMeshEntry : this.meshes.entrySet()) {
            Mesh mesh = stringMeshEntry.getValue();
        }
        for (Bone bone : skeleton.roots) {
            bone.render(this, partialTick, pPoseStack, pVertexConsumer, pPackedLight, pPackedOverlay, pRed, pGreen, pBlue, pAlpha, true);
        }
    }
}
