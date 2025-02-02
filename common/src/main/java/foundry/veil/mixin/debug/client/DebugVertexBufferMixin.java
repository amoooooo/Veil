package foundry.veil.mixin.debug.client;

import com.mojang.blaze3d.vertex.VertexBuffer;
import foundry.veil.api.client.render.ext.VeilDebug;
import foundry.veil.ext.DebugVertexBufferExt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import static org.lwjgl.opengl.GL11C.GL_VERTEX_ARRAY;
import static org.lwjgl.opengl.KHRDebug.GL_BUFFER;

@Mixin(VertexBuffer.class)
public class DebugVertexBufferMixin implements DebugVertexBufferExt {

    @Shadow
    private int arrayObjectId;

    @Shadow
    private int vertexBufferId;

    @Shadow
    private int indexBufferId;

    @Override
    public void veil$setName(String name) {
        VeilDebug debug = VeilDebug.get();
        debug.objectLabel(GL_VERTEX_ARRAY, this.arrayObjectId, "Vertex Array " + name);
        debug.objectLabel(GL_BUFFER, this.vertexBufferId, "Vertex Buffer " + name);
        debug.objectLabel(GL_BUFFER, this.indexBufferId, "Element Array Buffer " + name);
    }
}
