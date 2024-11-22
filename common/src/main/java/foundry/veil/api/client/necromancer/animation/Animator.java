package foundry.veil.api.client.necromancer.animation;



import foundry.veil.api.client.necromancer.Skeleton;
import foundry.veil.api.client.necromancer.SkeletonParent;
import foundry.veil.api.client.necromancer.constraint.Constraint;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public abstract class Animator<P extends SkeletonParent, T extends Skeleton<P>> {
    protected final P parent;
    protected final T skeleton;
    final List<ConstraintEntry> constraints;
    final List<AnimationEntry<P, T>> animations;

    protected Animator(P parent, T skeleton) {
        this.parent = parent;
        this.skeleton = skeleton;
        this.animations = new ArrayList<>();
        this.constraints = new ArrayList<>();
    }

    public void addConstraint(Constraint constraint, int priority) {
        constraints.add(new ConstraintEntry(constraint, priority));
        constraints.sort(Comparator.comparingInt(entry -> entry.priority));
    }

    public AnimationEntry<P, T> addAnimation(Animation<P, T> animation, int priority) {
        AnimationEntry<P, T> entry = new AnimationEntry<>(animation, priority);
        this.animations.add(entry);
        animations.sort(Comparator.comparingInt(animEntry -> animEntry.priority));
        return entry;
    }

    public TimedAnimationEntry<P, T> addTimedAnimation(Animation<P, T> animation, int priority, int animLength) {
        TimedAnimationEntry<P, T> entry = new TimedAnimationEntry<>(animation, priority, animLength);
        this.animations.add(entry);
        animations.sort(Comparator.comparingInt(animEntry -> animEntry.priority));
        return entry;
    }

    public void tick(P parent) {
        this.skeleton.tick();
        this.skeleton.bones.forEach((name, bone) -> bone.reset());
        this.animate(parent);
        this.animations.forEach(animation -> animation.apply(this.parent, this.skeleton));
        this.constraints.forEach(constraintEntry -> constraintEntry.constraint.apply());
        this.animatePostConstraints(parent);
    }
    public void animate(P parent) {}
    public void animatePostConstraints(P parent) {}

    record ConstraintEntry(Constraint constraint, int priority) {}

    public static class AnimationEntry<P extends SkeletonParent, T extends Skeleton<P>> {
        final Animation<P, T> animation;
        final int priority;
        float mixFactor;
        float time;

        private AnimationEntry(Animation<P, T> animation, int priority) {
            this.animation = animation;
            this.priority = priority;
        }

        public void setMixFactor(float mixFactor) {
            this.mixFactor = mixFactor;
        }
        public void setTime(float time) {
            this.time = time;
        }
        protected void apply(P parent, T skeleton) {
            if (!this.animation.running(parent, skeleton, mixFactor, time)) return;
            this.animation.apply(parent, skeleton, mixFactor, time);
        }
    }

    public static class TimedAnimationEntry<P extends SkeletonParent, T extends Skeleton<P>> extends AnimationEntry<P, T> {
        final int lengthInTicks;
        boolean rewinding = false;
        boolean playing = false;

        private TimedAnimationEntry(Animation<P, T> animation, int priority, int lengthInTicks) {
            super(animation, priority);
            this.lengthInTicks = lengthInTicks;
        }

        public void begin() { this.time = 0; this.resume(); }
        public void resume() { this.playing = true; }
        public void rewind() { this.rewinding = true; }
        public void stop() { this.playing = false; this.rewinding = false; }

        private void updateTime(P parent, T skeleton) {
            if (this.playing && this.animation.running(parent, skeleton, mixFactor, time)) this.time += (this.rewinding ? -1.0F : 1.0F) / lengthInTicks;
            if ((this.time > 1 && !rewinding) || (time < 0 && rewinding)) this.stop();
        }

        @Override
        protected void apply(P parent, T skeleton) {
            updateTime(parent, skeleton);
            if (!this.playing) return;
            super.apply(parent, skeleton);
        }
    }
}
