package foundry.veil.impl.client.render.vertex;

import foundry.veil.api.client.render.vertex.VertexArray;
import org.jetbrains.annotations.ApiStatus;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL15C.*;

@ApiStatus.Internal
public class ARBVertexAttribBindingVertexArray extends VertexArray {

    public ARBVertexAttribBindingVertexArray(int id) {
        super(id, ARBVertexAttribBindingBuilder::new);
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
}
