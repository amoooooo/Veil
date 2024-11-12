package foundry.veil.api.client.render.shader;

import foundry.veil.Veil;
import foundry.veil.impl.client.render.shader.modifier.InputShaderModification;
import foundry.veil.impl.client.render.shader.modifier.ReplaceShaderModification;
import foundry.veil.impl.client.render.shader.modifier.ShaderModification;
import foundry.veil.impl.client.render.shader.modifier.SimpleShaderModification;
import foundry.veil.impl.client.render.shader.transformer.VeilJobParameters;
import foundry.veil.impl.glsl.GlslParser;
import foundry.veil.impl.glsl.GlslSyntaxException;
import foundry.veil.impl.glsl.node.GlslTree;
import foundry.veil.impl.glsl.visitor.GlslStringWriter;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.StringUtil;
import net.minecraft.util.profiling.ProfilerFiller;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Reader;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Manages modifications for both vanilla and Veil shader files.
 *
 * @author Ocelot
 */
public class ShaderModificationManager extends SimplePreparableReloadListener<ShaderModificationManager.Preparations> {

    public static final FileToIdConverter MODIFIER_LISTER = new FileToIdConverter("pinwheel/shader_modifiers", ".txt");
    private static final Map<String, String> NEXT_STAGES = Map.of(
            "vsh", "gsh",
            "gsh", "fsh"
    );
    private static final Pattern OUT_PATTERN = Pattern.compile("out ");

    private Map<ResourceLocation, List<ShaderModification>> shaders;
    private Map<ShaderModification, ResourceLocation> names;

    public ShaderModificationManager() {
        this.shaders = Collections.emptyMap();
    }

    /**
     * Applies all shader modifiers to the specified shader source.
     *
     * @param shaderId The id of the shader to get modifiers for
     * @param source   The shader source text
     * @param flags    Additional flags for applying
     * @return The modified shader source
     * @see ShaderModification
     */
    public String applyModifiers(ResourceLocation shaderId, String source, int flags) {
        Collection<ShaderModification> modifiers = this.getModifiers(shaderId);
        if (modifiers.isEmpty()) {
            return source;
        }

        try {
            GlslTree tree = GlslParser.parse(source);
            VeilJobParameters parameters = new VeilJobParameters(this, shaderId, flags);
            for (ShaderModification modifier : modifiers) {
                modifier.inject(tree, parameters);
            }

            GlslStringWriter writer = new GlslStringWriter();
            tree.visit(writer);
            return writer.toString();
        } catch (Exception e) {
            Veil.LOGGER.error("Failed to transform shader: {}", shaderId, e);
        }
        return source;
    }

    /**
     * Retrieves all modifiers for the specified shader.
     *
     * @param shaderId The shader to get all modifiers for
     * @return The modifiers applied to the specified shader
     */
    public Collection<ShaderModification> getModifiers(ResourceLocation shaderId) {
        return this.shaders.getOrDefault(shaderId, Collections.emptyList());
    }

    /**
     * Retrieves the id of the specified modifier.
     *
     * @param modification The modification to get the id of
     * @return The id of that modification or <code>null</code> if unregistered
     */
    public @Nullable ResourceLocation getModifierId(ShaderModification modification) {
        return this.names.get(modification);
    }

    private @Nullable ResourceLocation getNextStage(ResourceLocation shader, ResourceProvider resourceProvider) {
        String[] parts = shader.getPath().split("\\.");
        String extension = parts[parts.length - 1].toLowerCase(Locale.ROOT);

        while (extension != null) {
            extension = NEXT_STAGES.get(extension);

            ResourceLocation nextShader = new ResourceLocation(shader.getNamespace(), shader.getPath().substring(0, shader.getPath().length() - 3) + extension);
            if (resourceProvider.getResource(nextShader).isPresent()) {
                return nextShader;
            }
        }
        return null;
    }

    @Override
    protected @NotNull Preparations prepare(@NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        Map<ResourceLocation, List<ShaderModification>> modifiers = new HashMap<>();
        Map<ShaderModification, ResourceLocation> names = new HashMap<>();

        for (Map.Entry<ResourceLocation, Resource> entry : MODIFIER_LISTER.listMatchingResources(resourceManager).entrySet()) {
            ResourceLocation file = entry.getKey();
            ResourceLocation id = MODIFIER_LISTER.fileToId(file);

            try {
                String[] parts = id.getPath().split("/", 2);
                if (parts.length < 2) {
                    Veil.LOGGER.warn("Ignoring shader modifier {}. Expected format to be located in shader_modifiers/domain/shader_path.vsh.txt", file);
                    continue;
                }

                ResourceLocation shaderId = new ResourceLocation(parts[0], parts[1]);
                try (Reader reader = entry.getValue().openAsReader()) {
                    ShaderModification modification = ShaderModification.parse(IOUtils.toString(reader), shaderId.getPath().endsWith(".vsh"));
                    List<ShaderModification> modifications = modifiers.computeIfAbsent(shaderId, name -> new LinkedList<>());

                    if (modification instanceof ReplaceShaderModification) {
                        // TODO This doesn't respect priority
                        modifications.clear();
                    }
                    if (modifications.size() != 1 || !(modifications.get(0) instanceof ReplaceShaderModification)) {
                        modifications.add(modification);
                    }
                    names.put(modification, id);
                }
            } catch (Exception e) {
                Veil.LOGGER.error("Couldn't parse data file {} from {}", id, file, e);
            }
        }

        // Inject inputs to next shader stage
        for (Map.Entry<ResourceLocation, List<ShaderModification>> entry : new HashMap<>(modifiers).entrySet()) {
            ResourceLocation nextStage = null;

            for (ShaderModification modification : entry.getValue()) {
                if (!(modification instanceof SimpleShaderModification simpleMod)) {
                    continue;
                }

                String output = simpleMod.getOutput();
                if (StringUtil.isNullOrEmpty(output)) {
                    continue;
                }

                if (nextStage == null) {
                    nextStage = this.getNextStage(entry.getKey(), resourceManager);
                }
                if (nextStage == null) {
                    // No need to inject in into next shader
                    break;
                }

                InputShaderModification input = new InputShaderModification(simpleMod.priority(), OUT_PATTERN.matcher(simpleMod.fillPlaceholders(simpleMod.getOutput())).replaceAll("in "));
                modifiers.computeIfAbsent(nextStage, unused -> new LinkedList<>()).add(input);
                names.put(input, names.get(simpleMod));
            }
        }
        modifiers.values().forEach(modifications -> modifications.sort(Comparator.comparingInt(ShaderModification::priority).thenComparing(names::get)));

        return new Preparations(modifiers, names);
    }

    @Override
    protected void apply(@NotNull Preparations preparations, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        this.shaders = Collections.unmodifiableMap(preparations.shaders);
        this.names = Collections.unmodifiableMap(preparations.names);
        Veil.LOGGER.info("Loaded {} shader modifications", this.names.size());
    }

    @ApiStatus.Internal
    public record Preparations(Map<ResourceLocation, List<ShaderModification>> shaders,
                               Map<ShaderModification, ResourceLocation> names) {
    }
}
