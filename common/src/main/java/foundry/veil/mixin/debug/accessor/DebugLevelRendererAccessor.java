package foundry.veil.mixin.debug.accessor;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.PostChain;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelRenderer.class)
public interface DebugLevelRendererAccessor {

    @Accessor
    @Nullable
    PostChain getEntityEffect();

    @Accessor
    @Nullable
    PostChain getTransparencyChain();
}
