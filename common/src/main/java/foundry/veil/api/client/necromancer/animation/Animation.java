package foundry.veil.api.client.necromancer.animation;

import foundry.veil.api.client.necromancer.Skeleton;
import foundry.veil.api.client.necromancer.SkeletonParent;

public abstract class Animation<P extends SkeletonParent<?, ?>, S extends Skeleton> {

    public boolean running(P parent, S skeleton, float mixFactor, float time) {
        return mixFactor > 0;
    }

    public void apply(P parent, S skeleton, float mixFactor, float time) {
    }
}
