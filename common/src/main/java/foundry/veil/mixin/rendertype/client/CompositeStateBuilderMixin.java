package foundry.veil.mixin.rendertype.client;

import foundry.veil.api.client.render.rendertype.VeilRenderTypeBuilder;
import foundry.veil.ext.CompositeStateExtension;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.LinkedList;
import java.util.List;

@Mixin(RenderType.CompositeState.CompositeStateBuilder.class)
public abstract class CompositeStateBuilderMixin implements VeilRenderTypeBuilder {

    @Shadow
    public abstract RenderType.CompositeState.CompositeStateBuilder setTextureState(RenderStateShard.EmptyTextureStateShard emptyTextureStateShard);

    @Shadow
    public abstract RenderType.CompositeState.CompositeStateBuilder setShaderState(RenderStateShard.ShaderStateShard shaderStateShard);

    @Shadow
    public abstract RenderType.CompositeState.CompositeStateBuilder setTransparencyState(RenderStateShard.TransparencyStateShard transparencyStateShard);

    @Shadow
    public abstract RenderType.CompositeState.CompositeStateBuilder setDepthTestState(RenderStateShard.DepthTestStateShard depthTestStateShard);

    @Shadow
    public abstract RenderType.CompositeState.CompositeStateBuilder setLightmapState(RenderStateShard.LightmapStateShard lightmapStateShard);

    @Shadow
    public abstract RenderType.CompositeState.CompositeStateBuilder setCullState(RenderStateShard.CullStateShard cullStateShard);

    @Shadow
    public abstract RenderType.CompositeState.CompositeStateBuilder setOverlayState(RenderStateShard.OverlayStateShard overlayStateShard);

    @Shadow
    public abstract RenderType.CompositeState.CompositeStateBuilder setLayeringState(RenderStateShard.LayeringStateShard layeringStateShard);

    @Shadow
    public abstract RenderType.CompositeState.CompositeStateBuilder setOutputState(RenderStateShard.OutputStateShard outputStateShard);

    @Shadow
    public abstract RenderType.CompositeState.CompositeStateBuilder setTexturingState(RenderStateShard.TexturingStateShard texturingStateShard);

    @Shadow
    public abstract RenderType.CompositeState.CompositeStateBuilder setWriteMaskState(RenderStateShard.WriteMaskStateShard writeMaskStateShard);

    @Shadow
    public abstract RenderType.CompositeState.CompositeStateBuilder setLineState(RenderStateShard.LineStateShard lineStateShard);

    @Shadow
    public abstract RenderType.CompositeState.CompositeStateBuilder setColorLogicState(RenderStateShard.ColorLogicStateShard colorLogicStateShard);

    @Shadow
    public abstract RenderType.CompositeState createCompositeState(RenderType.OutlineProperty outlineProperty);

    @Unique
    private List<RenderStateShard> veil$layers;

    @Override
    public VeilRenderTypeBuilder textureState(RenderStateShard.EmptyTextureStateShard state) {
        this.setTextureState(state);
        return this;
    }

    @Override
    public VeilRenderTypeBuilder shaderState(RenderStateShard.ShaderStateShard state) {
        this.setShaderState(state);
        return this;
    }

    @Override
    public VeilRenderTypeBuilder transparencyState(RenderStateShard.TransparencyStateShard state) {
        this.setTransparencyState(state);
        return this;
    }

    @Override
    public VeilRenderTypeBuilder depthTestState(RenderStateShard.DepthTestStateShard state) {
        this.setDepthTestState(state);
        return this;
    }

    @Override
    public VeilRenderTypeBuilder cullState(RenderStateShard.CullStateShard state) {
        this.setCullState(state);
        return this;
    }

    @Override
    public VeilRenderTypeBuilder lightmapState(RenderStateShard.LightmapStateShard state) {
        this.setLightmapState(state);
        return this;
    }

    @Override
    public VeilRenderTypeBuilder overlayState(RenderStateShard.OverlayStateShard state) {
        this.setOverlayState(state);
        return this;
    }

    @Override
    public VeilRenderTypeBuilder layeringState(RenderStateShard.LayeringStateShard state) {
        this.setLayeringState(state);
        return this;
    }

    @Override
    public VeilRenderTypeBuilder outputState(RenderStateShard.OutputStateShard state) {
        this.setOutputState(state);
        return this;
    }

    @Override
    public VeilRenderTypeBuilder texturingState(RenderStateShard.TexturingStateShard state) {
        this.setTexturingState(state);
        return this;
    }

    @Override
    public VeilRenderTypeBuilder writeMaskState(RenderStateShard.WriteMaskStateShard state) {
        this.setWriteMaskState(state);
        return this;
    }

    @Override
    public VeilRenderTypeBuilder lineState(RenderStateShard.LineStateShard state) {
        this.setLineState(state);
        return this;
    }

    @Override
    public VeilRenderTypeBuilder colorLogicState(RenderStateShard.ColorLogicStateShard state) {
        this.setColorLogicState(state);
        return this;
    }

    @Override
    public VeilRenderTypeBuilder addLayer(RenderStateShard shard) {
        if (this.veil$layers == null) {
            this.veil$layers = new LinkedList<>();
        }
        this.veil$layers.add(shard);
        return this;
    }

    @Override
    public RenderType.CompositeState create(RenderType.OutlineProperty outlineProperty) {
        return this.createCompositeState(outlineProperty);
    }

    @Inject(method = "createCompositeState(Lnet/minecraft/client/renderer/RenderType$OutlineProperty;)Lnet/minecraft/client/renderer/RenderType$CompositeState;", at = @At("RETURN"))
    public void addLayers(CallbackInfoReturnable<RenderType.CompositeState> cir) {
        if (this.veil$layers != null) {
            ((CompositeStateExtension) (Object) cir.getReturnValue()).veil$addShards(this.veil$layers);
        }
    }
}
