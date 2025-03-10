package foundry.veil.impl.client.render.vertex;

import com.mojang.blaze3d.vertex.MeshData;
import foundry.veil.api.client.render.vertex.VertexArray;
import org.jetbrains.annotations.ApiStatus;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL15C.*;

@ApiStatus.Internal
public class LegacyVertexArray extends VertexArray {

    public LegacyVertexArray(int id) {
        super(id, LegacyVertexAttribBindingBuilder::new);
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
    protected int createBuffer() {
        return glGenBuffers();
    }

    @Override
    protected void uploadVertexBuffer(int buffer, ByteBuffer data, int usage) {
        glBindBuffer(GL_ARRAY_BUFFER, buffer);
        glBufferData(GL_ARRAY_BUFFER, data, usage);
    }
}
