package foundry.veil.api.client.render.shader.processor;

import foundry.veil.api.client.render.shader.ShaderManager;
import net.minecraft.ResourceLocationException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.Reader;
import java.util.*;

/**
 * Processes a shader to add imports.
 *
 * @author Ocelot
 */
public class ShaderImportProcessor implements ShaderPreProcessor {

    private static final String INCLUDE_KEY = "#include ";

    private final ResourceProvider resourceProvider;
    private final Set<ResourceLocation> addedImports;
    private final Map<ResourceLocation, String> imports;
    private final List<ResourceLocation> importOrder;
    private int layer;

    /**
     * Creates a new import processor that loads import files from the specified resource provider.
     *
     * @param resourceProvider The provider for import resources
     */
    public ShaderImportProcessor(ResourceProvider resourceProvider) {
        this.resourceProvider = resourceProvider;
        this.addedImports = new HashSet<>();
        this.imports = new HashMap<>();
        this.importOrder = new ArrayList<>();
        this.layer = 0;
    }

    @Override
    public void prepare() {
        this.addedImports.clear();
    }

    @Override
    public String modify(Context context, String source) throws IOException {
        if (!(context instanceof VeilContext veilContext)) {
            return source;
        }

        List<String> inputLines = source.lines().toList();
        List<String> output = new LinkedList<>();

        for (String line : inputLines) {
            if (!line.startsWith(ShaderImportProcessor.INCLUDE_KEY)) {
                output.add(line);
                continue;
            }

            try {
                String trimmedImport = line.substring(ShaderImportProcessor.INCLUDE_KEY.length()).trim();
                ResourceLocation include = ResourceLocation.parse(trimmedImport);
                veilContext.addInclude(include);

                // Only read and process the import if it hasn't been added yet
                if (!this.addedImports.add(include)) {
                    continue;
                }

                try {
                    if (!this.imports.containsKey(include)) {
                        this.imports.put(include, this.loadImport(include));
                        this.importOrder.add(include);
                    }

                    String importString = this.imports.get(include);
                    if (importString == null) {
                        throw new IOException("Import previously failed to load");
                    }

                    long lineNumber = String.join("\n", output).lines().count();
                    int sourceNumber = this.importOrder.indexOf(include) + 1;
//                    output.add("#line 0 " + sourceNumber);
                    this.layer++;
                    output.add(context.modify(include, importString));
                    this.layer--;
//                    output.add("#line " + lineNumber + " " + (this.layer == 0 ? 0 : sourceNumber));
//                    output.add("#line " + lineNumber);
                } catch (Exception e) {
                    throw new IOException("Failed to add import: " + line, e);
                }
            } catch (ResourceLocationException e) {
                throw new IOException("Invalid import: " + line, e);
            }
        }

        return String.join("\n", output);
    }

    private String loadImport(ResourceLocation source) throws IOException {
        Resource resource = this.resourceProvider.getResourceOrThrow(ShaderManager.INCLUDE_LISTER.idToFile(source));
        try (Reader reader = resource.openAsReader()) {
            return IOUtils.toString(reader);
        }
    }
}
