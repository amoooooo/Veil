package foundry.veil.mixin.accessor;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.ShaderInstance;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(GameRenderer.class)
public interface GameRendererAccessor {

    @Accessor
    @Nullable
    PostChain getPostEffect();

    @Accessor
    Map<String, ShaderInstance> getShaders();

    @Accessor
    ShaderInstance getBlitShader();

    @Accessor
    void setRenderDistance(float renderDistance);
}
