package foundry.veil.impl.client.render.shader.definition;

import com.mojang.blaze3d.systems.RenderSystem;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.definition.DynamicShaderBlock;
import foundry.veil.api.client.render.shader.definition.ShaderBlock;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryStack;

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

    /**
     * Creates a new shader block of specified initial size.
     *
     * @param size       The initial size of the shader buffer
     * @param serializer The serializer of values
     */
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
            this.resized = false;
            this.dirty = true;
            RenderSystem.glBindBuffer(this.binding, this.buffer);
            glBufferData(this.binding, this.size, GL_DYNAMIC_DRAW);
            RenderSystem.glBindBuffer(this.binding, 0);
        }

        if (this.dirty) {
            this.dirty = false;
            RenderSystem.glBindBuffer(this.binding, this.buffer);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                if (this.value != null) {
                    ByteBuffer buffer = stack.malloc((int) this.size);
                    this.serializer.accept(this.value, buffer);
                    buffer.rewind();
                    glBufferSubData(this.binding, 0, buffer);
                } else {
                    glBufferSubData(this.binding, 0, stack.calloc((int) this.size));
                }
            }
            RenderSystem.glBindBuffer(this.binding, 0);
        }

        glBindBufferBase(this.binding, index, this.buffer);
    }

    @Override
    public void unbind(int index) {
        Validate.inclusiveBetween(0, VeilRenderSystem.maxTargetBindings(this.binding), index);
        glBindBufferBase(this.binding, index, 0);
    }
}
