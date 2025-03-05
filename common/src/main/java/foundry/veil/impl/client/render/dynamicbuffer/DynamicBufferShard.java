package foundry.veil.impl.client.render.dynamicbuffer;

import com.mojang.blaze3d.pipeline.RenderTarget;
import foundry.veil.Veil;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.framebuffer.FramebufferStack;
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
        super(Veil.MODID + ":dynamic_buffer", () -> {
            if (!Veil.platform().hasErrors()) {
                DynamicBufferManger dynamicBufferManger = VeilRenderSystem.renderer().getDynamicBufferManger();
                if (dynamicBufferManger.isEnabled()) {
                    FramebufferStack.push(name);
                    dynamicBufferManger.setupRenderState(name, targetSupplier.get(), true);
                }
            }
        }, () -> {
            if (!Veil.platform().hasErrors() && VeilRenderSystem.renderer().getDynamicBufferManger().isEnabled()) {
                FramebufferStack.pop(name);
            }
        });
    }
}
