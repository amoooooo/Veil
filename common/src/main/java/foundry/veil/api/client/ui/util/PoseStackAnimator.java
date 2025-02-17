package foundry.veil.api.client.ui.util;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * A utility class to manage and apply animations to a {@link PoseStack}.
 * Animations are defined as a sequence of {@link AnimationStage}s, each with a duration and a transformation function.
 */
public class PoseStackAnimator {

    private final PoseStack poseStack;
    private final List<AnimationStage> stages = new ArrayList<>();
    private int currentStageIndex = -1;
    private long stageStartTime;

    /**
     * Constructs a new {@link PoseStackAnimator} for the given {@link PoseStack}.
     *
     * @param poseStack The {@link PoseStack} to animate.
     */
    public PoseStackAnimator(PoseStack poseStack) {
        this.poseStack = poseStack;
    }

    /**
     * Adds a new animation stage to the sequence.
     *
     * @param durationMillis The duration of the stage in milliseconds.
     * @param transform      The transformation function to apply during this stage.
     *                       This function receives the {@link PoseStack} as an argument, allowing you to directly modify its transformations.
     */
    public void addStage(long durationMillis, BiConsumer<Long, PoseStack> transform) {
        stages.add(new AnimationStage(durationMillis, transform));
    }

    /**
     * Starts the animation sequence.
     */
    public void startAnimation() {
        currentStageIndex = 0;
        stageStartTime = System.currentTimeMillis();
    }

    /**
     * Advances the animation based on the elapsed time.
     * Call this method once per frame to update the animation.
     */
    public void tickAnimation() {
        if (isAnimating()) {
            AnimationStage currentStage = stages.get(currentStageIndex);

            long elapsedStageTime = System.currentTimeMillis() - stageStartTime;
            float progress = Mth.clamp((float) elapsedStageTime / currentStage.durationMillis, 0.0F, 1.0F);

            applyStageTransform(currentStage, progress);

            if (progress >= 1.0F && currentStageIndex < stages.size() - 1) {
                currentStageIndex++;
                stageStartTime = System.currentTimeMillis();
            } else if (progress >= 1.0F) {
                resetAnimation();
            }
        }
    }

    /**
     * Checks if the animation is currently active.
     *
     * @return True if the animation is running, false otherwise.
     */
    public boolean isAnimating() {
        return currentStageIndex >= 0 && currentStageIndex < stages.size();
    }

    /**
     * Resets the animation, stopping it and clearing any defined stages.
     */
    public void resetAnimation() {
        currentStageIndex = -1;
        stages.clear();
    }

    /**
     * Applies the transformation for the current animation stage based on the animation progress.
     * This method ensures that each stage's transformation is applied independently and doesn't affect other stages.
     *
     * @param stage    The current animation stage.
     * @param progress The animation progress within the current stage, ranging from 0.0 to 1.0.
     */
    private void applyStageTransform(AnimationStage stage, float progress) {
        poseStack.pushPose(); // Isolate this stage's transformations

        long elapsedStageTime = (long) (progress * stage.durationMillis);
        stage.transform.accept(elapsedStageTime, poseStack); // Apply the stage's transform function

        poseStack.popPose(); // Restore the pose stack to its previous state
    }

    /**
     * Represents a single stage in the animation sequence, holding its duration and the transformation function.
     */
    private static class AnimationStage {
        long durationMillis;
        BiConsumer<Long, PoseStack> transform;

        /**
         * Constructs a new animation stage.
         *
         * @param durationMillis The duration of the stage in milliseconds.
         * @param transform      The transformation function to apply during this stage.
         */
        AnimationStage(long durationMillis, BiConsumer<Long, PoseStack> transform) {
            this.durationMillis = durationMillis;
            this.transform = transform;
        }
    }
}