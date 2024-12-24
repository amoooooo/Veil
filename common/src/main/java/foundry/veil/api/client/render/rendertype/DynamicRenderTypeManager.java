package foundry.veil.api.client.render.rendertype;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import foundry.veil.Veil;
import foundry.veil.api.client.render.rendertype.layer.CompositeRenderTypeData;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class DynamicRenderTypeManager extends SimplePreparableReloadListener<Map<ResourceLocation, byte[]>> {

    private static final FileToIdConverter CONVERTER = FileToIdConverter.json("pinwheel/rendertypes");

    private final Map<ResourceLocation, RenderTypeCache> renderTypes = new Object2ObjectArrayMap<>();

    /**
     * Retrieves and caches a rendertype with the specified id.
     *
     * @param id     The id of the rendertype to get
     * @param params Additional parameters to configure the render type
     * @return The rendertype created or <code>null</code> if unregistered or an error occurs
     */
    public @Nullable RenderType get(ResourceLocation id, Object... params) {
        RenderTypeCache cache = this.renderTypes.get(id);
        if (cache == null) {
            return null;
        }

        return cache.get(params);
    }

    @Override
    protected @NotNull Map<ResourceLocation, byte[]> prepare(@NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        Map<ResourceLocation, byte[]> data = new HashMap<>();

        Map<ResourceLocation, Resource> resources = CONVERTER.listMatchingResources(resourceManager);
        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            ResourceLocation location = entry.getKey();
            ResourceLocation id = CONVERTER.fileToId(location);

            try (InputStream stream = entry.getValue().open()) {
                data.put(id, stream.readAllBytes());
            } catch (Exception e) {
                Veil.LOGGER.error("Couldn't read data file {} from {}", id, location, e);
            }
        }

        return data;
    }

    @Override
    protected void apply(Map<ResourceLocation, byte[]> fileData, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        Map<ResourceLocation, RenderTypeCache> renderTypes = new HashMap<>();

        for (Map.Entry<ResourceLocation, byte[]> entry : fileData.entrySet()) {
            ResourceLocation id = entry.getKey();

            try (Reader reader = new InputStreamReader(new ByteArrayInputStream(entry.getValue()))) {
                JsonElement element = JsonParser.parseReader(reader);
                DataResult<CompositeRenderTypeData> result = CompositeRenderTypeData.CODEC.parse(JsonOps.INSTANCE, element);

                if (result.error().isPresent()) {
                    throw new JsonSyntaxException(result.error().get().message());
                }

                CompositeRenderTypeData data = result.result().orElseThrow();
                if (renderTypes.put(id, new RenderTypeCache(id.toString(), data)) != null) {
                    throw new IllegalStateException("Duplicate data file ignored with ID " + id);
                }
            } catch (Exception e) {
                Veil.LOGGER.error("Couldn't parse data file {} from {}", id, CONVERTER.idToFile(id), e);
            }
        }

        this.renderTypes.clear();
        this.renderTypes.putAll(renderTypes);
        Veil.LOGGER.info("Loaded {} render types", renderTypes.size());
    }

    private static class RenderTypeCache {

        private final String name;
        private final CompositeRenderTypeData data;
        private final Int2ObjectMap<RenderType> objectCache;
        private RenderType defaultCache;
        private boolean defaultError;

        public RenderTypeCache(String name, CompositeRenderTypeData data) {
            this.name = name;
            this.data = data;
            this.objectCache = new Int2ObjectArrayMap<>(4);
            this.defaultCache = null;
        }

        public @Nullable RenderType get(Object... params) {
            if (params.length == 0) {
                if (this.defaultError) {
                    return null;
                }

                if (this.defaultCache == null) {
                    try {
                        this.defaultCache = this.data.createRenderType(this.name);
                    } catch (Exception e) {
                        Veil.LOGGER.error("Failed to create rendertype {} with no parameters", this.name, e);
                        this.defaultError = true;
                    }
                }
                return this.defaultCache;
            }

            return this.objectCache.computeIfAbsent(Arrays.hashCode(params), i -> {
                try {
                    return this.data.createRenderType(this.name, params);
                } catch (Exception e) {
                    Veil.LOGGER.error("Failed to create rendertype {} with parameters: [{}]", this.name, Arrays.stream(params).map(Objects::toString).collect(Collectors.joining(", ")), e);
                    return null;
                }
            });
        }
    }
}
