package foundry.veil.impl.client.render.pipeline;

import foundry.veil.Veil;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.framebuffer.FramebufferStack;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

@ApiStatus.Internal
public class AdvancedFboShard extends RenderStateShard.OutputStateShard {

    private final String fboName;

    public AdvancedFboShard(@Nullable ResourceLocation fboName, Supplier<AdvancedFbo> fbo) {
        super(Veil.MODID + ":advanced_fbo", () -> {
            AdvancedFbo value = fbo.get();
            if (value != null) {
                FramebufferStack.push(fboName);
                value.bindDraw(true);
            }
        }, () -> {
            if (fbo.get() != null) {
                FramebufferStack.pop(fboName);
            }
        });
        this.fboName = fboName != null ? fboName.toString() : "custom";
    }

    @Override
    public String toString() {
        return this.name + "[" + this.fboName + "]";
    }
}
