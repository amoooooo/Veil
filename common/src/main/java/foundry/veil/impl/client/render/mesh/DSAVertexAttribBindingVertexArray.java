package foundry.veil.impl.client.render.mesh;

import foundry.veil.api.client.render.mesh.VertexArray;
import foundry.veil.api.client.render.mesh.VertexArrayBuilder;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.ARBDirectStateAccess.glNamedBufferData;

@ApiStatus.Internal
public class DSAVertexAttribBindingVertexArray extends VertexArray {

    public DSAVertexAttribBindingVertexArray(int id) {
        super(id);
    }

    @Override
    protected void uploadVertexBuffer(VertexArrayBuilder builder, int buffer, int usage, @Nullable ByteBuffer data) {
        if (data != null) {
            glNamedBufferData(this.getOrCreateBuffer(VERTEX_BUFFER), buffer, usage);
        }
    }

    @Override
    public VertexArrayBuilder editFormat() {
        return new DSAVertexAttribBindingBuilder(this.id);
    }
}
