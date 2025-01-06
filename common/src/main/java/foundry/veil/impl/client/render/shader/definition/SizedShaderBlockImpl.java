package foundry.veil.impl.client.render.shader.definition;

import com.mojang.blaze3d.systems.RenderSystem;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.definition.ShaderBlock;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.ApiStatus;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.function.BiConsumer;

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

    protected final BiConsumer<T, ByteBuffer> serializer;
    private final int size;
    private ByteBuffer upload;

    public SizedShaderBlockImpl(int binding, int size, BiConsumer<T, ByteBuffer> serializer) {
        super(binding);
        this.serializer = serializer;
        this.size = size;
    }

    @Override
    public void bind(int index) {
        Validate.inclusiveBetween(0, VeilRenderSystem.maxTargetBindings(this.binding), index);

        if (this.buffer == 0) {
            this.buffer = glGenBuffers();
            RenderSystem.glBindBuffer(this.binding, this.buffer);
            glBufferData(this.binding, this.size, GL_DYNAMIC_DRAW);
            this.dirty = true;
        }

        if (this.dirty) {
            this.dirty = false;
            RenderSystem.glBindBuffer(this.binding, this.buffer);
            this.upload = glMapBuffer(this.binding, GL_WRITE_ONLY, this.size, this.upload);
            if (this.upload != null) {
                if (this.value != null) {
                    this.serializer.accept(this.value, this.upload);
                    this.upload.rewind();
                } else {
                    MemoryUtil.memSet(this.upload, 0);
                }
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
