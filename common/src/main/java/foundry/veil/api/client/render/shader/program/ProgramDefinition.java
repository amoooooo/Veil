package foundry.veil.api.client.render.shader.program;

import com.google.common.collect.Iterables;
import com.google.gson.*;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import foundry.veil.api.client.render.shader.definition.ShaderPreDefinitions;
import foundry.veil.api.client.render.shader.texture.ShaderTextureSource;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.*;

import static org.lwjgl.opengl.GL20C.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20C.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL32C.GL_GEOMETRY_SHADER;
import static org.lwjgl.opengl.GL40C.GL_TESS_CONTROL_SHADER;
import static org.lwjgl.opengl.GL40C.GL_TESS_EVALUATION_SHADER;
import static org.lwjgl.opengl.GL43C.GL_COMPUTE_SHADER;

/**
 * Defines a shader program instance.
 *
 * @param vertex                The vertex shader or <code>null</code> to not include one
 * @param tesselationControl    The tesselation control shader or <code>null</code> to not include one
 * @param tesselationEvaluation The tesselation evluation shader or <code>null</code> to not include one
 * @param geometry              The geometry shader or <code>null</code> to not include one
 * @param fragment              The fragment shader or <code>null</code> to not include one
 * @param compute               The compute shader or <code>null</code> to not include one.
 *                              Compute should be in a shader by itself
 * @param definitions           The definitions to inject when compiling
 * @param definitionDefaults    The default values for definitions
 * @param textures              The textures to bind when using this shader
 * @param shaders               A map of all sources and their OpenGL types for convenience
 * @param blendMode             The blend mode to use or <code>null</code> to use the current blend mode
 * @author Ocelot
 */
public record ProgramDefinition(@Nullable ResourceLocation vertex,
                                @Nullable ResourceLocation tesselationControl,
                                @Nullable ResourceLocation tesselationEvaluation,
                                @Nullable ResourceLocation geometry,
                                @Nullable ResourceLocation fragment,
                                @Nullable ResourceLocation compute,
                                String[] definitions,
                                Map<String, String> definitionDefaults,
                                Map<String, ShaderTextureSource> textures,
                                Int2ObjectMap<ResourceLocation> shaders,
                                @Nullable ShaderBlendMode blendMode) {

    public Map<String, String> getMacros(Set<String> dependencies, ShaderPreDefinitions definitions) {
        Map<String, String> macros = new HashMap<>(definitions.getStaticDefinitions());
        for (String name : this.definitions) {
            String definition = definitions.getDefinition(name);

            if (definition != null) {
                macros.put(name.toUpperCase(Locale.ROOT), definition);
            } else {
                String definitionDefault = this.definitionDefaults.get(name);
                if (definitionDefault != null) {
                    macros.put(name.toUpperCase(Locale.ROOT), definitionDefault);
                }
            }

            dependencies.add(name);
        }
        return macros;
    }

    /**
     * Deserializer for {@link ProgramDefinition}.
     */
    public static class Deserializer implements JsonDeserializer<ProgramDefinition> {

        private static @Nullable ResourceLocation deserializeSource(JsonObject json, String name, JsonDeserializationContext context) {
            JsonElement element = json.get(name);
            if (element == null) {
                return null;
            }

            return context.deserialize(element, ResourceLocation.class);
        }

        private String[] deserializeDefinitions(JsonArray json, Map<String, String> defaults) throws JsonParseException {
            List<String> definitions = new ArrayList<>(json.size());
            for (int i = 0; i < json.size(); i++) {
                JsonElement element = json.get(i);
                if (element.isJsonPrimitive()) {
                    definitions.add(element.getAsJsonPrimitive().getAsString());
                } else if (element.isJsonObject()) {
                    JsonObject definitionJson = element.getAsJsonObject();
                    Set<Map.Entry<String, JsonElement>> entrySet = definitionJson.entrySet();
                    if (entrySet.size() != 1) {
                        throw new JsonSyntaxException("Expected definitions[" + i + "] " + "to have one element, had " + entrySet.size());
                    }

                    Map.Entry<String, JsonElement> entry = Iterables.getOnlyElement(entrySet);
                    definitions.add(entry.getKey());
                    defaults.put(entry.getKey(), GsonHelper.convertToString(entry.getValue(), "definitions[" + i + "]"));
                } else {
                    throw new JsonSyntaxException("Expected definitions[" + i + "]" + " to be a JsonPrimitive or Object, was " + GsonHelper.getType(element));
                }
            }

            return definitions.toArray(String[]::new);
        }

        private Map<String, ShaderTextureSource> deserializeTextures(JsonObject json) throws JsonParseException {
            Map<String, ShaderTextureSource> textures = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                String name = entry.getKey();
                DataResult<ShaderTextureSource> texture = ShaderTextureSource.CODEC.parse(JsonOps.INSTANCE, entry.getValue());
                if (texture.error().isPresent()) {
                    throw new JsonSyntaxException("Failed to deserialize texture: " + name + ". " + texture.error().get().message());
                }

                textures.put(name, texture.result().orElseThrow());
            }
            ShaderTextureSource.CODEC.parse(JsonOps.INSTANCE, json);
            return Collections.unmodifiableMap(textures);
        }

        @Override
        public ProgramDefinition deserialize(JsonElement element, Type type, JsonDeserializationContext context) throws JsonParseException {
            JsonObject json = element.getAsJsonObject();
            ResourceLocation vertex = deserializeSource(json, "vertex", context);
            ResourceLocation tesselationControl = deserializeSource(json, "tesselation_control", context);
            ResourceLocation tesselationEvaluation = deserializeSource(json, "tesselation_evaluation", context);
            ResourceLocation geometry = deserializeSource(json, "geometry", context);
            ResourceLocation fragment = deserializeSource(json, "fragment", context);
            ResourceLocation compute = deserializeSource(json, "compute", context);

            String[] definitions;
            Map<String, String> definitionDefaults;
            if (json.has("definitions")) {
                Map<String, String> defaults = new HashMap<>();
                definitions = this.deserializeDefinitions(json.getAsJsonArray("definitions"), defaults);
                definitionDefaults = Collections.unmodifiableMap(defaults);
            } else {
                definitions = new String[0];
                definitionDefaults = Collections.emptyMap();
            }

            Map<String, ShaderTextureSource> textures = json.has("textures") ? this.deserializeTextures(json.getAsJsonObject("textures")) : Collections.emptyMap();
            ShaderBlendMode blendMode;
            if (json.has("blend")) {
                DataResult<ShaderBlendMode> result = ShaderBlendMode.CODEC.parse(JsonOps.INSTANCE, json.get("blend"));
                if (result.isError()) {
                    throw new JsonSyntaxException(result.error().orElseThrow().message());
                }
                blendMode = result.result().orElseThrow();
            } else {
                blendMode = null;
            }

            Int2ObjectMap<ResourceLocation> sources = new Int2ObjectArrayMap<>();
            if (vertex != null) {
                sources.put(GL_VERTEX_SHADER, vertex);
            }
            if (tesselationControl != null) {
                sources.put(GL_TESS_CONTROL_SHADER, tesselationControl);
            }
            if (tesselationEvaluation != null) {
                sources.put(GL_TESS_EVALUATION_SHADER, tesselationEvaluation);
            }
            if (geometry != null) {
                sources.put(GL_GEOMETRY_SHADER, geometry);
            }
            if (fragment != null) {
                sources.put(GL_FRAGMENT_SHADER, fragment);
            }
            if (compute != null) {
                sources.put(GL_COMPUTE_SHADER, compute);
            }

            return new ProgramDefinition(vertex,
                    tesselationControl,
                    tesselationEvaluation,
                    geometry,
                    fragment,
                    compute,
                    definitions,
                    definitionDefaults,
                    textures,
                    Int2ObjectMaps.unmodifiable(sources),
                    blendMode);
        }
    }
}
