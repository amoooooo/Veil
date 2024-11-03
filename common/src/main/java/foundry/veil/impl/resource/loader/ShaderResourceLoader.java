package foundry.veil.impl.resource.loader;

import foundry.veil.api.client.render.shader.ShaderManager;
import foundry.veil.api.resource.VeilResource;
import foundry.veil.api.resource.VeilResourceInfo;
import foundry.veil.api.resource.VeilResourceLoader;
import foundry.veil.api.resource.VeilResourceManager;
import foundry.veil.api.resource.type.VeilShaderDefinitionResource;
import foundry.veil.api.resource.type.VeilShaderFileResource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

public class ShaderResourceLoader implements VeilResourceLoader {

    private static final Set<String> EXTENSIONS = Set.of(
            ".json",
            ".glsl",
            ".vsh",
            ".tcsh",
            ".tesh",
            ".gsh",
            ".fsh",
            ".comp"
    );

    private final ShaderManager shaderManager;

    public ShaderResourceLoader(ShaderManager shaderManager) {
        this.shaderManager = shaderManager;
    }

    @Override
    public boolean canLoad(PackType packType, ResourceLocation location, Path filePath, @Nullable Path modResourcePath) {
        if (packType != PackType.CLIENT_RESOURCES) {
            return false;
        }

        String path = location.getPath();
        if (path.startsWith(this.shaderManager.getSourceSet().getFolder())) {
            for (String extension : EXTENSIONS) {
                if (path.endsWith(extension)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public VeilResource<?> load(VeilResourceManager resourceManager, ResourceProvider provider, PackType packType, ResourceLocation location, @Nullable Path filePath, @Nullable Path modResourcePath) throws IOException {
        VeilResourceInfo info = new VeilResourceInfo(packType, location, filePath, modResourcePath, false);
        return location.getPath().endsWith(".json") ? new VeilShaderDefinitionResource(info, this.shaderManager) : new VeilShaderFileResource(info, this.shaderManager);
    }
}
