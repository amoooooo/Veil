package foundry.veil.impl.client.render.pipeline;

import foundry.veil.Veil;
import foundry.veil.api.client.registry.VeilShaderBufferRegistry;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.VeilShaderBufferLayout;
import foundry.veil.api.client.render.shader.definition.ShaderBlock;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import foundry.veil.impl.client.render.LayoutSerializer;
import foundry.veil.impl.client.render.shader.definition.LayoutShaderBlockImpl;
import foundry.veil.impl.client.render.shader.definition.ShaderBlockImpl;
import foundry.veil.platform.VeilEventPlatform;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Manages the existence of registered shader blocks.
 *
 * @author Ocelot
 */
@ApiStatus.Internal
public class VeilShaderBufferCache {

    private final LayoutShaderBlockImpl<?>[] values;
    private final VeilShaderBufferLayout<?>[] layouts;

    public VeilShaderBufferCache() {
        this.layouts = VeilShaderBufferRegistry.REGISTRY.stream().toArray(VeilShaderBufferLayout[]::new);
        this.values = new LayoutShaderBlockImpl[this.layouts.length];

        VeilEventPlatform.INSTANCE.onVeilShaderCompile((shaderManager, updatedPrograms) -> {
            for (int i = 0; i < this.values.length; i++) {
                LayoutShaderBlockImpl<?> block = this.values[i];
                if (block != null) {
                    Set<ResourceLocation> shaders = block.getReferencedShaders();
                    if (shaders.removeAll(updatedPrograms.keySet()) && shaders.isEmpty()) {
                        // Since no old shaders reference it anymore, delete it and allow it to be created again
                        block.free();
                        this.values[i] = null;
                        Veil.LOGGER.info("Deleting block: {}", this.layouts[i].name());
                    }
                }

                VeilShaderBufferLayout<?> layout = this.layouts[i];
                String name = layout.name();
                for (ShaderProgram shader : updatedPrograms.values()) {
                    int index = switch (layout.binding()) {
                        case UNIFORM -> shader.getUniformBlock(name);
                        case SHADER_STORAGE -> shader.getStorageBlock(name);
                    };

                    if (index == -1) {
                        continue;
                    }

                    if (this.values[i] == null) {
                        block = LayoutSerializer.create(layout, shader, name, index);
                        block.getReferencedShaders().add(shader.getId());
                        this.values[i] = block;
                        Veil.LOGGER.info("Defined {} from shader: {}", name, shader.getId());
                    } else {
                        this.values[i].getReferencedShaders().add(shader.getId());
                        Veil.LOGGER.info("Adding {} to shader: {}", name, shader.getId());
                    }
                    break;
                }

                // Validate only a single shader uses the block
                if (this.layouts[i].memoryLayout() == ShaderBlock.MemoryLayout.PACKED) {
                    block = this.values[i];
                    if (block == null) {
                        continue;
                    }
                    Set<ResourceLocation> shaders = block.getReferencedShaders();
                    if (shaders.size() != 1) {
                        String error = shaders.stream().map(ResourceLocation::toString).collect(Collectors.joining(", "));
                        Veil.LOGGER.error("Shader Block {} uses the 'packed' memory layout and only supports a single shader using the block. Either use a different format or only use the block in one shader. Affected shaders: {}", name, error);
                    }
                }
            }
        });
    }

    public void bind() {
        for (int i = 0; i < this.values.length; i++) {
            LayoutShaderBlockImpl<?> block = this.values[i];
            if (block != null) {
                VeilRenderSystem.bind(this.layouts[i].name(), block);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <T> @Nullable ShaderBlock<T> getBlock(VeilShaderBufferLayout<T> layout) throws IllegalArgumentException {
        int id = VeilShaderBufferRegistry.REGISTRY.getId(layout);
        if (id < 0 || id >= this.values.length) {
            throw new IllegalArgumentException("Attempted to use unregistered buffer layout: " + layout.name());
        }
        return (ShaderBlockImpl<T>) this.values[id];
    }
}