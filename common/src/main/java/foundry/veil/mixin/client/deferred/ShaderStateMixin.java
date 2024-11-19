package foundry.veil.mixin.client.deferred;

import foundry.veil.impl.client.render.deferred.DeferredShaderStateCache;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.Optional;
import java.util.function.Supplier;

@Mixin(RenderStateShard.ShaderStateShard.class)
public class ShaderStateMixin extends RenderStateShard {

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @Shadow
    @Final
    private Optional<Supplier<ShaderInstance>> shader;

    @Unique
    private final DeferredShaderStateCache veil$cache = new DeferredShaderStateCache();

    public ShaderStateMixin(String $$0, Runnable $$1, Runnable $$2) {
        super($$0, $$1, $$2);
    }

    @Override
    public void setupRenderState() {
        if (!this.veil$cache.setupRenderState(this.shader.orElse(() -> null).get())) {
            super.setupRenderState();
        }
    }
}
