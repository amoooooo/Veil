package foundry.veil.api.client.necromancer;


import foundry.veil.api.client.necromancer.animation.Animator;

public interface SkeletonParent<P extends SkeletonParent<?, ?>, S extends Skeleton<P>> {

    void setSkeleton(S skeleton);

    void setAnimator(Animator<P, S> animator);

    S getSkeleton();

    Animator<P, S> getAnimator();
}
