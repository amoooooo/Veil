package foundry.veil.impl.client.render.shader.block;

import com.mojang.blaze3d.platform.GlStateManager;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.block.ShaderBlock;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * Abstract implementation of {@link ShaderBlock}.
 *
 * @param <T> The type of object to serialize
 * @author Ocelot
 */
@ApiStatus.Internal
public abstract class ShaderBlockImpl<T> implements ShaderBlock<T> {

    protected final BufferBinding binding;
    protected int buffer;
    protected T value;
    protected boolean dirty;

    private int index;

    protected ShaderBlockImpl(BufferBinding binding) {
        this.binding = binding;
        this.buffer = 0;
        this.value = null;
        this.dirty = false;
        this.index = -1;
    }

    @Override
    public void set(@Nullable T value) {
        this.value = value;
        this.dirty = true;
    }

    /**
     * Binds this block to the specified index.
     *
     * @param index The index to bind this block to
     */
    public abstract void bind(int index);

    /**
     * Unbinds this block from the specified index.
     *
     * @param index The index to unbind this block from
     */
    public abstract void unbind(int index);

    public BufferBinding getBinding() {
        return this.binding;
    }

    public int getIndex() {
        return this.index;
    }

    @Override
    public @Nullable T getValue() {
        return this.value;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    public void free() {
        VeilRenderSystem.unbind(this);
        if (this.buffer != 0) {
            GlStateManager._glDeleteBuffers(this.buffer);
            this.buffer = 0;
        }
    }
}
