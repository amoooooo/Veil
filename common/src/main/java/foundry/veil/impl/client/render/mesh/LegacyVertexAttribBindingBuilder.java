package foundry.veil.impl.client.render.mesh;

import com.mojang.blaze3d.systems.RenderSystem;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.mesh.VertexArrayBuilder;
import org.jetbrains.annotations.ApiStatus;

import static org.lwjgl.opengl.ARBVertexAttribBinding.*;
import static org.lwjgl.opengl.GL20C.*;

@ApiStatus.Internal
public class LegacyVertexAttribBindingBuilder implements VertexArrayBuilder {

    private final VertexBuffer[] vertexBuffers;
    private int boundIndex = -1;

    public LegacyVertexAttribBindingBuilder() {
        this.vertexBuffers = new VertexBuffer[VeilRenderSystem.maxVertexAttributes()];
    }

    private record VertexBuffer(int buffer, int offset, int size) {
    }

    @Override
    public VertexArrayBuilder defineVertexBuffer(int index, int buffer, int offset, int size) {
        this.vertexBuffers[index] = new VertexBuffer(buffer, offset, size);
        return this;
    }

    private void bindIndex(int index) {
        if (index < 0 || index >= this.vertexBuffers.length) {
            throw new IllegalArgumentException("Invalid vertex attribute index. Must be between 0 and " + (this.vertexBuffers.length - 1) + ": " + index);
        }

        if (this.boundIndex != index) {
            if (this.vertexBuffers[index] == null) {
                throw new IllegalArgumentException("No vertex buffer defined for index: " + index);
            }

            RenderSystem.glBindBuffer(GL_ARRAY_BUFFER, this.vertexBuffers[index].buffer);
            this.boundIndex = index;
        }
    }

    @Override
    public VertexArrayBuilder setVertexAttribute(int index, int bufferIndex, int size, DataType type, boolean normalized, int relativeOffset, int divisor) {
        VertexArrayBuilder.validateRelativeOffset(relativeOffset);
        this.bindIndex(bufferIndex);
        glEnableVertexAttribArray(index);
        glVertexAttribPointer(index, size, type.getGlType(), normalized, this.vertexBuffers[this.boundIndex].size, this.vertexBuffers[this.boundIndex].offset + relativeOffset);
        glVertexBindingDivisor(index, divisor);
        return this;
    }

    @Override
    public VertexArrayBuilder setVertexIAttribute(int index, int bufferIndex, int size, DataType type, int relativeOffset, int divisor) {
        VertexArrayBuilder.validateRelativeOffset(relativeOffset);
        this.bindIndex(bufferIndex);
        glEnableVertexAttribArray(index);
        glVertexAttribIFormat(index, size, type.getGlType(), relativeOffset);
        glVertexAttribBinding(index, bufferIndex);
        glVertexBindingDivisor(index, divisor);
        return this;
    }

    @Override
    public VertexArrayBuilder setVertexLAttribute(int index, int bufferIndex, int size, DataType type, int relativeOffset, int divisor) {
        VertexArrayBuilder.validateRelativeOffset(relativeOffset);
        this.bindIndex(bufferIndex);
        glEnableVertexAttribArray(index);
        glVertexAttribLFormat(index, size, type.getGlType(), relativeOffset);
        glVertexAttribBinding(index, bufferIndex);
        glVertexBindingDivisor(index, divisor);
        return this;
    }

    @Override
    public VertexArrayBuilder removeVertexBuffer(int index) {
        glBindVertexBuffer(index, 0, 0, 0);
        return this;
    }

    @Override
    public VertexArrayBuilder removeAttribute(int index) {
        glDisableVertexAttribArray(index);
        return this;
    }

    @Override
    public VertexArrayBuilder clearVertexBuffers() {
        for (int i = 0; i < VeilRenderSystem.maxVertexAttributes(); i++) {
            glBindVertexBuffer(i, 0, 0, 0);
        }
        return this;
    }

    @Override
    public VertexArrayBuilder clearVertexAttributes() {
        for (int i = 0; i < VeilRenderSystem.maxVertexAttributes(); i++) {
            glDisableVertexAttribArray(i);
        }
        return this;
    }
}
