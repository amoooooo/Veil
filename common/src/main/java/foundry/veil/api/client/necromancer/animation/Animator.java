package foundry.veil.api.client.necromancer.animation;


import foundry.veil.api.client.necromancer.Skeleton;
import foundry.veil.api.client.necromancer.SkeletonParent;
import foundry.veil.api.client.necromancer.constraint.Constraint;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public abstract class Animator<P extends SkeletonParent<?, ?>, S extends Skeleton> {

    protected final P parent;
    protected final S skeleton;
    private final List<ConstraintEntry> constraints;
    private final List<AnimationEntry<P, S>> animations;

    protected Animator(P parent, S skeleton) {
        this.parent = parent;
        this.skeleton = skeleton;
        this.animations = new ArrayList<>();
        this.constraints = new ArrayList<>();
    }

    public void addConstraint(Constraint constraint, int priority) {
        this.constraints.add(new ConstraintEntry(constraint, priority));
        this.constraints.sort(Comparator.comparingInt(ConstraintEntry::priority));
    }

    public AnimationEntry<P, S> addAnimation(Animation<P, S> animation, int priority) {
        AnimationEntry<P, S> entry = new AnimationEntry<>(animation, priority);
        this.animations.add(entry);
        this.animations.sort(Comparator.comparingInt(animEntry -> animEntry.priority));
        return entry;
    }

    public TimedAnimationEntry<P, S> addTimedAnimation(Animation<P, S> animation, int priority, int animLength) {
        TimedAnimationEntry<P, S> entry = new TimedAnimationEntry<>(animation, priority, animLength);
        this.animations.add(entry);
        this.animations.sort(Comparator.comparingInt(animEntry -> animEntry.priority));
        return entry;
    }

    public void tick() {
        this.skeleton.tick();
        this.skeleton.bones.forEach((name, bone) -> bone.reset());
        this.animate();
        this.animations.forEach(animation -> animation.apply(this.parent, this.skeleton));
        this.constraints.forEach(constraintEntry -> constraintEntry.constraint.apply());
        this.animatePostConstraints();
    }

    public void animate() {
    }

    public void animatePostConstraints() {
    }

    record ConstraintEntry(Constraint constraint, int priority) {
    }

    public static class AnimationEntry<P extends SkeletonParent<?, ?>, S extends Skeleton> {

        protected final Animation<P, S> animation;
        protected final int priority;

        protected float mixFactor;
        protected float time;

        private AnimationEntry(Animation<P, S> animation, int priority) {
            this.animation = animation;
            this.priority = priority;
        }

        public float getMixFactor() {
            return this.mixFactor;
        }

        public void setMixFactor(float mixFactor) {
            this.mixFactor = mixFactor;
        }

        public float getTime() {
            return this.time;
        }

        public void setTime(float time) {
            this.time = time;
        }

        protected void apply(P parent, S skeleton) {
            if (!this.animation.running(parent, skeleton, this.mixFactor, this.time)) {
                return;
            }
            this.animation.apply(parent, skeleton, this.mixFactor, this.time);
        }
    }

    public static class TimedAnimationEntry<P extends SkeletonParent<?, ?>, T extends Skeleton> extends AnimationEntry<P, T> {
        int lengthInTicks;
        boolean rewinding = false;
        public boolean playing = false;

        private TimedAnimationEntry(Animation<P, T> animation, int priority, int lengthInTicks) {
            super(animation, priority);
            this.setAnimLength(lengthInTicks);
        }

        public void setAnimLength(int lengthInTicks) {
            this.lengthInTicks = lengthInTicks;
        }

        public void begin() {
            this.time = 0;
            this.resume();
        }

        public void resume() {
            this.playing = true;
            if (this.animationEnded()) {
                this.time = 0;
            }
        }

        public void rewind() {
            this.rewinding = true;
        }

        public void stop() {
            this.playing = false;
            this.rewinding = false;
        }

        public boolean animationEnded() {
            return (this.time > this.lengthInTicks && !this.rewinding) || (this.time < 0 && this.rewinding);
        }

        private void updateTime(P parent, T skeleton) {
            if (this.playing && this.animation.running(parent, skeleton, this.mixFactor, this.time)) {
                this.time += (this.rewinding ? -1.0F : 1.0F);
            }
            if (this.animationEnded()) {
                this.stop();
            }
        }

        @Override
        protected void apply(P parent, T skeleton) {
            this.updateTime(parent, skeleton);
            super.apply(parent, skeleton);
        }
    }
}
