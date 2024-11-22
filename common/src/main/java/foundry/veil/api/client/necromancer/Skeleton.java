package foundry.veil.api.client.necromancer;

import java.util.*;

public abstract class Skeleton<P extends SkeletonParent> {
    public List<Bone> roots;
    public Map<String, Bone> bones;

    public Skeleton() {
        this.roots = new ArrayList<>();
        this.bones = new HashMap<>();
    }

    public void tick() {
        for (Bone part : this.bones.values()) part.updatePreviousPosition();
        for (Bone bone : this.bones.values()) bone.tick(1.0F / 20.0F);
    }

    public void addBone(Bone part) {
        this.bones.put(part.identifier, part);
    }

    public void buildRoots() {
        for (Bone part : this.bones.values()) {
            if (part.parent == null) {
                roots.add(part);
            } else {
                Bone parentBone = part.parent;
                while (parentBone != null) {
                    part.parentChain.add(parentBone);
                    parentBone = parentBone.parent;
                }
                Collections.reverse(part.parentChain);
            }
        }
    }
}
