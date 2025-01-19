package foundry.veil.mixin.pipeline.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.GlStateManager;
import foundry.veil.api.client.render.VeilRenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import static org.lwjgl.opengl.ARBDirectStateAccess.*;

@Mixin(GlStateManager.class)
public class PipelineGlStateManagerMixin {

    @WrapOperation(method = "_glGenBuffers", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL15;glGenBuffers()I", remap = false), remap = false)
    private static int createBuffers(Operation<Integer> original) {
        return VeilRenderSystem.directStateAccessSupported() ? glCreateBuffers() : original.call();
    }

    @WrapOperation(method = "_glGenVertexArrays", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL30;glGenVertexArrays()I", remap = false), remap = false)
    private static int createVertexArrays(Operation<Integer> original) {
        return VeilRenderSystem.directStateAccessSupported() ? glCreateVertexArrays() : original.call();
    }

    @WrapOperation(method = "glGenFramebuffers", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL30;glGenFramebuffers()I", remap = false), remap = false)
    private static int glGenFramebuffers(Operation<Integer> original) {
        return VeilRenderSystem.directStateAccessSupported() ? glCreateFramebuffers() : original.call();
    }

    @WrapOperation(method = "glGenRenderbuffers", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL30;glGenRenderbuffers()I", remap = false), remap = false)
    private static int glGenRenderbuffers(Operation<Integer> original) {
        return VeilRenderSystem.directStateAccessSupported() ? glCreateRenderbuffers() : original.call();
    }
}
