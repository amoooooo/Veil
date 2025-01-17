package foundry.veil.impl.client.render.mesh;

import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.mesh.VertexArray;
import foundry.veil.api.client.render.mesh.VertexArrayBuilder;
import org.jetbrains.annotations.ApiStatus;

import static org.lwjgl.opengl.ARBVertexAttribBinding.*;
import static org.lwjgl.opengl.GL20C.glDisableVertexAttribArray;
import static org.lwjgl.opengl.GL20C.glEnableVertexAttribArray;

@ApiStatus.Internal
public record ARBVertexAttribBindingBuilder(VertexArray vertexArray) implements VertexArrayBuilder {

    @Override
    public VertexArrayBuilder defineVertexBuffer(int index, int buffer, int offset, int stride, int divisor) {
        glBindVertexBuffer(index, buffer, offset, stride);
        glVertexBindingDivisor(index, divisor);
        return this;
    }

    @Override
    public VertexArrayBuilder setVertexAttribute(int index, int bufferIndex, int size, DataType type, boolean normalized, int relativeOffset) {
        glEnableVertexAttribArray(index);
        glVertexAttribFormat(index, size, type.getGlType(), normalized, relativeOffset);
        glVertexAttribBinding(index, bufferIndex);
        return this;
    }

    @Override
    public VertexArrayBuilder setVertexIAttribute(int index, int bufferIndex, int size, DataType type, int relativeOffset) {
        glEnableVertexAttribArray(index);
        glVertexAttribIFormat(index, size, type.getGlType(), relativeOffset);
        glVertexAttribBinding(index, bufferIndex);
        return this;
    }

    @Override
    public VertexArrayBuilder setVertexLAttribute(int index, int bufferIndex, int size, DataType type, int relativeOffset) {
        glEnableVertexAttribArray(index);
        glVertexAttribLFormat(index, size, type.getGlType(), relativeOffset);
        glVertexAttribBinding(index, bufferIndex);
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
