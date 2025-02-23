package foundry.veil.mixin.resource.accessor;

import net.minecraft.client.renderer.texture.TextureAtlas;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TextureAtlas.class)
public interface ResourceTextureAtlasAccessor {

    @Accessor
    int getMipLevel();
}
