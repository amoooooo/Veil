package foundry.veil.api.client.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.renderer.RenderType;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class VertexFormatCodec {

    private static final Map<String, VertexFormatElement> DEFAULT_ELEMENTS = Map.of(
            "POSITION", VertexFormatElement.POSITION,
            "COLOR", VertexFormatElement.COLOR,
            "UV0", VertexFormatElement.UV0,
            "UV1", VertexFormatElement.UV1,
            "UV2", VertexFormatElement.UV2,
            "NORMAL", VertexFormatElement.NORMAL,
            "UV", VertexFormatElement.UV);
    private static final Map<String, VertexFormat> DEFAULT_FORMATS = Map.ofEntries(
            Map.entry("BLIT_SCREEN", DefaultVertexFormat.BLIT_SCREEN),
            Map.entry("BLOCK", DefaultVertexFormat.BLOCK),
            Map.entry("NEW_ENTITY", DefaultVertexFormat.NEW_ENTITY),
            Map.entry("PARTICLE", DefaultVertexFormat.PARTICLE),
            Map.entry("POSITION", DefaultVertexFormat.POSITION),
            Map.entry("POSITION_COLOR", DefaultVertexFormat.POSITION_COLOR),
            Map.entry("POSITION_COLOR_NORMAL", DefaultVertexFormat.POSITION_COLOR_NORMAL),
            Map.entry("POSITION_COLOR_LIGHTMAP", DefaultVertexFormat.POSITION_COLOR_LIGHTMAP),
            Map.entry("POSITION_TEX", DefaultVertexFormat.POSITION_TEX),
            Map.entry("POSITION_TEX_COLOR", DefaultVertexFormat.POSITION_TEX_COLOR),
            Map.entry("POSITION_COLOR_TEX_LIGHTMAP", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP),
            Map.entry("POSITION_TEX_LIGHTMAP_COLOR", DefaultVertexFormat.POSITION_TEX_LIGHTMAP_COLOR),
            Map.entry("POSITION_TEX_COLOR_NORMAL", DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL));
    private static final Object2IntMap<String> DEFAULT_BUFFER_SIZES = new Object2IntArrayMap<>(Map.of(
            "BIG", RenderType.BIG_BUFFER_SIZE,
            "SMALL", RenderType.SMALL_BUFFER_SIZE,
            "TRANSIENT", RenderType.TRANSIENT_BUFFER_SIZE));

    private static final Codec<VertexFormatElement.Type> ELEMENT_TYPE_CODEC = Codec.STRING.flatXmap(name -> {
        for (VertexFormatElement.Type type : VertexFormatElement.Type.values()) {
            if (type.name().equalsIgnoreCase(name)) {
                return DataResult.success(type);
            }
        }
        return DataResult.error(() -> "Unknown element type: " + name.toLowerCase(Locale.ROOT));
    }, type -> DataResult.success(type.name().toLowerCase(Locale.ROOT)));
    private static final Codec<VertexFormatElement.Usage> ELEMENT_USAGE_CODEC = Codec.STRING.flatXmap(name -> {
        for (VertexFormatElement.Usage usage : VertexFormatElement.Usage.values()) {
            if (usage.name().equalsIgnoreCase(name)) {
                return DataResult.success(usage);
            }
        }
        return DataResult.error(() -> "Unknown mode: " + name.toLowerCase(Locale.ROOT));
    }, usage -> DataResult.success(usage.name().toLowerCase(Locale.ROOT)));
    public static final Codec<VertexFormat.Mode> MODE_CODEC = Codec.STRING.flatXmap(name -> {
        for (VertexFormat.Mode mode : VertexFormat.Mode.values()) {
            if (mode.name().equalsIgnoreCase(name)) {
                return DataResult.success(mode);
            }
        }
        return DataResult.error(() -> "Unknown mode: " + name.toLowerCase(Locale.ROOT));
    }, mode -> DataResult.success(mode.name().toLowerCase(Locale.ROOT)));

    private static final Codec<VertexFormatElement> DEFAULT_ELEMENT_CODEC = Codec.STRING.flatXmap(name -> {
        String key = name.toUpperCase(Locale.ROOT);
        VertexFormatElement element = DEFAULT_ELEMENTS.get(key);
        return element != null ? DataResult.success(element) : DataResult.error(() -> "Unknown default element: " + key);
    }, element -> {
        for (Map.Entry<String, VertexFormatElement> entry : DEFAULT_ELEMENTS.entrySet()) {
            if (element.equals(entry.getValue())) {
                return DataResult.success(entry.getKey());
            }
        }
        return DataResult.error(() -> "Unknown default element for: " + element);
    });
    // FIXME
    private static final Codec<VertexFormatElement> FULL_ELEMENT_CODEC = null;/*= RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("index").forGetter(VertexFormatElement::index),
            ELEMENT_TYPE_CODEC.fieldOf("type").forGetter(VertexFormatElement::type),
            ELEMENT_USAGE_CODEC.fieldOf("usage").forGetter(VertexFormatElement::usage),
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("count").forGetter(VertexFormatElement::count)
    ).apply(instance, VertexFormatElement::new));*/

    public static final Codec<VertexFormatElement> ELEMENT_CODEC = Codec.either(DEFAULT_ELEMENT_CODEC, FULL_ELEMENT_CODEC).xmap(either -> either.map(a -> a, a -> a), element -> {
        for (Map.Entry<String, VertexFormatElement> entry : DEFAULT_ELEMENTS.entrySet()) {
            if (element.equals(entry.getValue())) {
                return Either.left(element);
            }
        }
        return Either.right(element);
    });

    private static final Codec<VertexFormat> DEFAULT_CODEC = Codec.STRING.flatXmap(name -> {
        String key = name.toUpperCase(Locale.ROOT);
        VertexFormat format = DEFAULT_FORMATS.get(key);
        return format != null ? DataResult.success(format) : DataResult.error(() -> "Unknown default vertex format: " + key);
    }, format -> {
        for (Map.Entry<String, VertexFormat> entry : DEFAULT_FORMATS.entrySet()) {
            if (format.equals(entry.getValue())) {
                return DataResult.success(entry.getKey());
            }
        }
        return DataResult.error(() -> "Unknown default vertex format for: " + format);
    });
    private static final Codec<VertexFormat> FULL_CODEC = null;/*Codec.unboundedMap(Codec.STRING, ELEMENT_CODEC).xmap(map -> new VertexFormat(ImmutableMap.copyOf(map)), format -> {
        List<String> keys = format.getElementAttributeNames();
        List<VertexFormatElement> values = format.getElements();
        ImmutableMap.Builder<String, VertexFormatElement> map = ImmutableMap.builderWithExpectedSize(keys.size());
        for (int i = 0; i < keys.size(); i++) {
            map.put(keys.get(i), values.get(i));
        }
        return map.build();
    });*/

    public static final Codec<VertexFormat> CODEC = Codec.either(DEFAULT_CODEC, FULL_CODEC).xmap(either -> either.map(a -> a, a -> a), format -> {
        for (Map.Entry<String, VertexFormat> entry : DEFAULT_FORMATS.entrySet()) {
            if (format.equals(entry.getValue())) {
                return Either.left(format);
            }
        }
        return Either.right(format);
    });

    private static final Codec<Integer> DEFAULT_SIZE_CODEC = Codec.STRING.flatXmap(name -> {
        String key = name.toUpperCase(Locale.ROOT);
        int format = DEFAULT_BUFFER_SIZES.getOrDefault(key, -1);
        return format != -1 ? DataResult.success(format) : DataResult.error(() -> "Unknown default buffer size: " + key);
    }, format -> {
        for (Object2IntMap.Entry<String> entry : DEFAULT_BUFFER_SIZES.object2IntEntrySet()) {
            if (format == entry.getIntValue()) {
                return DataResult.success(entry.getKey());
            }
        }
        return DataResult.error(() -> "Unknown default vertex format for: " + format);
    });

    public static final Codec<Integer> BUFFER_SIZE_CODEC = Codec.either(DEFAULT_SIZE_CODEC, Codec.intRange(0, Integer.MAX_VALUE)).xmap(either -> either.map(a -> a, a -> a), format -> {
        for (Object2IntMap.Entry<String> entry : DEFAULT_BUFFER_SIZES.object2IntEntrySet()) {
            if (format == entry.getIntValue()) {
                return Either.left(format);
            }
        }
        return Either.right(format);
    });
}
