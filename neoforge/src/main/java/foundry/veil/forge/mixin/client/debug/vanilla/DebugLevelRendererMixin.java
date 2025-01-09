package foundry.veil.forge.mixin.client.debug.vanilla;

import com.llamalad7.mixinextras.sugar.Local;
import foundry.veil.api.client.render.rendertype.VeilRenderType;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Supplier;

@Mixin(LevelRenderer.class)
public abstract class DebugLevelRendererMixin {

    @Redirect(method = "renderSectionLayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/util/function/Supplier;)V"))
    private void fixBlockProfilerName(ProfilerFiller instance, Supplier<String> stringSupplier, @Local(argsOnly = true) RenderType renderType) {
        instance.popPush("render_" + VeilRenderType.getName(renderType));
    }
}
