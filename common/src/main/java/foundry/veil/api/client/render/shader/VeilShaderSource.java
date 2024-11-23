package foundry.veil.api.client.render.shader;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Set;

/**
 * A shader instance that has additional pre-compiled data.
 *
 * @param sourceId               The id of the source file this shader was compiled from
 * @param sourceCode             The shader source ready to be uploaded to the GPU
 * @param uniformBindings        The bindings set by the shader
 * @param definitionDependencies The shader pre-definitions this shader is dependent on
 * @param includes               All shader imports included in this file
 * @author Ocelot
 */
public record VeilShaderSource(ResourceLocation sourceId,
                               String sourceCode,
                               Object2IntMap<String> uniformBindings,
                               Set<String> definitionDependencies,
                               Set<ResourceLocation> includes) {

    public VeilShaderSource(ResourceLocation sourceId, String sourceCode) {
        this(sourceId, sourceCode, Object2IntMaps.emptyMap(), Collections.emptySet(), Collections.emptySet());
    }
}