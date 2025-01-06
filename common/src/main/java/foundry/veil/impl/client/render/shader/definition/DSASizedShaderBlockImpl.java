package foundry.veil.impl.client.render.shader.definition;

import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.definition.ShaderBlock;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.ApiStatus;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.function.BiConsumer;

import static org.lwjgl.opengl.ARBDirectStateAccess.*;
import static org.lwjgl.opengl.GL15C.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL15C.GL_WRITE_ONLY;
import static org.lwjgl.opengl.GL30C.glBindBufferBase;

/**
 * Direct State Access Fixed-size implementation of {@link ShaderBlock}.
 *
 * @param <T> The type of object to serialize
 * @author Ocelot
 */
@ApiStatus.Internal
public class DSASizedShaderBlockImpl<T> extends ShaderBlockImpl<T> {

    protected final BiConsumer<T, ByteBuffer> serializer;
    private final int size;
    private ByteBuffer upload;

    public DSASizedShaderBlockImpl(int binding, int size, BiConsumer<T, ByteBuffer> serializer) {
        super(binding);
        this.serializer = serializer;
        this.size = size;
    }

    @Override
    public void bind(int index) {
        Validate.inclusiveBetween(0, VeilRenderSystem.maxTargetBindings(this.binding), index);

        if (this.buffer == 0) {
            this.buffer = glCreateBuffers();
            glNamedBufferData(this.buffer, this.size, GL_DYNAMIC_DRAW);
            this.dirty = true;
        }

        if (this.dirty) {
            this.dirty = false;
            this.upload = glMapNamedBuffer(this.buffer, GL_WRITE_ONLY, this.size, this.upload);
            if (this.upload != null) {
                if (this.value != null) {
                    this.serializer.accept(this.value, this.upload);
                    this.upload.rewind();
                } else {
                    MemoryUtil.memSet(this.upload, 0);
                }
            }
            if (!glUnmapNamedBuffer(this.buffer)) {
                this.dirty = true;
            }
        }

        glBindBufferBase(this.binding, index, this.buffer);
    }

    @Override
    public void unbind(int index) {
        Validate.inclusiveBetween(0, VeilRenderSystem.maxTargetBindings(this.binding), index);
        glBindBufferBase(this.binding, index, 0);
    }
}
