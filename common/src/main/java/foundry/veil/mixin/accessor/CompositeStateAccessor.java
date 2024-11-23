package foundry.veil.mixin.accessor;

import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderType.CompositeState.class)
public interface CompositeStateAccessor {

    @Accessor
    RenderStateShard.EmptyTextureStateShard getTextureState();

    @Accessor
    RenderStateShard.ShaderStateShard getShaderState();

    @Accessor
    RenderStateShard.TransparencyStateShard getTransparencyState();

    @Accessor
    RenderStateShard.DepthTestStateShard getDepthTestState();

    @Accessor
    RenderStateShard.CullStateShard getCullState();

    @Accessor
    RenderStateShard.LightmapStateShard getLightmapState();

    @Accessor
    RenderStateShard.OverlayStateShard getOverlayState();

    @Accessor
    RenderStateShard.LayeringStateShard getLayeringState();

    @Accessor
    RenderStateShard.OutputStateShard getOutputState();

    @Accessor
    RenderStateShard.TexturingStateShard getTexturingState();

    @Accessor
    RenderStateShard.WriteMaskStateShard getWriteMaskState();

    @Accessor
    RenderStateShard.LineStateShard getLineState();

    @Accessor
    RenderStateShard.ColorLogicStateShard getColorLogicState();

    @Accessor
    RenderType.OutlineProperty getOutlineProperty();
}
