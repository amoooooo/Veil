package foundry.veil.impl.client.render.shader.definition;

import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.definition.DynamicShaderBlock;
import foundry.veil.api.client.render.shader.definition.ShaderBlock;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.function.BiConsumer;

import static org.lwjgl.opengl.ARBDirectStateAccess.*;
import static org.lwjgl.opengl.GL15C.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL15C.GL_WRITE_ONLY;
import static org.lwjgl.opengl.GL30C.glBindBufferBase;

/**
 * Direct State Access Dynamic-size implementation of {@link ShaderBlock}.
 *
 * @param <T> The type of object to serialize
 * @author Ocelot
 */
@ApiStatus.Internal
public class DSADynamicShaderBlockImpl<T> extends ShaderBlockImpl<T> implements DynamicShaderBlock<T> {

    protected final BiConsumer<T, ByteBuffer> serializer;

    private long size;
    private boolean resized;
    private ByteBuffer upload;

    public DSADynamicShaderBlockImpl(int binding, long size, @NotNull BiConsumer<T, ByteBuffer> serializer) {
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
            this.buffer = glCreateBuffers();
            this.resized = true;
        }

        if (this.resized) {
            this.dirty = true;
            glNamedBufferData(this.buffer, this.size, GL_DYNAMIC_DRAW);
        }

        if (this.dirty) {
            this.upload = glMapNamedBuffer(this.buffer, GL_WRITE_ONLY, this.size, this.upload);
            if (this.upload != null) {
                if (this.value != null) {
                    this.serializer.accept(this.value, this.upload);
                    this.upload.rewind();
                } else {
                    MemoryUtil.memSet(this.upload, 0);
                }
            }
            if (glUnmapNamedBuffer(this.buffer)) {
                this.dirty = false;
            }
        }

        this.resized = false;
        glBindBufferBase(this.binding, index, this.buffer);
    }

    @Override
    public void unbind(int index) {
        Validate.inclusiveBetween(0, VeilRenderSystem.maxTargetBindings(this.binding), index);
        glBindBufferBase(this.binding, index, 0);
    }
}
