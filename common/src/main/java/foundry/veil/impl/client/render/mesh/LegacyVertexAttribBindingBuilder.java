package foundry.veil.impl.client.render.mesh;

import com.mojang.blaze3d.systems.RenderSystem;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.mesh.VertexArrayBuilder;
import org.jetbrains.annotations.ApiStatus;

import java.util.Arrays;

import static org.lwjgl.opengl.GL33C.*;

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
    public VertexArrayBuilder defineVertexBuffer(int index, int buffer, int offset, int stride) {
        this.vertexBuffers[index] = new VertexBuffer(buffer, offset, stride);
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
        glVertexAttribDivisor(index, divisor);
        return this;
    }

    @Override
    public VertexArrayBuilder setVertexIAttribute(int index, int bufferIndex, int size, DataType type, int relativeOffset, int divisor) {
        VertexArrayBuilder.validateRelativeOffset(relativeOffset);
        this.bindIndex(bufferIndex);
        glEnableVertexAttribArray(index);
        glVertexAttribIPointer(index, size, type.getGlType(), this.vertexBuffers[this.boundIndex].size, this.vertexBuffers[this.boundIndex].offset + relativeOffset);
        glVertexAttribDivisor(index, divisor);
        return this;
    }

    @Override
    public VertexArrayBuilder setVertexLAttribute(int index, int bufferIndex, int size, DataType type, int relativeOffset, int divisor) {
        throw new UnsupportedOperationException("Long attributes not supported");
    }

    @Override
    public VertexArrayBuilder removeVertexBuffer(int index) {
        this.vertexBuffers[index] = null;
        return this;
    }

    @Override
    public VertexArrayBuilder removeAttribute(int index) {
        glDisableVertexAttribArray(index);
        return this;
    }

    @Override
    public VertexArrayBuilder clearVertexBuffers() {
        Arrays.fill(this.vertexBuffers, null);
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
