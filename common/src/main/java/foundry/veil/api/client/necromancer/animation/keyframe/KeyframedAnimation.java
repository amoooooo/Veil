package foundry.veil.api.client.necromancer.animation.keyframe;

import foundry.veil.api.client.necromancer.Bone;
import foundry.veil.api.client.necromancer.Skeleton;
import foundry.veil.api.client.necromancer.SkeletonParent;
import foundry.veil.api.client.necromancer.animation.Animation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3fc;

import java.util.*;

public class KeyframedAnimation<P extends SkeletonParent<?, ?>, S extends Skeleton> extends Animation<P, S> {
    private final Map<String, KeyframeTimeline> keyframesByBoneName;
    private final boolean additive;
    private final boolean looping;

    private KeyframedAnimation(Map<String, KeyframeTimeline> keyframesByBoneName, boolean additive, boolean looping) {
        this.keyframesByBoneName = keyframesByBoneName;
        this.additive = additive;
        this.looping = looping;
    }

    // todo: make this more static
    // so it works with multithreading.
    // no idea how to do that though without
    // a ton of allocations though!
    private final Quaternionf tempRotationA = new Quaternionf(), tempRotationB = new Quaternionf();
    private final Keyframe[] tempKeyframes = new Keyframe[4];
    @Override
    public void apply(P parent, S skeleton, float mixFactor, float time) {
        for (Map.Entry<String, KeyframeTimeline> timeline : keyframesByBoneName.entrySet()) {
            Bone bone = skeleton.bones.get(timeline.getKey());
            if (bone == null) continue;

            KeyframeTimeline keyframes = timeline.getValue();
            float t = keyframes.getAdjacentKeyframes(time, this.looping, tempKeyframes);
            Keyframe a = tempKeyframes[1];
            Keyframe b = tempKeyframes[2];
            Interpolation interpolation = a.interpolation();

            // todo: cubic interpolation
            if (additive) {
                bone.position.add(
                        interpolation.interpolate(a.transform().px(), b.transform().px(), t),
                        interpolation.interpolate(a.transform().py(), b.transform().py(), t),
                        interpolation.interpolate(a.transform().pz(), b.transform().pz(), t)
                );
                tempRotationA.set(a.transform().qx(), a.transform().qy(), a.transform().qz(), a.transform().qw());
                tempRotationB.set(b.transform().qx(), b.transform().qy(), b.transform().qz(), b.transform().qw());
                interpolation.interpolate(tempRotationA, tempRotationB, t, tempRotationA);
                tempRotationA.slerp(tempRotationB.identity(), mixFactor);
                bone.rotation.premul(tempRotationA);
                bone.size.mul(
                        Mth.lerp(mixFactor, 1, interpolation.interpolate(a.transform().sx(), b.transform().sx(), t)),
                        Mth.lerp(mixFactor, 1, interpolation.interpolate(a.transform().sy(), b.transform().sy(), t)),
                        Mth.lerp(mixFactor, 1, interpolation.interpolate(a.transform().sz(), b.transform().sz(), t))
                );
            } else {
                bone.size.set(
                        Mth.lerp(mixFactor, bone.position.x, interpolation.interpolate(a.transform().px(), b.transform().px(), t)),
                        Mth.lerp(mixFactor, bone.position.y, interpolation.interpolate(a.transform().py(), b.transform().py(), t)),
                        Mth.lerp(mixFactor, bone.position.z, interpolation.interpolate(a.transform().pz(), b.transform().pz(), t))
                );
                tempRotationA.set(a.transform().qx(), a.transform().qy(), a.transform().qz(), a.transform().qw());
                tempRotationB.set(b.transform().qx(), b.transform().qy(), b.transform().qz(), b.transform().qw());
                interpolation.interpolate(tempRotationA, tempRotationB, t, tempRotationA);
                bone.rotation.slerp(tempRotationA, mixFactor);
                bone.size.set(
                        Mth.lerp(mixFactor, bone.size.x, interpolation.interpolate(a.transform().sx(), b.transform().sx(), t)),
                        Mth.lerp(mixFactor, bone.size.y, interpolation.interpolate(a.transform().sy(), b.transform().sy(), t)),
                        Mth.lerp(mixFactor, bone.size.z, interpolation.interpolate(a.transform().sz(), b.transform().sz(), t))
                );
            }
        }
    }

    public static class Builder {
        boolean looped = false, additive = false;
        Map<String, List<Keyframe>> timelines = new HashMap<>();
        
        Builder looped(boolean isLooped) {
            this.looped = isLooped;
            return this;
        }

        Builder additive(boolean isAdditive) {
            this.additive = isAdditive;
            return this;
        }

        // todo: allow for keyframes to only specify one channel (ex. only position, only orientation, etc.)
        // probably only in the builder? and just bake everything to equivalent full keyframes
        // idk!
        public void addKeyframe(String boneId, float time, Interpolation interpolation,
                                Vector3fc position, Vector3fc size, Quaternionfc orientation) {
            if (!timelines.containsKey(boneId)) timelines.put(boneId, new ArrayList<>(2));
            timelines.get(boneId).add(new Keyframe(time, interpolation, new Keyframe.KeyframeTransform(position, size, orientation)));
        }

        public KeyframedAnimation<?, ?> build() {
            Map<String, KeyframeTimeline> builtTimelines = new HashMap<>();
            for (Map.Entry<String, List<Keyframe>> timeline : timelines.entrySet()) {
                List<Keyframe> keyframeList = timeline.getValue();
                keyframeList.sort(Comparator.comparingDouble(Keyframe::time));
                KeyframeTimeline builtTimeline = new KeyframeTimeline(keyframeList.toArray(new Keyframe[0]));
                builtTimelines.put(timeline.getKey(), builtTimeline);
            }
            return new KeyframedAnimation<>(builtTimelines, this.additive, this.looped);
        }
    }
}
