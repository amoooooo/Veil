package foundry.veil.forge.mixin.compat.sodium;

import foundry.veil.forge.sodium.VeilNormalUniform;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.ChunkShaderOptions;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.DefaultShaderInterface;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.ShaderBindingContext;
import org.joml.Matrix3f;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DefaultShaderInterface.class)
public class DefaultShaderInterfaceMixin {

    @Unique
    private VeilNormalUniform veil$uniformNormalMatrix;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void init(ShaderBindingContext context, ChunkShaderOptions options, CallbackInfo ci) {
        this.veil$uniformNormalMatrix = context.bindUniformOptional("VeilNormalMatrix", VeilNormalUniform::new);
    }

    @Inject(method = "setModelViewMatrix", at = @At("TAIL"), remap = false)
    public void setModelViewMatrix(Matrix4fc matrix, CallbackInfo ci) {
        if (this.veil$uniformNormalMatrix != null) {
            this.veil$uniformNormalMatrix.set(matrix.normal(new Matrix3f()));
        }
    }
}
