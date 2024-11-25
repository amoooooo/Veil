package foundry.veil.api.client.necromancer.animation.keyframed;

import foundry.veil.api.client.necromancer.Bone;
import foundry.veil.api.client.necromancer.Skeleton;
import foundry.veil.api.client.necromancer.SkeletonParent;
import foundry.veil.api.client.necromancer.animation.Animation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class KeyframedAnimation<P extends SkeletonParent, T extends Skeleton<P>> extends Animation<P, T> {
    final boolean looped;
    final boolean additive;
    final Map<String, BoneTimeline> timelines;

    protected KeyframedAnimation(boolean looped, boolean additive) {
        this.looped = looped;
        this.additive = additive;
        this.timelines = new HashMap<>();
    }

    public boolean running(P parent, T skeleton, float mixFactor, float time) {
        return mixFactor > 0;
    }

    public void apply(P parent, T skeleton, float mixFactor, float time) {
        if (this.looped) time = time % 1;

        for (Map.Entry<String, BoneTimeline> entry : this.timelines.entrySet()) {
            String boneID = entry.getKey();
            BoneTimeline timeline = entry.getValue();

            if (!skeleton.bones.containsKey(boneID)) continue;
            Bone bone = skeleton.bones.get(boneID);

            // special case for first or last keyframes
            Keyframe first = timeline.keyframes.get(0);
            Keyframe last = timeline.keyframes.get(timeline.keyframes.size() - 1);
            if (time < first.time) {
                if (this.looped) applyMixedKeyframes(bone, last, first, (time - (last.time - 1)) / (first.time - (last.time - 1)), mixFactor);
                else first.transform.apply(bone, mixFactor, additive);
                continue;
            } else if (time > last.time) {
                if (this.looped) applyMixedKeyframes(bone, last, first, (time - last.time) / ((first.time + 1) - last.time), mixFactor);
                else last.transform.apply(bone, mixFactor, additive);
                continue;
            }

            // todo: replace with binary search
            for (int i = 0; i < timeline.keyframes.size() - 1; i++) {
                Keyframe prev = timeline.keyframes.get(i);
                Keyframe next = timeline.keyframes.get(i + 1);
                if (next.time > time) {
                    applyMixedKeyframes(bone, prev, first, (time - prev.time) / (next.time - prev.time), mixFactor);
                    break;
                }
            }
        }
    }

    private void applyMixedKeyframes(Bone bone, Keyframe prevKeyframe, Keyframe nextKeyframe, float keyframeMixFactor, float poseMixFactor) {
        BoneTransform transform = BoneTransform.compose(prevKeyframe.transform, nextKeyframe.transform, keyframeMixFactor);
        transform.apply(bone, poseMixFactor, additive);
    }

    protected static class BoneTimeline {
        final List<Keyframe> keyframes;
        protected BoneTimeline(String boneID) {
            this.keyframes = new ArrayList<>();
        }
    }

    protected static class Keyframe {
        final float time;
        final BoneTransform transform;

        protected Keyframe(float time, BoneTransform transform) {
            this.time = time;
            this.transform = transform;
        }
    }
}
