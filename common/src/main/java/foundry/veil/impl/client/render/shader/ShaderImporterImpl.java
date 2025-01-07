package foundry.veil.impl.client.render.shader;

import foundry.veil.Veil;
import foundry.veil.api.client.render.shader.ShaderImporter;
import foundry.veil.api.client.render.shader.ShaderManager;
import foundry.veil.api.client.render.shader.processor.ShaderPreProcessor;
import foundry.veil.api.glsl.node.GlslTree;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectSets;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class ShaderImporterImpl implements ShaderImporter {

    private static final String DEPRECATED_MARKER = "#veil:deprecated";

    private final ResourceProvider resourceProvider;
    private final ObjectSet<ResourceLocation> failedImports;
    private final ObjectSet<ResourceLocation> addedImports;
    private final ObjectSet<ResourceLocation> addedImportsView;
    private final Map<ResourceLocation, String> imports;

    /**
     * Creates a new import processor that loads import files from the specified resource provider.
     *
     * @param resourceProvider The provider for import resources
     */
    public ShaderImporterImpl(ResourceProvider resourceProvider) {
        this.resourceProvider = resourceProvider;
        this.failedImports = new ObjectArraySet<>();
        this.addedImports = new ObjectArraySet<>();
        this.addedImportsView = ObjectSets.unmodifiable(this.addedImports);
        this.imports = new HashMap<>();
    }

    public void reset() {
        this.addedImports.clear();
    }

    @Override
    public GlslTree loadImport(ShaderPreProcessor.Context context, ResourceLocation name, boolean force) throws IOException {
        if (this.failedImports.contains(name)) {
            throw new IOException("Import previously failed to load");
        }

        if (this.addedImports.contains(name) && !force) {
            return new GlslTree();
        }

        try {
            if (!this.imports.containsKey(name)) {
                try (Reader reader = this.resourceProvider.openAsReader(ShaderManager.INCLUDE_LISTER.idToFile(name))) {
                    this.imports.put(name, IOUtils.toString(reader));
                }
            }

            // TODO add a way to safely clone a glsl tree
            GlslTree tree = context.modifyInclude(name, this.imports.get(name));
            Iterator<String> iterator = tree.getDirectives().iterator();
            while (iterator.hasNext()) {
                String directive = iterator.next();
                if (!directive.startsWith(DEPRECATED_MARKER)) {
                    continue;
                }

                iterator.remove();
                String message = directive.substring(DEPRECATED_MARKER.length()).trim();
                if (message.isEmpty()) {
                    Veil.LOGGER.error("Program '{}' uses deprecated import in {} shader '{}'", context.name(), ShaderManager.getTypeName(context.type()), name);
                } else {
                    Veil.LOGGER.error("Program '{}' uses deprecated import in {} shader '{}': {}", context.name(), ShaderManager.getTypeName(context.type()), name, message);
                }
            }
            return tree;
        } catch (Throwable t) {
            this.failedImports.add(name);
            throw new IOException("Failed to add import: " + name, t);
        }
    }

    @Override
    public Set<ResourceLocation> addedImports() {
        return this.addedImportsView;
    }
}
