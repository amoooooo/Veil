package foundry.veil.impl.client.render.mesh;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import foundry.veil.api.client.render.mesh.VertexArray;
import foundry.veil.api.client.render.mesh.VertexArrayBuilder;
import foundry.veil.ext.AutoStorageIndexBufferExtension;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.ARBDirectStateAccess.*;

@ApiStatus.Internal
public class DSAVertexAttribBindingVertexArray extends VertexArray {

    public DSAVertexAttribBindingVertexArray(int id) {
        super(id);
    }

    @Override
    public void uploadIndexBuffer(MeshData.DrawState drawState, @Nullable ByteBuffer data, int usage) {
        if (data != null) {
            int elementArrayBuffer = this.getOrCreateBuffer(ELEMENT_ARRAY_BUFFER);
            glNamedBufferData(elementArrayBuffer, data, usage);
            glVertexArrayElementBuffer(this.id, elementArrayBuffer);
        } else {
            AutoStorageIndexBufferExtension ext = (AutoStorageIndexBufferExtension) (Object) RenderSystem.getSequentialBuffer(drawState.mode());
            ext.veil$bind(this.id, drawState.indexCount());
        }
    }

    @Override
    protected int createBuffer() {
        return glCreateBuffers();
    }

    @Override
    protected void uploadVertexBuffer(int buffer, ByteBuffer data, int usage) {
        glNamedBufferData(buffer, data, usage);
    }

    @Override
    public VertexArrayBuilder editFormat() {
        this.bind();
        return new DSAVertexAttribBindingBuilder(this, this.id);
    }
}
