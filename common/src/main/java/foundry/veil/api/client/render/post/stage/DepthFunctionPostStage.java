package foundry.veil.api.client.render.post.stage;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import foundry.veil.api.client.registry.PostPipelineStageRegistry;
import foundry.veil.api.client.render.post.PostPipeline;

import static org.lwjgl.opengl.GL11C.*;

/**
 * Sets the depth function.
 *
 * @param func The new depth function to use
 * @author Ocelot
 */
public record DepthFunctionPostStage(DepthFunc func) implements PostPipeline {

    public static final MapCodec<DepthFunctionPostStage> CODEC = DepthFunc.CODEC.fieldOf("function").xmap(DepthFunctionPostStage::new, DepthFunctionPostStage::func);

    @Override
    public void apply(Context context) {
        RenderSystem.depthFunc(this.func.getId());
    }

    @Override
    public PostPipelineStageRegistry.PipelineType<? extends PostPipeline> getType() {
        return PostPipelineStageRegistry.DEPTH_FUNC.get();
    }


    public enum DepthFunc {
        NEVER(GL_NEVER),
        LESS(GL_LESS),
        EQUAL(GL_EQUAL),
        LEQUAL(GL_LEQUAL),
        GREATER(GL_GREATER),
        NOTEQUAL(GL_NOTEQUAL),
        GEQUAL(GL_GEQUAL),
        ALWAYS(GL_ALWAYS);

        public static final Codec<DepthFunc> CODEC = Codec.STRING.flatXmap(name -> {
            for (DepthFunc type : DepthFunc.values()) {
                if (type.name().equalsIgnoreCase(name)) {
                    return DataResult.success(type);
                }
            }
            return DataResult.error(() -> "Unknown depth function: " + name);
        }, type -> DataResult.success(type.name()));

        private final int id;

        DepthFunc(int id) {
            this.id = id;
        }

        /**
         * @return The OpenGL id of this data type
         */
        public int getId() {
            return this.id;
        }
    }
}
