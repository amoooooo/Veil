package foundry.veil.mixin.debug.client;

import com.mojang.blaze3d.vertex.VertexBuffer;
import foundry.veil.ext.DebugVertexBufferExt;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(LevelRenderer.class)
public class DebugLevelRendererMixin {

    @Shadow
    @Nullable
    private VertexBuffer darkBuffer;

    @Shadow
    @Nullable
    private VertexBuffer skyBuffer;

    @Shadow
    @Nullable
    private VertexBuffer starBuffer;

    @Inject(method = "createDarkSky", at = @At("TAIL"))
    public void nameDarkSky(CallbackInfo ci) {
        ((DebugVertexBufferExt) this.darkBuffer).veil$setName("Void");
    }

    @Inject(method = "createLightSky", at = @At("TAIL"))
    public void nameLightSky(CallbackInfo ci) {
        ((DebugVertexBufferExt) this.skyBuffer).veil$setName("Sky");
    }

    @Inject(method = "createStars", at = @At("TAIL"))
    public void nameStars(CallbackInfo ci) {
        ((DebugVertexBufferExt) this.starBuffer).veil$setName("Star");
    }
}
