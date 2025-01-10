package foundry.veil.api.client.registry;

import foundry.veil.api.client.render.rendertype.VeilRenderType;
import foundry.veil.ext.CompositeStateExtension;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.ApiStatus;

import java.util.*;
import java.util.function.Predicate;

/**
 * <p>This allows custom render type shards to be registered. This allows custom code to be run for the setup and clear state of any render type.
 * {@link RenderTypeShardRegistry#addGenericShard(Predicate, RenderStateShard...)} also allows arbitrary injection into any render type created.</p>
 * <p><strong>This should be called during mod construction/init.</strong></p>
 *
 * @author Ocelot
 */
public final class RenderTypeShardRegistry {

    private static final Map<String, List<RenderStateShard>> SHARDS = new HashMap<>();
    private static final Set<GenericShard> GENERIC_SHARDS = new HashSet<>();
    private static final Set<RenderType.CompositeRenderType> CREATED_RENDER_TYPES = new HashSet<>();

    private RenderTypeShardRegistry() {
    }

    /**
     * Registers a render stage. The specified shards will only be added to the specific render type.
     *
     * @param renderType The render type to add the stage to
     * @param shards     The shards to add to all matching render types
     */
    public static synchronized void addShard(RenderType renderType, RenderStateShard... shards) {
        if (shards.length == 0) {
            return;
        }
        if (!(renderType instanceof RenderType.CompositeRenderType compositeRenderType)) {
            throw new IllegalArgumentException("RenderType must be CompositeRenderType");
        }
        ((CompositeStateExtension) (Object) compositeRenderType.state()).veil$addShards(Arrays.asList(shards));
    }

    /**
     * Registers a render stage. The specified shards will be added to the specified render type during construction.
     *
     * @param name   The name of the render type to add the stage to
     * @param shards The shards to add to all matching render types
     */
    public static synchronized void addShard(String name, RenderStateShard... shards) {
        if (shards.length == 0) {
            throw new IllegalArgumentException("No shards provided");
        }
        List<RenderStateShard> newShards = Arrays.asList(shards);
        SHARDS.computeIfAbsent(name, unused -> new ArrayList<>()).addAll(newShards);

        for (RenderType.CompositeRenderType renderType : CREATED_RENDER_TYPES) {
            if (name.equals(VeilRenderType.getName(renderType))) {
                ((CompositeStateExtension) (Object) renderType.state()).veil$addShards(newShards);
            }
        }
    }

    /**
     * Registers a render stage. The specified shards will be added to all render types that match the specified filter during construction.
     *
     * @param filter The filter for what render types to add the stage to
     * @param shards The shards to add to all matching render types
     */
    public static synchronized void addGenericShard(Predicate<RenderType.CompositeRenderType> filter, RenderStateShard... shards) {
        if (shards.length == 0) {
            throw new IllegalArgumentException("No shards provided");
        }
        GENERIC_SHARDS.add(new GenericShard(filter, shards));

        for (RenderType.CompositeRenderType renderType : CREATED_RENDER_TYPES) {
            if (filter.test(renderType)) {
                ((CompositeStateExtension) (Object) renderType.state()).veil$addShards(Arrays.asList(shards));
            }
        }
    }

    // Implementation

    @SuppressWarnings({"UnreachableCode", "DataFlowIssue"})
    @ApiStatus.Internal
    public static void inject(RenderType.CompositeRenderType renderType) {
        List<RenderStateShard> shards = SHARDS.get(VeilRenderType.getName(renderType));
        if (shards != null) {
            shards = new ArrayList<>(shards);
        }
        for (GenericShard stage : GENERIC_SHARDS) {
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

    private record GenericShard(Predicate<RenderType.CompositeRenderType> filter, RenderStateShard[] shards) {
    }
}
