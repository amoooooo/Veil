package foundry.veil.impl.client.render.shader;

import foundry.veil.api.client.render.shader.ShaderImporter;
import foundry.veil.api.client.render.shader.ShaderManager;
import foundry.veil.api.client.render.shader.processor.ShaderPreProcessor;
import foundry.veil.api.client.render.shader.processor.ShaderProcessorList;
import foundry.veil.impl.glsl.GlslParser;
import foundry.veil.impl.glsl.grammar.GlslVersionStatement;
import foundry.veil.impl.glsl.node.GlslNode;
import foundry.veil.impl.glsl.node.GlslTree;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectSets;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.Reader;
import java.util.*;

public class ShaderImporterImpl implements ShaderImporter {

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
            return context.modifyInclude(name, this.imports.get(name));
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
