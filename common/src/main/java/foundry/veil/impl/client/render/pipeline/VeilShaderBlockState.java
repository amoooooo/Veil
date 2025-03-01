package foundry.veil.impl.client.render.pipeline;

import foundry.veil.Veil;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.definition.ShaderBlock;
import foundry.veil.impl.client.render.shader.definition.ShaderBlockImpl;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import org.jetbrains.annotations.ApiStatus;

import java.util.BitSet;
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

    private final ObjectSet<ShaderBlockImpl<?>> boundBlocks;
    private final BitSet usedBindings;
    private CharSequence[] shaderBindings;
    private int nextBinding;

    public VeilShaderBlockState() {
        this.boundBlocks = new ObjectArraySet<>();
        this.usedBindings = new BitSet();
        this.shaderBindings = null;
    }

    /**
     * Looks for a stale binding that can be replaced with a new one.
     */
    private void freeBinding() {
        ObjectIterator<ShaderBlockImpl<?>> iterator = this.boundBlocks.iterator();
        while (iterator.hasNext()) {
            ShaderBlockImpl<?> block = iterator.next();
            int index = block.getIndex();
            if (this.usedBindings.get(index)) {
                continue;
            }

            // Unbind the buffer if it has an index and move on
            if (index != -1) {
                block.unbind(index + RESERVED_BINDINGS);
                this.usedBindings.clear(index);
                block.setIndex(-1);
            }

            iterator.remove();
            this.nextBinding = index;
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
    private int bind(ShaderBlock<?> block) {
        if (!(block instanceof ShaderBlockImpl<?> impl)) {
            throw new UnsupportedOperationException("Cannot bind " + block.getClass());
        }

        int index = impl.getIndex();
        if (index == -1) {
            if (this.nextBinding >= VeilRenderSystem.maxUniformBuffersBindings() - RESERVED_BINDINGS) {
                this.freeBinding();
            }

            impl.setIndex(index = this.nextBinding);

            // Find the next open binding
            this.nextBinding = this.usedBindings.nextClearBit(this.nextBinding + 1);
        }

        this.boundBlocks.add(impl);
        this.usedBindings.set(index);
        impl.bind(index + RESERVED_BINDINGS);

        return index;
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
        if (this.shaderBindings == null) {
            this.shaderBindings = new CharSequence[VeilRenderSystem.maxUniformBuffersBindings() - RESERVED_BINDINGS];
        }

        CharSequence boundName = this.shaderBindings[binding];
        if (!Objects.equals(name, boundName)) {
            this.shaderBindings[binding] = name;
            VeilRenderSystem.renderer().getShaderManager().setGlobal(shader -> {
                switch (impl.getBinding()) {
                    case UNIFORM -> shader.setUniformBlock(name, binding + RESERVED_BINDINGS);
                    case SHADER_STORAGE -> shader.setStorageBlock(name, binding + RESERVED_BINDINGS);
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

        int index = impl.getIndex();
        if (index == -1) {
            return;
        }

        impl.unbind(index + RESERVED_BINDINGS);
        this.boundBlocks.remove(impl);
        this.usedBindings.clear(index);
        impl.setIndex(-1);

        if (this.shaderBindings != null) {
            CharSequence name = this.shaderBindings[index];
            if (name != null) {
                this.shaderBindings[index] = null;
                VeilRenderSystem.renderer().getShaderManager().setGlobal(shader -> {
                    switch (impl.getBinding()) {
                        case UNIFORM -> shader.setUniformBlock(name, 0);
                        case SHADER_STORAGE -> shader.setStorageBlock(name, 0);
                    }
                });
            }
        }

        // Fill the gap since the spot is open now
        if (index < this.nextBinding) {
            this.nextBinding = index;
        }
    }

    /**
     * Forces all shader bindings to be updated next frame.
     */
    public void onShaderCompile() {
        this.shaderBindings = null;
    }

    /**
     * Clears all used bindings from the current frame.
     */
    public void clearUsedBindings() {
        this.usedBindings.clear();
    }
}