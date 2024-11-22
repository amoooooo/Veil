package foundry.veil.impl.client.render.pipeline;

import foundry.veil.Veil;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

@ApiStatus.Internal
public class AdvancedFboShard extends RenderStateShard.OutputStateShard {

    private final ResourceLocation fboName;

    public AdvancedFboShard(@Nullable ResourceLocation fboName, Supplier<AdvancedFbo> fbo) {
        super(Veil.MODID + ":advanced_fbo", () -> {
            AdvancedFbo value = fbo.get();
            if (value != null) {
                value.bindDraw(true);
            }
        }, AdvancedFbo::unbind);
        this.fboName = fboName;
    }

    @Override
    public String toString() {
        return this.name + "[" + (this.fboName != null ? this.fboName : "custom") + "]";
    }
}
