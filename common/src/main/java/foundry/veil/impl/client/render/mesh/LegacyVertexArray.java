package foundry.veil.impl.client.render.mesh;

import com.mojang.blaze3d.vertex.MeshData;
import foundry.veil.api.client.render.mesh.VertexArray;
import foundry.veil.api.client.render.mesh.VertexArrayBuilder;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL15C.*;

@ApiStatus.Internal
public class LegacyVertexArray extends VertexArray {

    public LegacyVertexArray(int id) {
        super(id);
    }

    @Override
    public void upload(int attributeStart, MeshData meshData, DrawUsage usage) {
        int old = glGetInteger(GL_ARRAY_BUFFER_BINDING);
        try {
            super.upload(attributeStart, meshData, usage);
        } finally {
            glBindBuffer(GL_ARRAY_BUFFER, old);
        }
    }

    @Override
    protected void uploadVertexBuffer(VertexArrayBuilder builder, int buffer, int usage, @Nullable ByteBuffer data) {
        if (data != null) {
            glBindBuffer(GL_ARRAY_BUFFER, this.getOrCreateBuffer(VERTEX_BUFFER));
            glBufferData(GL_ARRAY_BUFFER, buffer, usage);
        }
    }

    @Override
    public VertexArrayBuilder editFormat() {
        this.bind();
        return new LegacyVertexAttribBindingBuilder();
    }
}
