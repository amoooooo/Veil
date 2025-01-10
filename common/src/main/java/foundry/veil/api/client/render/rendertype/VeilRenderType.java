package foundry.veil.api.client.render.rendertype;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import foundry.veil.Veil;
import foundry.veil.api.client.render.VeilRenderBridge;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.VeilVertexFormat;
import foundry.veil.api.client.render.shader.VeilShaders;
import foundry.veil.impl.client.render.pipeline.CullFaceShard;
import foundry.veil.mixin.rendertype.accessor.RenderStateShardAccessor;
import foundry.veil.mixin.rendertype.accessor.RenderTypeAccessor;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.lwjgl.opengl.GL11C.*;

/**
 * Custom Veil-implemented render types.
 */
public final class VeilRenderType extends RenderType {

    public static final RenderStateShard.DepthTestStateShard NEVER_DEPTH_TEST = new RenderStateShard.DepthTestStateShard("never", GL_NEVER);
    public static final RenderStateShard.DepthTestStateShard LESS_DEPTH_TEST = new RenderStateShard.DepthTestStateShard("<", GL_LESS);
    public static final RenderStateShard.DepthTestStateShard NOTEQUAL_DEPTH_TEST = new RenderStateShard.DepthTestStateShard("<", GL_NOTEQUAL);
    public static final RenderStateShard.DepthTestStateShard GEQUAL_DEPTH_TEST = new RenderStateShard.DepthTestStateShard(">=", GL_GEQUAL);

    public static final RenderStateShard CULL_FRONT = new CullFaceShard(GL_FRONT);
    public static final RenderStateShard CULL_BACK = new CullFaceShard(GL_BACK);
    public static final RenderStateShard CULL_FRONT_AND_BACK = new CullFaceShard(GL_FRONT_AND_BACK);
    public static final RenderStateShard.WriteMaskStateShard NO_WRITE = new RenderStateShard.WriteMaskStateShard(false, false);

    private static final EnumMap<GlStateManager.LogicOp, ColorLogicStateShard> COLOR_LOGIC_SHARDS = new EnumMap<>(GlStateManager.LogicOp.class);

    static {
        for (GlStateManager.LogicOp logicOp : GlStateManager.LogicOp.values()) {
            COLOR_LOGIC_SHARDS.put(logicOp, new ColorLogicStateShard(logicOp.name().toLowerCase(Locale.ROOT), () -> {
                RenderSystem.enableColorLogicOp();
                RenderSystem.logicOp(logicOp);
            }, RenderSystem::disableColorLogicOp));
        }
    }

    private static final ShaderStateShard PARTICLE = VeilRenderBridge.shaderState(VeilShaders.PARTICLE);
    private static final ShaderStateShard SKINNED_MESH = VeilRenderBridge.shaderState(VeilShaders.SKINNED_MESH);

    private static final BiFunction<ResourceLocation, Boolean, RenderType> QUASAR_PARTICLE = Util.memoize((texture, additive) -> {
        CompositeState state = RenderType.CompositeState.builder()
                .setShaderState(PARTICLE)
                .setTextureState(new TextureStateShard(texture, false, false))
                .setTransparencyState(additive ? ADDITIVE_TRANSPARENCY : TRANSLUCENT_TRANSPARENCY)
                .setLightmapState(LIGHTMAP)
                .setWriteMaskState(additive ? COLOR_WRITE : COLOR_DEPTH_WRITE)
                .createCompositeState(false);
        return create(Veil.MODID + ":quasar_particle", VeilVertexFormat.QUASAR_PARTICLE, VertexFormat.Mode.QUADS, SMALL_BUFFER_SIZE, false, !additive, state);
    });
    private static final Function<ResourceLocation, RenderType> QUASAR_TRAIL = Util.memoize((texture) -> {
        CompositeState state = CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                .setTextureState(new TextureStateShard(texture, false, false))
                .setTransparencyState(ADDITIVE_TRANSPARENCY)
                .setWriteMaskState(COLOR_WRITE)
                .setCullState(NO_CULL)
                .createCompositeState(false);
        return RenderType.create(Veil.MODID + ":quasar_trail", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.TRIANGLE_STRIP, TRANSIENT_BUFFER_SIZE, false, false, state);
    });

    private static final Function<ResourceLocation, RenderType> NECROMANCER_SKINNED_MESH = Util.memoize((texture) -> {
        CompositeState state = RenderType.CompositeState.builder()
                .setShaderState(SKINNED_MESH)
                .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                .setTransparencyState(NO_TRANSPARENCY)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .createCompositeState(true);
        return RenderType.create(Veil.MODID + ":skinned_mesh", VeilVertexFormat.SKINNED_MESH, VertexFormat.Mode.QUADS, SMALL_BUFFER_SIZE, true, false, state);
    });

    public static RenderType quasarParticle(ResourceLocation texture, boolean additive) {
        return QUASAR_PARTICLE.apply(texture, additive);
    }

    public static RenderType quasarTrail(ResourceLocation texture) {
        return QUASAR_TRAIL.apply(texture);
    }

    public static TransparencyStateShard noTransparencyShard() {
        return RenderStateShard.NO_TRANSPARENCY;
    }

    public static TransparencyStateShard additiveTransparencyShard() {
        return RenderStateShard.ADDITIVE_TRANSPARENCY;
    }

    public static TransparencyStateShard lightningTransparencyShard() {
        return RenderStateShard.LIGHTNING_TRANSPARENCY;
    }

    public static TransparencyStateShard glintTransparencyShard() {
        return RenderStateShard.GLINT_TRANSPARENCY;
    }

    public static TransparencyStateShard crumblingTransparencyShard() {
        return RenderStateShard.CRUMBLING_TRANSPARENCY;
    }

    public static TransparencyStateShard translucentTransparencyShard() {
        return RenderStateShard.TRANSLUCENT_TRANSPARENCY;
    }

    public static DepthTestStateShard noDepthTestShard() {
        return RenderStateShard.NO_DEPTH_TEST;
    }

    public static DepthTestStateShard equalDepthTestShard() {
        return RenderStateShard.EQUAL_DEPTH_TEST;
    }

    public static DepthTestStateShard lequalDepthTestShard() {
        return RenderStateShard.LEQUAL_DEPTH_TEST;
    }

    public static DepthTestStateShard greaterDepthTestShard() {
        return RenderStateShard.GREATER_DEPTH_TEST;
    }

    public static CullStateShard cullShard() {
        return RenderStateShard.CULL;
    }

    public static CullStateShard noCullShard() {
        return RenderStateShard.NO_CULL;
    }

    public static LightmapStateShard lightmap() {
        return RenderStateShard.LIGHTMAP;
    }

    public static LightmapStateShard noLightmap() {
        return RenderStateShard.NO_LIGHTMAP;
    }

    public static OverlayStateShard overlay() {
        return RenderStateShard.OVERLAY;
    }

    public static OverlayStateShard noOverlay() {
        return RenderStateShard.NO_OVERLAY;
    }

    public static LayeringStateShard noLayering() {
        return RenderStateShard.NO_LAYERING;
    }

    public static LayeringStateShard polygonOffsetLayering() {
        return RenderStateShard.POLYGON_OFFSET_LAYERING;
    }

    public static LayeringStateShard viewOffsetLayering() {
        return RenderStateShard.VIEW_OFFSET_Z_LAYERING;
    }

    public static WriteMaskStateShard colorDepthWriteShard() {
        return RenderStateShard.COLOR_DEPTH_WRITE;
    }

    public static WriteMaskStateShard colorWriteShard() {
        return RenderStateShard.COLOR_WRITE;
    }

    public static WriteMaskStateShard depthWriteShard() {
        return RenderStateShard.DEPTH_WRITE;
    }

    public static ColorLogicStateShard colorLogicStateShard(GlStateManager.LogicOp op) {
        return COLOR_LOGIC_SHARDS.get(op);
    }

    /**
     * Retrieves and caches a render type with the specified id.
     *
     * @param id     The id of the render type to get
     * @param params Additional parameters to configure the render type
     * @return The render type created or <code>null</code> if unregistered or an error occurs
     */
    public static @Nullable RenderType get(ResourceLocation id, Object... params) {
        return VeilRenderSystem.renderer().getDynamicRenderTypeManager().get(id, params);
    }

    /**
     * Retrieves the name of the specified render shard.
     *
     * @param shard The render shard to get the name of
     * @return The name of the render type to get
     */
    public static String getName(RenderStateShard shard) {
        return ((RenderStateShardAccessor) shard).getName();
    }

    public static VeilRenderTypeAccessor getShards(RenderType renderType) {
        if (!(renderType instanceof CompositeRenderType compositeRenderType)) {
            throw new IllegalArgumentException("Expected composite render type to be an instance of " + CompositeRenderType.class.getName() + ", but was " + renderType.getClass());
        }
        return (VeilRenderTypeAccessor) (Object) compositeRenderType.state();
    }

    /**
     * Creates a render type that uses a single draw buffer, but re-uses the data to draw the specified layers.
     *
     * @param layers The layers to use
     * @return A render type that draws all layers from a single buffer
     * @throws IllegalStateException If there are zero layers, the vertex formats don't all match, or the primitive modes don't match
     */
    public static RenderType layered(RenderType... layers) {
        if (layers.length == 0) {
            throw new IllegalArgumentException("At least 1 render type must be specified");
        }
        if (layers.length == 1) {
            return layers[0];
        }
        ImmutableList.Builder<RenderType> builder = ImmutableList.builder();
        VertexFormat format = layers[0].format();
        VertexFormat.Mode mode = layers[0].mode();
        int bufferSize = layers[0].bufferSize();
        boolean sortOnUpload = ((RenderTypeAccessor) layers[0]).isSortOnUpload();
        for (int i = 1; i < layers.length; i++) {
            RenderType layer = layers[i];
            if (!layer.format().equals(format)) {
                throw new IllegalArgumentException("Expected " + layer + " to use " + format + ", but was " + layer.format());
            }
            if (!layer.mode().equals(mode)) {
                throw new IllegalArgumentException("Expected " + layer + " to use " + mode + ", but was " + layer.mode());
            }
            bufferSize = Math.max(bufferSize, layer.bufferSize());
            if (((RenderTypeAccessor) layer).
                    isSortOnUpload()) {
                sortOnUpload = true;
            }
            builder.add(layer);
        }
        return new LayeredRenderType(layers[0], builder.build(), "LayeredRenderType[" + Arrays.stream(layers).map(VeilRenderType::getName).collect(Collectors.joining(", ")) + "]", bufferSize, sortOnUpload);
    }

    private VeilRenderType(String $$0, VertexFormat $$1, VertexFormat.Mode $$2, int $$3, boolean $$4, boolean $$5, Runnable $$6, Runnable $$7) {
        super($$0, $$1, $$2, $$3, $$4, $$5, $$6, $$7);
    }

    public static class LayeredRenderType extends RenderType {

        private final List<RenderType> layers;

        private LayeredRenderType(RenderType defaultValue, List<RenderType> layers, String name, int bufferSize, boolean sortOnUpload) {
            super(name, defaultValue.format(), defaultValue.mode(), bufferSize, defaultValue.affectsCrumbling(), sortOnUpload, defaultValue::setupRenderState, defaultValue::clearRenderState);
            this.layers = layers;
        }

        @Override
        public void draw(MeshData meshData) {
            super.draw(meshData);
            if (BufferUploader.lastImmediateBuffer != null) {
                for (RenderType layer : this.layers) {
                    layer.setupRenderState();
                    BufferUploader.lastImmediateBuffer.drawWithShader(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
                    layer.clearRenderState();
                }
            }
        }

        public List<RenderType> getLayers() {
            return this.layers;
        }
    }
}
