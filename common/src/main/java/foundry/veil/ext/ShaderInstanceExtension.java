package foundry.veil.ext;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

import java.util.Collection;

@ApiStatus.Internal
public interface ShaderInstanceExtension {

    boolean veil$swapBuffers(int activeBuffers);

    void veil$recompile(boolean vertex, String source, int activeBuffers);

    Collection<ResourceLocation> veil$getShaderSources();
}
