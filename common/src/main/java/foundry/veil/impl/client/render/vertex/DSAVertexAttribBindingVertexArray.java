package foundry.veil.impl.client.render.vertex;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import foundry.veil.api.client.render.vertex.VertexArray;
import foundry.veil.ext.AutoStorageIndexBufferExtension;
import org.jetbrains.annotations.ApiStatus;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.ARBDirectStateAccess.*;
import static org.lwjgl.opengl.GL15C.GL_STATIC_DRAW;

@ApiStatus.Internal
public class DSAVertexAttribBindingVertexArray extends VertexArray {

    public DSAVertexAttribBindingVertexArray(int id) {
        super(id, vao -> new DSAVertexAttribBindingBuilder(vao, id));
    }

    @Override
    public void uploadIndexBuffer(ByteBuffer data) {
        int elementArrayBuffer = this.getOrCreateBuffer(ELEMENT_ARRAY_BUFFER);
        glNamedBufferData(elementArrayBuffer, data, GL_STATIC_DRAW);
        glVertexArrayElementBuffer(this.id, elementArrayBuffer);
    }

    @Override
    public void uploadIndexBuffer(MeshData.DrawState drawState) {
        AutoStorageIndexBufferExtension ext = (AutoStorageIndexBufferExtension) (Object) RenderSystem.getSequentialBuffer(drawState.mode());
        ext.veil$bind(this.id, drawState.indexCount());
    }

    @Override
    protected int createBuffer() {
        return glCreateBuffers();
    }

    @Override
    protected void uploadVertexBuffer(int buffer, ByteBuffer data, int usage) {
        glNamedBufferData(buffer, data, usage);
    }
}
