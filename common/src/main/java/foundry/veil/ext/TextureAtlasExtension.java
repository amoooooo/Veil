package foundry.veil.ext;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface TextureAtlasExtension {

    boolean veil$hasTexture(ResourceLocation location);
}
