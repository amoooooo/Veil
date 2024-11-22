package foundry.veil.api.client.necromancer;


import foundry.veil.api.client.necromancer.animation.Animator;

public interface SkeletonParent<P extends SkeletonParent<?,?,?>, S extends Skeleton<P>, A extends Animator<P, S>> {
    void setSkeleton(S skeleton);
    void setAnimator(A animator);

    S getSkeleton();
    A getAnimator();
}
