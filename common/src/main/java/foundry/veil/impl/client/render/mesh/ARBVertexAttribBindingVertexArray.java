package foundry.veil.impl.client.render.mesh;

import foundry.veil.api.client.render.mesh.VertexArray;
import foundry.veil.api.client.render.mesh.VertexArrayBuilder;
import org.jetbrains.annotations.ApiStatus;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL15C.*;

@ApiStatus.Internal
public class ARBVertexAttribBindingVertexArray extends VertexArray {

    public ARBVertexAttribBindingVertexArray(int id) {
        super(id);
    }

    @Override
    protected int createBuffer() {
        return glGenBuffers();
    }

    @Override
    protected void uploadVertexBuffer(int buffer, ByteBuffer data, int usage) {
        int old = glGetInteger(GL_ARRAY_BUFFER_BINDING);
        glBindBuffer(GL_ARRAY_BUFFER, buffer);
        glBufferData(GL_ARRAY_BUFFER, data, usage);
        glBindBuffer(GL_ARRAY_BUFFER, old);
    }

    @Override
    public VertexArrayBuilder editFormat() {
        this.bind();
        return new ARBVertexAttribBindingBuilder(this);
    }
}
