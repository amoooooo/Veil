package foundry.veil.api.client.registry;

import foundry.veil.ext.CompositeStateExtension;
import foundry.veil.mixin.accessor.RenderStateShardAccessor;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.ApiStatus;

import java.util.*;
import java.util.function.Predicate;

/**
 * <p>This allows custom render type stages to be registered. This allows custom code to be run for the setup and clear state of any render type.
 * {@link RenderTypeStageRegistry#addGenericStage(Predicate, RenderStateShard...)} also allows arbitrary injection into any render type created.</p>
 * <p><strong>This should be called during mod construction/init.</strong></p>
 *
 * @author Ocelot
 */
public final class RenderTypeStageRegistry {

    private static final Map<String, List<RenderStateShard>> STAGES = new HashMap<>();
    private static final Set<GenericStage> GENERIC_STAGES = new HashSet<>();
    private static final Set<RenderType.CompositeRenderType> CREATED_RENDER_TYPES = new HashSet<>();

    private RenderTypeStageRegistry() {
    }

    /**
     * Registers a render stage. The specified shards will only be added to the specific render type.
     *
     * @param renderType The render type to add the stage to
     * @param shards     The shards to add to all matching render types
     */
    public static synchronized void addStage(RenderType renderType, RenderStateShard... shards) {
        if (shards.length == 0) {
            return;
        }
        if (!(renderType instanceof RenderType.CompositeRenderType compositeRenderType)) {
            throw new IllegalArgumentException("RenderType must be CompositeRenderType");
        }

        List<RenderStateShard> addShards = new ArrayList<>(Arrays.asList(shards));
        for (GenericStage stage : GENERIC_STAGES) {
            if (stage.filter.test(compositeRenderType)) {
                addShards.addAll(Arrays.asList(stage.shards));
            }
        }

        ((CompositeStateExtension) (Object) compositeRenderType.state()).veil$addShards(addShards);
    }

    /**
     * Registers a render stage. The specified shards will be added to the specified render type during construction.
     *
     * @param name   The name of the render type to add the stage to
     * @param shards The shards to add to all matching render types
     */
    public static synchronized void addStage(String name, RenderStateShard... shards) {
        if (shards.length == 0) {
            throw new IllegalArgumentException("No shards provided");
        }
        STAGES.computeIfAbsent(name, unused -> new ArrayList<>()).addAll(Arrays.asList(shards));

        for (RenderType.CompositeRenderType renderType : CREATED_RENDER_TYPES) {
            inject(renderType);
        }
    }

    /**
     * Registers a render stage. The specified shards will be added to all render types that match the specified filter during construction.
     *
     * @param filter The filter for what render types to add the stage to
     * @param shards The shards to add to all matching render types
     */
    public static synchronized void addGenericStage(Predicate<RenderType.CompositeRenderType> filter, RenderStateShard... shards) {
        if (shards.length == 0) {
            throw new IllegalArgumentException("No shards provided");
        }
        GENERIC_STAGES.add(new GenericStage(filter, shards));

        for (RenderType.CompositeRenderType renderType : CREATED_RENDER_TYPES) {
            inject(renderType);
        }
    }

    @SuppressWarnings({"UnreachableCode", "DataFlowIssue"})
    @ApiStatus.Internal
    public static void inject(RenderType.CompositeRenderType renderType) {
        List<RenderStateShard> shards = STAGES.get(((RenderStateShardAccessor) (Object) renderType).getName());
        if (shards != null) {
            shards = new ArrayList<>(shards);
        }
        for (GenericStage stage : GENERIC_STAGES) {
            if (stage.filter.test(renderType)) {
                if (shards == null) {
                    shards = new ArrayList<>(Arrays.asList(stage.shards));
                    continue;
                }

                shards.addAll(Arrays.asList(stage.shards));
            }
        }

        if (shards != null) {
            ((CompositeStateExtension) (Object) renderType.state()).veil$addShards(shards);
        }
        CREATED_RENDER_TYPES.add(renderType);
    }

    private record GenericStage(Predicate<RenderType.CompositeRenderType> filter, RenderStateShard... shards) {
    }
}
