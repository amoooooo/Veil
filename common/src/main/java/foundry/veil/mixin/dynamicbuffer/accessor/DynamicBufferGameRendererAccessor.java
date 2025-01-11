package foundry.veil.mixin.dynamicbuffer.accessor;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(GameRenderer.class)
public interface DynamicBufferGameRendererAccessor {

    @Accessor
    Map<String, ShaderInstance> getShaders();
}
