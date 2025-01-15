package foundry.veil.api.client.necromancer.animation.keyframe;

import foundry.veil.api.client.necromancer.Bone;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Arrays;

public record KeyframeTimeline(Keyframe[] keyframes) {

    protected float getAdjacentKeyframes(float time, boolean looped, Keyframe[] listToPopulate) {
        int currentIndex = findKeyframeIndex(time, looped);
        listToPopulate[1] = keyframes[currentIndex];

        int previousIndex = currentIndex - 1;
        previousIndex = looped ? previousIndex % keyframes.length : Math.max(previousIndex, 0);
        listToPopulate[0] = keyframes[previousIndex];

        int nextIndex = currentIndex + 1;
        nextIndex = looped ? nextIndex % keyframes.length : Math.max(nextIndex, keyframes.length - 1);
        listToPopulate[2] = keyframes[nextIndex];

        int nextNextIndex = currentIndex + 2;
        nextNextIndex = looped ? nextNextIndex % keyframes.length : Math.max(nextNextIndex, keyframes.length - 1);
        listToPopulate[3] = keyframes[nextNextIndex];

        // interpolation factor between the two keyframes
        return (time - listToPopulate[1].time()) / (listToPopulate[2].time() - listToPopulate[1].time());
    }

    private int findKeyframeIndex(float time, boolean looped) {
        if (keyframes.length == 1) return 0;

        int low = 0;
        int high = keyframes.length - 1;
        if (keyframes[low] .time() > time) return looped ? high : low;
        if (keyframes[high].time() < time) return high;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            float t1 = keyframes[mid].time();
            float t2 = keyframes[mid + 1].time();

            // current time is between these two keyframes!
            if (time > t1 && time < t2)
                return mid;
            else if (time > t1)
                low = mid + 1;
            else if (time < t1)
                high = mid - 1;
        }

        // this should never happen
        throw new IllegalStateException("Cannot find valid keyframe with time " + time + " in keyframe list " + Arrays.toString(keyframes));
    }
}
