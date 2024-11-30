package foundry.veil.mixin.client.pipeline;

import foundry.veil.ext.TextureAtlasExtension;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

@Mixin(TextureAtlas.class)
public class TextureAtlasMixin implements TextureAtlasExtension {

    @Shadow
    private Map<ResourceLocation, TextureAtlasSprite> texturesByName;

    @Override
    public boolean veil$hasTexture(ResourceLocation location) {
        return this.texturesByName.containsKey(location);
    }
}
