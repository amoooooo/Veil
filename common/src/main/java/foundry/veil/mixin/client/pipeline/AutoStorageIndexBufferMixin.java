package foundry.veil.mixin.client.pipeline;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import foundry.veil.ext.AutoStorageIndexBufferExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import static org.lwjgl.opengl.ARBDirectStateAccess.glVertexArrayElementBuffer;

@Mixin(RenderSystem.AutoStorageIndexBuffer.class)
public abstract class AutoStorageIndexBufferMixin implements AutoStorageIndexBufferExtension {

    @Shadow
    private int name;

    @Shadow
    protected abstract void ensureStorage(int neededIndexCount);

    @Shadow
    public abstract boolean hasStorage(int index);

    @Override
    public void veil$bind(int vao, int indexCount) {
        if (this.name == 0) {
            this.name = GlStateManager._glGenBuffers();
        }

        if (!this.hasStorage(indexCount)) {
            this.ensureStorage(indexCount);
        }
        glVertexArrayElementBuffer(vao, this.name);
    }
}
