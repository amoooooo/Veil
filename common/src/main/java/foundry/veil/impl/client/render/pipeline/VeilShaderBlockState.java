package foundry.veil.impl.client.render.pipeline;

import foundry.veil.Veil;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.definition.ShaderBlock;
import foundry.veil.impl.client.render.shader.definition.ShaderBlockImpl;
import foundry.veil.platform.VeilEventPlatform;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

/**
 * Manages the state of uniform block bindings and their associated shader names.
 *
 * @author Ocelot
 */
@ApiStatus.Internal
public class VeilShaderBlockState {

    // Flywheel uses the first 8 buffer bindings for instanced/indirect rendering
    public static final int RESERVED_BINDINGS = Veil.platform().isModLoaded("flywheel") ? 8 : 0;

    private final Object2IntMap<ShaderBlockImpl<?>> boundBlocks;
    private final Int2ObjectMap<CharSequence> shaderBindings;
    private final IntSet usedBindings;
    private int nextBinding;

    public VeilShaderBlockState() {
        this.boundBlocks = new Object2IntArrayMap<>();
        this.shaderBindings = new Int2ObjectArrayMap<>();
        this.usedBindings = new IntOpenHashSet();
        VeilEventPlatform.INSTANCE.onVeilShaderCompile((shaderManager, updatedPrograms) -> this.shaderBindings.clear());
    }

    /**
     * Looks for a stale binding that can be replaced with a new one.
     */
    private void freeBinding() {
        ObjectIterator<Object2IntMap.Entry<ShaderBlockImpl<?>>> iterator = this.boundBlocks.object2IntEntrySet().iterator();
        while (iterator.hasNext()) {
            Object2IntMap.Entry<ShaderBlockImpl<?>> entry = iterator.next();
            int binding = entry.getIntValue();
            if (this.usedBindings.contains(binding)) {
                continue;
            }

            this.unbind(binding, entry.getKey());
            iterator.remove();

            this.nextBinding = binding;
            return;
        }

        throw new IllegalStateException("Too many shader blocks bound, failed to find empty space.");
    }

    /**
     * Binds the specified block and returns the used binding.
     *
     * @param block The block to bind
     * @return The binding used
     */
    public int bind(ShaderBlock<?> block) {
        if (!(block instanceof ShaderBlockImpl<?> impl)) {
            throw new UnsupportedOperationException("Cannot bind " + block.getClass());
        }

        int binding = this.boundBlocks.getOrDefault(block, -1);
        if (binding == -1) {
            if (this.nextBinding >= VeilRenderSystem.maxUniformBuffersBindings() - RESERVED_BINDINGS) {
                this.freeBinding();
            }

            binding = this.nextBinding;
            this.boundBlocks.put(impl, binding);

            // Find the next open binding
            while (this.boundBlocks.containsValue(this.nextBinding)) {
                this.nextBinding++;
            }
        }

        impl.bind(binding + RESERVED_BINDINGS);
        this.usedBindings.add(binding);
        return binding;
    }

    /**
     * Binds and assigns the bound index to all shaders under the specified name.
     *
     * @param name  The name of the block to bind in shader code
     * @param block The block to bind
     */
    public void bind(CharSequence name, ShaderBlock<?> block) {
        if (!(block instanceof ShaderBlockImpl<?> impl)) {
            throw new UnsupportedOperationException("Cannot bind " + block.getClass());
        }

        int binding = this.bind(block);
        CharSequence boundName = this.shaderBindings.get(binding);
        if (!Objects.equals(name, boundName)) {
            this.shaderBindings.put(binding, name);
            VeilRenderSystem.renderer().getShaderManager().setGlobal(shader -> {
                switch (impl.getBinding()) {
                    case UNIFORM -> shader.setUniformBlock(name, binding);
                    case SHADER_STORAGE -> shader.setStorageBlock(name, binding);
                }
            });
        }
    }

    /**
     * Unbinds the specified shader block.
     *
     * @param block The block to unbind
     */
    public void unbind(ShaderBlock<?> block) {
        if (!(block instanceof ShaderBlockImpl<?> impl)) {
            throw new UnsupportedOperationException("Cannot unbind " + block.getClass());
        }

        if (this.boundBlocks.containsKey(block)) {
            this.unbind(this.boundBlocks.removeInt(block), impl);
        }
    }

    private void unbind(int binding, ShaderBlockImpl<?> block) {
        block.unbind(binding + RESERVED_BINDINGS);

        CharSequence name = this.shaderBindings.remove(binding);
        if (name != null) {
            VeilRenderSystem.renderer().getShaderManager().setGlobal(shader -> {
                switch (block.getBinding()) {
                    case UNIFORM -> shader.setUniformBlock(name, 0);
                    case SHADER_STORAGE -> shader.setStorageBlock(name, 0);
                }
            });
        }

        // Fill the gap since the spot is open now
        if (binding < this.nextBinding) {
            this.nextBinding = binding;
        }
    }

    /**
     * Clears all used bindings from the current frame.
     */
    public void clear() {
        this.usedBindings.clear();
    }
}