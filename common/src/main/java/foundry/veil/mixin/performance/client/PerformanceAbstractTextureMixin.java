package foundry.veil.mixin.performance.client;

import com.llamalad7.mixinextras.sugar.Local;
import foundry.veil.api.client.render.VeilRenderSystem;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.lwjgl.opengl.ARBDirectStateAccess.glTextureParameteri;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_MIN_FILTER;

@Mixin(AbstractTexture.class)
public abstract class PerformanceAbstractTextureMixin {

    @Shadow
    public abstract int getId();

    @Unique
    private boolean veil$initialized;

    @Inject(method = "setFilter", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/AbstractTexture;bind()V"), cancellable = true)
    public void setFilterDSA(boolean blur, boolean mipmap, CallbackInfo ci, @Local(ordinal = 0) int minFilter, @Local(ordinal = 1) int magFilter) {
        if (!this.veil$initialized || !VeilRenderSystem.directStateAccessSupported()) {
            return;
        }

        ci.cancel();

        int id = this.getId();
        glTextureParameteri(id, GL_TEXTURE_MIN_FILTER, minFilter);
        glTextureParameteri(id, GL_TEXTURE_MAG_FILTER, magFilter);
    }

    @Inject(method = "bind", at = @At("TAIL"))
    public void bind(CallbackInfo ci) {
        // Make sure the texture has been bound, so the storage exists for it
        this.veil$initialized = true;
    }
}
