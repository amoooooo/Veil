package foundry.veil.api.client.render.shader.definition;

import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.impl.client.render.shader.definition.*;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.NativeResource;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * <p>Defines a block of memory on the GPU that can be referenced as a uniform block.</p>
 * <p>{@link #update(Object)} changes the data in the block of memory.</p>
 * <p>{@link VeilRenderSystem#bind(CharSequence, ShaderBlock)} updates the data
 * if it has previously been changed with {@link #update(Object)}.</p>
 * <p>The end result is a lazy buffer that only updates contents when the Java data has been changed.</p>
 *
 * @param <T> The type of object to serialize
 * @author Ocelot
 */
public interface ShaderBlock<T> extends NativeResource {

    /**
     * Creates a new shader block with a fixed size.
     *
     * @param binding    The buffer attachment point
     * @param size       The size of the buffer in bytes
     * @param serializer The serializer to fill the buffer
     * @param <T>        The type of data to write
     * @return A new shader block
     */
    static <T> ShaderBlock<T> withSize(int binding, int size, BiConsumer<T, ByteBuffer> serializer) {
        return VeilRenderSystem.directStateAccessSupported() ? new DSASizedShaderBlockImpl<>(binding, size, serializer) : new SizedShaderBlockImpl<>(binding, size, serializer);
    }

    /**
     * Creates a new shader block with a dynamically-changing size. The initial size is set to <code>256</code>.
     *
     * @param binding    The buffer attachment point
     * @param serializer The serializer to fill the buffer
     * @param <T>        The type of data to write
     * @return A new shader block
     */
    static <T> DynamicShaderBlock<T> dynamic(int binding, BiConsumer<T, ByteBuffer> serializer) {
        return dynamic(binding, 256, serializer);
    }

    /**
     * Creates a new shader block with a dynamically-changing size.
     *
     * @param binding     The buffer attachment point
     * @param initialSize The initial size of the buffer
     * @param serializer  The serializer to fill the buffer
     * @param <T>         The type of data to write
     * @return A new shader block
     */
    static <T> DynamicShaderBlock<T> dynamic(int binding, int initialSize, BiConsumer<T, ByteBuffer> serializer) {
        return VeilRenderSystem.directStateAccessSupported() ? new DSADynamicShaderBlockImpl<>(binding, initialSize, serializer) : new DynamicShaderBlockImpl<>(binding, initialSize, serializer);
    }

    /**
     * Creates a new shader block that points to an existing GL buffer.
     *
     * @param binding The buffer attachment point
     * @param buffer  The buffer to bind as a shader block
     * @return A new shader block
     */
    static DynamicShaderBlock<?> wrapper(int binding, int buffer) {
        return new WrapperShaderBlockImpl(binding, buffer);
    }

    /**
     * Sets the value of this block. Data is only updated if the result of equals is <code>false</code>.
     *
     * @param value The new value
     */
    default void update(@Nullable T value) {
        if (!Objects.equals(this.getValue(), value)) {
            this.set(value);
        }
    }

    /**
     * Sets the value of this block. Sets the value regardless if it has changed or not.
     *
     * @param value The new value
     */
    void set(@Nullable T value);

    /**
     * @return The value stored in this block
     */
    @Nullable
    T getValue();
}
