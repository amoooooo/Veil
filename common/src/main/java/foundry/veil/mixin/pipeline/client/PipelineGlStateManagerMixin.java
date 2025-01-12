package foundry.veil.mixin.pipeline.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.GlStateManager;
import foundry.veil.api.client.render.VeilRenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import static org.lwjgl.opengl.ARBDirectStateAccess.glCreateBuffers;
import static org.lwjgl.opengl.ARBDirectStateAccess.glCreateVertexArrays;

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
}
