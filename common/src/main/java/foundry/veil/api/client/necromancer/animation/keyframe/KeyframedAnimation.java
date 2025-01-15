package foundry.veil.api.client.necromancer.animation.keyframe;

import foundry.veil.api.client.necromancer.Bone;
import foundry.veil.api.client.necromancer.Skeleton;
import foundry.veil.api.client.necromancer.SkeletonParent;
import foundry.veil.api.client.necromancer.animation.Animation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.*;

public class KeyframedAnimation<P extends SkeletonParent, S extends Skeleton<P>> extends Animation<P, S> {
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
    // a ton of allocations.
    private final Vector3f tempPos = new Vector3f(), tempScale = new Vector3f();
    private final Quaternionf tempRotA = new Quaternionf(), tempRotB = new Quaternionf();
    private final Keyframe[] tempKeyframes = new Keyframe[4];
    @Override
    public void apply(P parent, S skeleton, float mixFactor, float time) {
        for (Map.Entry<String, KeyframeTimeline> timeline : keyframesByBoneName.entrySet()) {
            Bone bone = skeleton.bones.get(timeline.getKey());
            if (bone == null) continue;

            KeyframeTimeline keyframes = timeline.getValue();
            keyframes.getTransform(time, looping, tempPos, tempScale, tempRotA, tempRotB, tempKeyframes);
            if (additive) {
                bone.x += tempPos.x() * mixFactor;
                bone.y += tempPos.y() * mixFactor;
                bone.z += tempPos.z() * mixFactor;

                bone.xSize *= tempScale.x();
                bone.ySize *= tempScale.y();
                bone.zSize *= tempScale.z();

                bone.rotation.premul(tempRotA.slerp(tempRotB.identity(), (1 - mixFactor)));
            } else {
                bone.x = Mth.lerp(mixFactor, bone.x, tempPos.x());
                bone.y = Mth.lerp(mixFactor, bone.y, tempPos.y());
                bone.z = Mth.lerp(mixFactor, bone.z, tempPos.z());

                // linear interpolation technically wrong here
                // todo: fix
                bone.xSize = Mth.lerp(mixFactor, bone.xSize, tempScale.x());
                bone.ySize = Mth.lerp(mixFactor, bone.ySize, tempScale.y());
                bone.zSize = Mth.lerp(mixFactor, bone.zSize, tempScale.z());

                bone.rotation.slerp(tempRotA, mixFactor);
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
