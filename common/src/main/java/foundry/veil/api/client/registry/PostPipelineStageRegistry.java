package foundry.veil.api.client.registry;

import com.mojang.serialization.MapCodec;
import foundry.veil.Veil;
import foundry.veil.api.client.render.post.PostPipeline;
import foundry.veil.api.client.render.post.stage.BlitPostStage;
import foundry.veil.api.client.render.post.stage.CopyPostStage;
import foundry.veil.api.client.render.post.stage.DepthFunctionPostStage;
import foundry.veil.api.client.render.post.stage.MaskPostStage;
import foundry.veil.platform.registry.RegistrationProvider;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Supplier;

/**
 * Registry for all post pipeline stages.
 */
public final class PostPipelineStageRegistry {

    public static final ResourceKey<Registry<PipelineType<?>>> REGISTRY_KEY = ResourceKey.createRegistryKey(Veil.veilPath("post_pipeline_stage"));
    private static final RegistrationProvider<PipelineType<?>> PROVIDER = RegistrationProvider.get(REGISTRY_KEY, Veil.MODID);
    public static final Registry<PipelineType<?>> REGISTRY = PROVIDER.asVanillaRegistry();

    public static final Supplier<PipelineType<BlitPostStage>> BLIT = register("blit", BlitPostStage.CODEC);
    public static final Supplier<PipelineType<CopyPostStage>> COPY = register("copy", CopyPostStage.CODEC);
    public static final Supplier<PipelineType<MaskPostStage>> MASK = register("mask", MaskPostStage.CODEC);
    public static final Supplier<PipelineType<DepthFunctionPostStage>> DEPTH_FUNC = register("depth_function", DepthFunctionPostStage.CODEC);

    private PostPipelineStageRegistry() {
    }

    @ApiStatus.Internal
    public static void bootstrap() {
    }

    private static <T extends PostPipeline> Supplier<PipelineType<T>> register(String name, MapCodec<T> codec) {
        return PROVIDER.register(name, () -> new PipelineType<>(codec));
    }

    public record PipelineType<T extends PostPipeline>(MapCodec<T> codec) {
    }
}
