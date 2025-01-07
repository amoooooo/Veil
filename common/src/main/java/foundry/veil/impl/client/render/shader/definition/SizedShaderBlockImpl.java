package foundry.veil.impl.client.render.shader.definition;

import com.mojang.blaze3d.systems.RenderSystem;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.definition.ShaderBlock;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.function.BiConsumer;

import static org.lwjgl.opengl.ARBDirectStateAccess.*;
import static org.lwjgl.opengl.GL15C.*;
import static org.lwjgl.opengl.GL30C.glBindBufferBase;

/**
 * Fixed-size implementation of {@link ShaderBlock}.
 *
 * @param <T> The type of object to serialize
 * @author Ocelot
 */
@ApiStatus.Internal
public class SizedShaderBlockImpl<T> extends ShaderBlockImpl<T> {

    private final Serializer<T> serializer;
    private final long size;

    public SizedShaderBlockImpl(BufferBinding binding, long size, Serializer<T> serializer) {
        super(binding);
        this.serializer = serializer;
        this.size = size;
    }

    @Override
    public void bind(int index) {
        int binding = this.binding.getGlType();
        Validate.inclusiveBetween(0, VeilRenderSystem.maxTargetBindings(binding), index);

        if (this.buffer == 0) {
            this.dirty = true;
            this.buffer = this.serializer.createBuffer(binding, this.size);
        }

        if (this.dirty && this.serializer.write(binding, this.buffer, this.size, this.value)) {
            this.dirty = false;
        }

        glBindBufferBase(binding, index, this.buffer);
    }

    @Override
    public void unbind(int index) {
        int binding = this.binding.getGlType();
        Validate.inclusiveBetween(0, VeilRenderSystem.maxTargetBindings(binding), index);
        glBindBufferBase(binding, index, 0);
    }

    public sealed interface Serializer<T> {

        int createBuffer(int binding, long size);

        boolean write(int binding, int buffer, long size, @Nullable T value);
    }

    public static final class DSASerializer<T> implements Serializer<T> {

        private final BiConsumer<T, ByteBuffer> serializer;
        private ByteBuffer upload;

        public DSASerializer(BiConsumer<T, ByteBuffer> serializer) {
            this.serializer = serializer;
        }

        @Override
        public int createBuffer(int binding, long size) {
            int buffer = glCreateBuffers();
            glNamedBufferData(buffer, size, GL_DYNAMIC_DRAW);
            return buffer;
        }

        @Override
        public boolean write(int binding, int buffer, long size, @Nullable T value) {
            this.upload = glMapNamedBuffer(buffer, GL_WRITE_ONLY, size, this.upload);
            if (this.upload != null) {
                if (value != null) {
                    this.serializer.accept(value, this.upload);
                    this.upload.rewind();
                } else {
                    MemoryUtil.memSet(this.upload, 0);
                }
            }
            return glUnmapNamedBuffer(buffer);
        }
    }

    public static final class LegacySerializer<T> implements Serializer<T> {

        private final BiConsumer<T, ByteBuffer> serializer;
        private ByteBuffer upload;

        public LegacySerializer(BiConsumer<T, ByteBuffer> serializer) {
            this.serializer = serializer;
        }

        @Override
        public int createBuffer(int binding, long size) {
            int buffer = glGenBuffers();
            RenderSystem.glBindBuffer(binding, buffer);
            glBufferData(binding, size, GL_DYNAMIC_DRAW);
            return buffer;
        }

        @Override
        public boolean write(int binding, int buffer, long size, @Nullable T value) {
            RenderSystem.glBindBuffer(binding, buffer);
            this.upload = glMapBuffer(binding, GL_READ_WRITE, size, this.upload);
            if (this.upload != null) {
                if (value != null) {
                    this.serializer.accept(value, this.upload);
                    this.upload.rewind();
                } else {
                    MemoryUtil.memSet(this.upload, 0);
                }
            }
            return glUnmapBuffer(binding);
        }
    }
}
