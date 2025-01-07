package foundry.veil.mixin.pipeline.accessor;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MultiBufferSource.BufferSource.class)
public interface PipelineBufferSourceAccessor {

    @Accessor
    @Nullable
    RenderType getLastSharedType();
}
