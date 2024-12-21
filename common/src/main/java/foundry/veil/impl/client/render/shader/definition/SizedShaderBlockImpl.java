package foundry.veil.impl.client.render.shader.definition;

import com.mojang.blaze3d.systems.RenderSystem;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.definition.ShaderBlock;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.ApiStatus;
import org.lwjgl.system.MemoryStack;

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
            RenderSystem.glBindBuffer(this.binding, 0);
            this.dirty = true;
        }

        if (this.dirty) {
            this.dirty = false;
            RenderSystem.glBindBuffer(this.binding, this.buffer);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                if (this.value != null) {
                    ByteBuffer buffer = stack.malloc(this.size);
                    this.serializer.accept(this.value, buffer);
                    buffer.rewind();
                    glBufferSubData(this.binding, 0, buffer);
                } else {
                    glBufferSubData(this.binding, 0, stack.calloc(this.size));
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
