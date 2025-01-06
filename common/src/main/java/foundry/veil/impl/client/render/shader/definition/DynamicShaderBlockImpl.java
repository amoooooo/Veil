package foundry.veil.impl.client.render.shader.definition;

import com.mojang.blaze3d.systems.RenderSystem;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.definition.DynamicShaderBlock;
import foundry.veil.api.client.render.shader.definition.ShaderBlock;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.function.BiConsumer;

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

    protected final BiConsumer<T, ByteBuffer> serializer;

    private long size;
    private boolean resized;
    private ByteBuffer upload;

    public DynamicShaderBlockImpl(int binding, long size, @NotNull BiConsumer<T, ByteBuffer> serializer) {
        super(binding);
        this.serializer = serializer;
        this.size = size;
        this.resized = false;
    }

    @Override
    public void setSize(long size) {
        this.size = size;
        this.resized = true;
    }

    @Override
    public void bind(int index) {
        Validate.inclusiveBetween(0, VeilRenderSystem.maxTargetBindings(this.binding), index);

        if (this.buffer == 0) {
            this.buffer = glGenBuffers();
            this.resized = true;
        }

        if (this.resized) {
            this.dirty = true;
            RenderSystem.glBindBuffer(this.binding, this.buffer);
            glBufferData(this.binding, this.size, GL_DYNAMIC_DRAW);
        }

        if (this.dirty) {
            if (!this.resized) { // Only bind the buffer once
                RenderSystem.glBindBuffer(this.binding, this.buffer);
            }

            this.upload = glMapBuffer(this.binding, GL_WRITE_ONLY, this.size, this.upload);
            if (this.upload != null) {
                if (this.value != null) {
                    this.serializer.accept(this.value, this.upload);
                    this.upload.rewind();
                } else {
                    MemoryUtil.memSet(this.upload, 0);
                }
            }
            glUnmapBuffer(this.binding);
        }

        this.resized = false;
        this.dirty = false;

        glBindBufferBase(this.binding, index, this.buffer);
    }

    @Override
    public void unbind(int index) {
        Validate.inclusiveBetween(0, VeilRenderSystem.maxTargetBindings(this.binding), index);
        glBindBufferBase(this.binding, index, 0);
    }
}
