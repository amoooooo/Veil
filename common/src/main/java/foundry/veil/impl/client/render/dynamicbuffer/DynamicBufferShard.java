package foundry.veil.impl.client.render.dynamicbuffer;

import com.mojang.blaze3d.pipeline.RenderTarget;
import foundry.veil.Veil;
import foundry.veil.api.client.render.VeilRenderSystem;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Supplier;

@ApiStatus.Internal
public class DynamicBufferShard extends RenderStateShard {

    public DynamicBufferShard(String name, Supplier<RenderTarget> targetSupplier) {
        this(Veil.veilPath("dynamic_" + name), targetSupplier);
    }

    public DynamicBufferShard(ResourceLocation name, Supplier<RenderTarget> targetSupplier) {
        super(Veil.MODID + ":dynamic_buffer", () -> VeilRenderSystem.renderer().getDynamicBufferManger().setupRenderState(name, targetSupplier.get()), () -> VeilRenderSystem.renderer().getDynamicBufferManger().clearRenderState());
    }
}
