package foundry.veil.api.client.render.rendertype.layer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.ResourceLocationException;
import net.minecraft.resources.ResourceLocation;

import java.util.Arrays;
import java.util.IllegalFormatException;
import java.util.Locale;
import java.util.function.Function;

/**
 * Helper for managing codecs with string template values.
 *
 * @param <T> The type this value must become
 * @author Ocelot
 */
public sealed interface LayerTemplateValue<T> {

    Codec<LayerTemplateValue<String>> STRING_CODEC = Codec.STRING
            .xmap(name -> {
                if (name.contains("%")) {
                    return new FormattedValue<>(name, formatted -> formatted);
                }
                return new RawValue<>(name, name);
            }, LayerTemplateValue::rawValue);

    Codec<LayerTemplateValue<ResourceLocation>> LOCATION_CODEC = Codec.STRING
            .flatXmap(name -> {
                if (name.contains("%")) {
                    return DataResult.success(new FormattedValue<>(name, ResourceLocation::parse));
                }
                return ResourceLocation.read(name).map(loc -> new RawValue<>(loc.toString(), loc));
            }, value -> DataResult.success(value.rawValue()));

    /**
     * @return The raw input from the user
     */
    String rawValue();

    /**
     * Attempts to parse the real value of this template.
     *
     * @param params The provided parameters for formatting
     * @return The value of this template
     * @throws IllegalFormatException    If the formatting string provided is invalid for the parameters
     * @throws ResourceLocationException If the value is a resource location and is not valid
     */
    T parse(Object... params) throws IllegalFormatException, ResourceLocationException;

    /**
     * Creates a codec for the specified enum values.
     *
     * @param clazz The enum class to choose values from
     * @param <T>   The type of value to store
     * @return A codec that can use formatting strings to interpret the name of the enum value
     */
    static <T extends Enum<T>> Codec<LayerTemplateValue<T>> enumCodec(Class<T> clazz) {
        T[] values = clazz.getEnumConstants();
        String[] names = Arrays.stream(values).map(value -> value.name().toLowerCase()).toArray(String[]::new);
        String name = clazz.getSimpleName();
        String options = ". Valid options: " + String.join(", ", names);

        return Codec.STRING.xmap(value -> {
            for (int i = 0; i < names.length; i++) {
                if (names[i].equalsIgnoreCase(value)) {
                    return new RawValue<>(value, values[i]);
                }
            }
            return new FormattedValue<>(value, formattedValue -> {
                for (int i = 0; i < names.length; i++) {
                    if (names[i].equalsIgnoreCase(formattedValue)) {
                        return values[i];
                    }
                }
                throw new IllegalStateException("Unknown " + name + ": " + value + options);
            });
        }, LayerTemplateValue::rawValue);
    }

    /**
     * Creates a new raw value for the specified enum value.
     *
     * @param value The value to use
     * @param <T>   The type of enum value to store
     * @return A new raw value, using the name of the enum value as the raw name
     */
    static <T extends Enum<T>> RawValue<T> raw(T value) {
        return new RawValue<>(value.name().toLowerCase(Locale.ROOT), value);
    }

    record RawValue<T>(String rawValue, T value) implements LayerTemplateValue<T> {

        @Override
        public T parse(Object... params) {
            return this.value;
        }
    }

    record FormattedValue<T>(String rawValue, Function<String, T> decoder) implements LayerTemplateValue<T> {

        @Override
        public T parse(Object... params) {
            return this.decoder.apply(String.format(this.rawValue, params));
        }
    }
}
