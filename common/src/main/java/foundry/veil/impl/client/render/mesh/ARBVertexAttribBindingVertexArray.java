package foundry.veil.impl.client.render.mesh;

import foundry.veil.api.client.render.mesh.VertexArray;
import foundry.veil.api.client.render.mesh.VertexArrayBuilder;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL15C.*;

@ApiStatus.Internal
public class ARBVertexAttribBindingVertexArray extends VertexArray {

    public ARBVertexAttribBindingVertexArray(int id) {
        super(id);
    }

    @Override
    protected void uploadVertexBuffer(VertexArrayBuilder builder, int buffer, int usage, @Nullable ByteBuffer data) {
        if (data != null) {
            int old = glGetInteger(GL_ARRAY_BUFFER_BINDING);
            glBindBuffer(GL_ARRAY_BUFFER, this.getOrCreateBuffer(VERTEX_BUFFER));
            glBufferData(GL_ARRAY_BUFFER, buffer, usage);
            glBindBuffer(GL_ARRAY_BUFFER, old);
        }
    }

    @Override
    public VertexArrayBuilder editFormat() {
        this.bind();
        return ARBVertexAttribBindingBuilder.INSTANCE;
    }
}
