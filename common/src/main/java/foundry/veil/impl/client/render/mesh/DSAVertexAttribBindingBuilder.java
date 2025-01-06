package foundry.veil.impl.client.render.mesh;

import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.mesh.VertexArrayBuilder;
import org.jetbrains.annotations.ApiStatus;

import static org.lwjgl.opengl.ARBDirectStateAccess.*;

@ApiStatus.Internal
public record DSAVertexAttribBindingBuilder(int vao) implements VertexArrayBuilder {

    @Override
    public VertexArrayBuilder defineVertexBuffer(int index, int buffer, int offset, int size) {
        glVertexArrayVertexBuffer(this.vao, index, buffer, offset, size);
        return this;
    }

    @Override
    public VertexArrayBuilder setVertexAttribute(int index, int bufferIndex, int size, DataType type, boolean normalized, int relativeOffset, int divisor) {
        VertexArrayBuilder.validateRelativeOffset(relativeOffset);
        glEnableVertexArrayAttrib(this.vao, index);
        glVertexArrayAttribFormat(this.vao, index, size, type.getGlType(), normalized, relativeOffset);
        glVertexArrayAttribBinding(this.vao, index, bufferIndex);
        glVertexArrayBindingDivisor(this.vao, index, divisor);
        return this;
    }

    @Override
    public VertexArrayBuilder setVertexIAttribute(int index, int bufferIndex, int size, DataType type, int relativeOffset, int divisor) {
        VertexArrayBuilder.validateRelativeOffset(relativeOffset);
        glEnableVertexArrayAttrib(this.vao, index);
        glVertexArrayAttribIFormat(this.vao, index, size, type.getGlType(), relativeOffset);
        glVertexArrayAttribBinding(this.vao, index, bufferIndex);
        glVertexArrayBindingDivisor(this.vao, index, divisor);
        return this;
    }

    @Override
    public VertexArrayBuilder setVertexLAttribute(int index, int bufferIndex, int size, DataType type, int relativeOffset, int divisor) {
        VertexArrayBuilder.validateRelativeOffset(relativeOffset);
        glEnableVertexArrayAttrib(this.vao, index);
        glVertexArrayAttribLFormat(this.vao, index, size, type.getGlType(), relativeOffset);
        glVertexArrayAttribBinding(this.vao, index, bufferIndex);
        glVertexArrayBindingDivisor(this.vao, index, divisor);
        return this;
    }

    @Override
    public VertexArrayBuilder removeVertexBuffer(int index) {
        glVertexArrayVertexBuffer(this.vao, index, 0, 0, 0);
        return this;
    }

    @Override
    public VertexArrayBuilder removeAttribute(int index) {
        glDisableVertexArrayAttrib(this.vao, index);
        return this;
    }

    @Override
    public VertexArrayBuilder clearVertexBuffers() {
        for (int i = 0; i < VeilRenderSystem.maxVertexAttributes(); i++) {
            glVertexArrayVertexBuffer(this.vao, i, 0, 0, 0);
        }
        return this;
    }

    @Override
    public VertexArrayBuilder clearVertexAttributes() {
        for (int i = 0; i < VeilRenderSystem.maxVertexAttributes(); i++) {
            glDisableVertexArrayAttrib(this.vao, i);
        }
        return this;
    }
}
