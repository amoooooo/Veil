package foundry.veil.api.client.render.rendertype;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import foundry.veil.Veil;
import foundry.veil.api.client.render.VeilRenderBridge;
import foundry.veil.api.client.render.VeilVertexFormat;
import foundry.veil.api.client.render.shader.VeilShaders;
import foundry.veil.mixin.accessor.RenderTypeAccessor;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Custom Veil-implemented render types.
 */
public final class VeilRenderType extends RenderType {

    private static final ShaderStateShard PARTICLE = VeilRenderBridge.shaderState(VeilShaders.PARTICLE);
    private static final ShaderStateShard PARTICLE_ADD = VeilRenderBridge.shaderState(VeilShaders.PARTICLE_ADD);

    private static final ShaderStateShard SKINNED_MESH = VeilRenderBridge.shaderState(VeilShaders.SKINNED_MESH);

    private static final BiFunction<ResourceLocation, Boolean, RenderType> QUASAR_PARTICLE = Util.memoize((texture, additive) -> {
        CompositeState state = RenderType.CompositeState.builder()
                .setShaderState(additive ? PARTICLE_ADD : PARTICLE)
                .setTextureState(new TextureStateShard(texture, false, false))
                .setTransparencyState(additive ? ADDITIVE_TRANSPARENCY : TRANSLUCENT_TRANSPARENCY)
                .setLightmapState(LIGHTMAP)
                .setWriteMaskState(additive ? COLOR_WRITE : COLOR_DEPTH_WRITE)
                .createCompositeState(false);
        return create(Veil.MODID + ":quasar_particle", DefaultVertexFormat.PARTICLE, VertexFormat.Mode.QUADS, SMALL_BUFFER_SIZE, false, !additive, state);
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
            if (((RenderTypeAccessor) layer).isSortOnUpload()) {
                sortOnUpload = true;
            }
            builder.add(layer);
        }
        return new LayeredRenderType(layers[0], builder.build(), "LayeredRenderType[" + Arrays.stream(layers).map(RenderType::toString) + "]", bufferSize, sortOnUpload);
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
    }
}
