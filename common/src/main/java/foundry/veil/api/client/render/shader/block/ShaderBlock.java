package foundry.veil.api.client.render.shader.block;

import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.impl.client.render.shader.block.DynamicShaderBlockImpl;
import foundry.veil.impl.client.render.shader.block.SizedShaderBlockImpl;
import foundry.veil.impl.client.render.shader.block.WrapperShaderBlockImpl;
import io.github.ocelot.glslprocessor.api.grammar.GlslTypeQualifier;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.NativeResource;

import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BiConsumer;

import static org.lwjgl.opengl.GL31C.GL_UNIFORM_BUFFER;
import static org.lwjgl.opengl.GL43C.GL_SHADER_STORAGE_BUFFER;

/**
 * <p>Defines a block of memory on the GPU that can be referenced as a uniform or shader block.</p>
 * <p>{@link #update(Object)} changes the data in the block of memory.</p>
 * <p>{@link VeilRenderSystem#bind(CharSequence, ShaderBlock)} must be called for shaders to access block data.</p>
 * <p>The result is a lazy buffer that only updates contents when the Java data has been changed.</p>
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
    static <T> ShaderBlock<T> withSize(BufferBinding binding, long size, BiConsumer<T, ByteBuffer> serializer) {
        return new SizedShaderBlockImpl<>(binding, size, VeilRenderSystem.directStateAccessSupported() ? new SizedShaderBlockImpl.DSASerializer<>(serializer) : new SizedShaderBlockImpl.LegacySerializer<>(serializer));
    }

    /**
     * Creates a new shader block with a dynamically-changing size. The initial size is set to <code>256</code>.
     *
     * @param binding    The buffer attachment point
     * @param serializer The serializer to fill the buffer
     * @param <T>        The type of data to write
     * @return A new shader block
     */
    static <T> DynamicShaderBlock<T> dynamic(BufferBinding binding, BiConsumer<T, ByteBuffer> serializer) {
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
    static <T> DynamicShaderBlock<T> dynamic(BufferBinding binding, long initialSize, BiConsumer<T, ByteBuffer> serializer) {
        return new DynamicShaderBlockImpl<>(binding, initialSize, VeilRenderSystem.directStateAccessSupported() ? new DynamicShaderBlockImpl.DSASerializer<>(serializer) : new DynamicShaderBlockImpl.LegacySerializer<>(serializer));
    }

    /**
     * Creates a new shader block that points to an existing GL buffer.
     *
     * @param binding The buffer attachment point
     * @param buffer  The buffer to bind as a shader block
     * @return A new shader block
     */
    static DynamicShaderBlock<?> wrapper(BufferBinding binding, int buffer) {
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

    /**
     * The bindings shaders blocks can be attached to.
     */
    enum BufferBinding {
        UNIFORM(GL_UNIFORM_BUFFER),
        SHADER_STORAGE(GL_SHADER_STORAGE_BUFFER);

        private final int glType;

        BufferBinding(int glType) {
            this.glType = glType;
        }

        public int getGlType() {
            return this.glType;
        }
    }

    /**
     * Valid memory layouts for blocks. Used to decide how to generate GLSL code.
     */
    enum MemoryLayout {
        PACKED, SHARED, STD140, STD430;

        private final GlslTypeQualifier.LayoutId layoutId;

        MemoryLayout() {
            this.layoutId = new GlslTypeQualifier.LayoutId(this.name().toLowerCase(Locale.ROOT), null);
        }

        public GlslTypeQualifier.LayoutId getLayoutId() {
            return this.layoutId;
        }
    }
}
