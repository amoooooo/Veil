package foundry.veil.api.event;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import foundry.veil.api.client.render.MatrixStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import org.joml.Matrix4fc;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Fired for each render stage to draw arbitrarily to the screen. This is available as a last-resort if {@link VeilRegisterFixedBuffersEvent} doesn't fit the use case.
 *
 * @author Ocelot
 * @see VeilRegisterFixedBuffersEvent
 */
@FunctionalInterface
public interface VeilRenderLevelStageEvent {

    /**
     * Called when the specified level stage is rendered. This functions the same as Forge.
     *
     * @param stage            The stage rendering
     * @param levelRenderer    The level renderer instance
     * @param bufferSource     The buffer source instance
     * @param matrixStack      The current render transformations
     * @param frustumMatrix    The current state of view matrix transformations
     * @param projectionMatrix The current projection matrix being used to render
     * @param renderTick       The current tick of rendering
     * @param deltaTracker     The delta time tracker for rendering
     * @param camera           The camera the level is rendered from
     * @param frustum          The view frustum instance
     */
    void onRenderLevelStage(Stage stage, LevelRenderer levelRenderer, MultiBufferSource.BufferSource bufferSource, MatrixStack matrixStack, Matrix4fc frustumMatrix, Matrix4fc projectionMatrix, int renderTick, DeltaTracker deltaTracker, Camera camera, Frustum frustum);

    /**
     * Stages for rendering specific render types.
     *
     * @author Ocelot
     */
    enum Stage {
        AFTER_SKY,
        AFTER_SOLID_BLOCKS,
        AFTER_CUTOUT_MIPPED_BLOCKS,
        AFTER_CUTOUT_BLOCKS,
        AFTER_ENTITIES,
        AFTER_BLOCK_ENTITIES,
        AFTER_TRANSLUCENT_BLOCKS,
        AFTER_TRIPWIRE_BLOCKS,
        AFTER_PARTICLES,
        AFTER_WEATHER,
        AFTER_LEVEL;

        private static final Stage[] VALUES = values();

        public static final Codec<Stage> CODEC = Codec.STRING.flatXmap(name -> {
            for (Stage stage : VALUES) {
                if (stage.getName().equals(name)) {
                    return DataResult.success(stage);
                }
            }
            return DataResult.error(() -> "Unknown render stage: " + name + ". Valid stages: " + Arrays.stream(VALUES).map(Stage::getName).collect(Collectors.joining(", ")));
        }, stage -> DataResult.success(stage.getName()));

        private final String name;

        Stage() {
            this.name = this.name().toLowerCase(Locale.ROOT);
        }

        /**
         * @return The name of this render stage
         */
        public String getName() {
            return this.name;
        }
    }
}
