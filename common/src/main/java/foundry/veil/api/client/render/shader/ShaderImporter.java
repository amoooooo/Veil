package foundry.veil.api.client.render.shader;

import foundry.veil.api.client.render.shader.processor.ShaderPreProcessor;
import io.github.ocelot.glslprocessor.api.node.GlslTree;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.util.Collection;

/**
 * Loads extra shader files to include inside others.
 *
 * @author Ocelot
 */
public interface ShaderImporter {

    /**
     * Loads the specified import from file <code>assets/modid/pinwheel/shaders/include/path.glsl</code>.
     *
     * @param name  The name of the import to load
     * @param force Whether to load the import nodes even if the import has already been loaded
     * @return An immutable view of the nodes inside the import
     * @throws IOException If there was an error loading the import file
     */
    GlslTree loadImport(ShaderPreProcessor.Context context, ResourceLocation name, boolean force) throws IOException;

    /**
     * @return A view of all imports added to the current file
     */
    Collection<ResourceLocation> addedImports();
}
