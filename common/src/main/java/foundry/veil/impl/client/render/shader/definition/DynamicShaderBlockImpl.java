package foundry.veil.impl.client.render.shader.definition;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.definition.DynamicShaderBlock;
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
 * Dynamic-size implementation of {@link ShaderBlock}.
 *
 * @param <T> The type of object to serialize
 * @author Ocelot
 */
@ApiStatus.Internal
public class DynamicShaderBlockImpl<T> extends ShaderBlockImpl<T> implements DynamicShaderBlock<T> {

    private final Serializer<T> serializer;
    private long size;
    private boolean resized;

    public DynamicShaderBlockImpl(BufferBinding binding, long initialSize, Serializer<T> serializer) {
        super(binding);
        this.serializer = serializer;
        this.size = initialSize;
        this.resized = false;
    }

    @Override
    public long getSize() {
        return this.size;
    }

    @Override
    public void setSize(long size) {
        this.size = size;
        this.resized = true;
    }

    @Override
    public void bind(int index) {
        int binding = this.binding.getGlType();
        Validate.inclusiveBetween(0, VeilRenderSystem.maxTargetBindings(binding), index);

        if (this.buffer == 0) {
            this.resized = true;
            this.buffer = GlStateManager._glGenBuffers();
        }

        if (this.resized) {
            this.resized = false;
            this.dirty = true;
            this.serializer.resize(binding, this.buffer, this.size);
        }

        if (this.dirty && this.serializer.write(this.buffer, binding, this.size, this.value)) {
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

        void resize(int binding, int buffer, long size);

        boolean write(int buffer, int binding, long size, @Nullable T value);
    }

    public static final class DSASerializer<T> implements Serializer<T> {

        private final BiConsumer<T, ByteBuffer> serializer;
        private ByteBuffer upload;

        public DSASerializer(BiConsumer<T, ByteBuffer> serializer) {
            this.serializer = serializer;
        }

        @Override
        public void resize(int binding, int buffer, long size) {
            glNamedBufferData(buffer, size, GL_DYNAMIC_DRAW);
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
        public void resize(int binding, int buffer, long size) {
            RenderSystem.glBindBuffer(binding, buffer);
            glBufferData(binding, size, GL_DYNAMIC_DRAW);
        }

        @Override
        public boolean write(int binding, int buffer, long size, @Nullable T value) {
            RenderSystem.glBindBuffer(binding, buffer);
            this.upload = glMapBuffer(binding, GL_WRITE_ONLY, size, this.upload);
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
