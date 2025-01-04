package foundry.veil.mixin.client.pipeline;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.Direction;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(ShaderInstance.class)
public abstract class ShaderInstanceMixin {

    @Unique
    private static final Direction[] veil$DIRECTIONS = Direction.values();

    @Shadow
    @Nullable
    public abstract Uniform getUniform(String name);

    @Inject(method = "setDefaultUniforms", at = @At("TAIL"))
    public void setDefaultUniforms(VertexFormat.Mode mode, Matrix4f projectionMatrix, Matrix4f frustrumMatrix, Window window, CallbackInfo ci) {
        Uniform iModelViewMat = this.getUniform("NormalMat");
        if (iModelViewMat != null) {
            iModelViewMat.set(projectionMatrix.normal(new Matrix3f()));
        }

        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            for (Direction value : veil$DIRECTIONS) {
                Uniform uniform = this.getUniform("VeilBlockFaceBrightness[" + value.get3DDataValue() + "]");
                if (uniform != null) {
                    uniform.set(level.getShade(value, true));
                }
            }
        }
    }
}
