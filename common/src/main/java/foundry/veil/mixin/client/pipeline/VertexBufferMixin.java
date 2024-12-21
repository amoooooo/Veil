package foundry.veil.mixin.client.pipeline;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import foundry.veil.ext.VertexBufferExtension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.lwjgl.opengl.GL11C.glDrawArrays;
import static org.lwjgl.opengl.GL11C.glGetInteger;
import static org.lwjgl.opengl.GL15C.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15C.glBindBuffer;
import static org.lwjgl.opengl.GL20C.GL_CURRENT_PROGRAM;
import static org.lwjgl.opengl.GL31C.glDrawArraysInstanced;
import static org.lwjgl.opengl.GL31C.glDrawElementsInstanced;
import static org.lwjgl.opengl.GL40C.GL_PATCHES;
import static org.lwjgl.opengl.GL43C.glMultiDrawElementsIndirect;

@Mixin(VertexBuffer.class)
public abstract class VertexBufferMixin implements VertexBufferExtension {

    @Shadow
    private VertexFormat.Mode mode;

    @Shadow
    private int indexBufferId;

    @Shadow
    private int indexCount;

    @Shadow
    protected abstract VertexFormat.IndexType getIndexType();

    @Shadow
    @Nullable
    private RenderSystem.AutoStorageIndexBuffer sequentialIndices;

    @Shadow
    private VertexFormat.IndexType indexType;

    @Override
    public void veil$drawInstanced(int instances) {
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(() -> this._veil$drawInstanced(instances));
        } else {
            this._veil$drawInstanced(instances);
        }
    }

    @Override
    public void veil$drawIndirect(long indirect, int drawCount, int stride) {
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(() -> this._veil$drawIndirect(indirect, drawCount, stride));
        } else {
            this._veil$drawIndirect(indirect, drawCount, stride);
        }
    }

    @Override
    public int veil$getIndexCount() {
        return this.indexCount;
    }

    @Inject(method = "_drawWithShader", at = @At("HEAD"))
    public void _drawWithShader(Matrix4f modelView, Matrix4f projection, ShaderInstance shader, CallbackInfo ci) {
        Uniform iModelViewMat = shader.getUniform("NormalMat");
        if (iModelViewMat != null) {
            iModelViewMat.set(modelView.normal(new Matrix3f()));
        }

        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            for (Direction value : Direction.values()) {
                Uniform uniform = shader.getUniform("VeilBlockFaceBrightness[" + value.get3DDataValue() + "]");
                if (uniform != null) {
                    uniform.set(level.getShade(value, true));
                }
            }
        }
    }

    @Inject(method = "draw", at = @At("HEAD"), cancellable = true)
    public void drawPatches(CallbackInfo ci) {
        if (this.mode != VertexFormat.Mode.QUADS) {
            return;
        }

        ShaderProgram shader = VeilRenderSystem.getShader();
        if (shader != null && shader.hasTesselation() && shader.getProgram() == glGetInteger(GL_CURRENT_PROGRAM)) {
            // Quads are internally switched to triangles with indices in vanilla mc, so just use draw arrays
            // This will be wrong if custom indices are used! (transparent objects)
            glDrawArrays(GL_PATCHES, 0, this.indexCount * 4 / 6);
            ci.cancel();
        }
    }

    @ModifyArg(method = "draw", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;drawElements(III)V"), index = 0)
    public int modifyDrawMode(int glMode) {
        return this.veil$getDrawMode(glMode);
    }

    @Unique
    private int veil$getDrawMode(int defaultMode) {
        ShaderProgram shader = VeilRenderSystem.getShader();
        if (shader != null && shader.hasTesselation() && shader.getProgram() == glGetInteger(GL_CURRENT_PROGRAM)) {
            return GL_PATCHES;
        }
        return defaultMode;
    }

    @Unique
    private void _veil$drawInstanced(int instances) {
        if (this.mode == VertexFormat.Mode.QUADS) {
            ShaderProgram shader = VeilRenderSystem.getShader();
            if (shader != null && shader.hasTesselation() && shader.getProgram() == glGetInteger(GL_CURRENT_PROGRAM)) {
                // Quads are internally switched to triangles with indices in vanilla mc, so just use draw arrays
                // This will be wrong if custom indices are used! (transparent objects)
                glDrawArraysInstanced(GL_PATCHES, 0, this.indexCount * 4 / 6, instances);
                return;
            }
        }

        glDrawElementsInstanced(this.veil$getDrawMode(this.mode.asGLMode), this.indexCount, this.getIndexType().asGLType, 0L, instances);
    }

    @Unique
    private void _veil$drawIndirect(long indirect, int drawCount, int stride) {
        if (this.sequentialIndices != null) {
            this.sequentialIndices.bind(this.indexCount);
            glMultiDrawElementsIndirect(this.veil$getDrawMode(this.mode.asGLMode), this.sequentialIndices.type().asGLType, indirect, drawCount, stride);
        } else {
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, this.indexBufferId);
            glMultiDrawElementsIndirect(this.veil$getDrawMode(this.mode.asGLMode), this.indexType.asGLType, indirect, drawCount, stride);
        }
    }
}
