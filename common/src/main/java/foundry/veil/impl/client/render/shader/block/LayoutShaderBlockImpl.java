package foundry.veil.impl.client.render.shader.block;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

import java.util.HashSet;
import java.util.Set;

@ApiStatus.Internal
public class LayoutShaderBlockImpl<T> extends SizedShaderBlockImpl<T> {

    private final Set<ResourceLocation> referencedShaders;

    public LayoutShaderBlockImpl(BufferBinding binding, long size, Serializer<T> serializer) {
        super(binding, size, serializer);
        this.referencedShaders = new HashSet<>();
    }

    public Set<ResourceLocation> getReferencedShaders() {
        return this.referencedShaders;
    }
}
