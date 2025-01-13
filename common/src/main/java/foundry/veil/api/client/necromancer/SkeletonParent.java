package foundry.veil.api.client.necromancer;


import foundry.veil.api.client.necromancer.animation.Animator;
import org.jetbrains.annotations.Nullable;

public interface SkeletonParent<P extends SkeletonParent<?, ?>, S extends Skeleton> {

    void setSkeleton(@Nullable S skeleton);

    void setAnimator(@Nullable Animator<P, S> animator);

    @Nullable S getSkeleton();

    @Nullable Animator<P, S> getAnimator();
}
