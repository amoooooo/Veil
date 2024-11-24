package foundry.veil.ext;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;

public interface ShaderInstanceExtension {

    void veil$recompile(boolean vertex, String source);

    Collection<ResourceLocation> veil$getShaderSources();
}
