package foundry.veil.api.util;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Codec for serializing/deserializing enum constants.
 *
 * @param <T> The enum type to encode/decode
 * @author Ocelot
 */
public class EnumCodec<T extends Enum<?>> implements Codec<T> {

    private final String name;
    private final T[] values;
    private final String valid;
    private final Function<T, String> toString;

    private EnumCodec(String name, T[] values, Function<T, String> toString) {
        this.name = name;
        this.values = values;
        this.valid = Arrays.stream(values).map(toString).collect(Collectors.joining(", "));
        this.toString = toString;
    }

    @Override
    public <T1> DataResult<Pair<T, T1>> decode(DynamicOps<T1> ops, T1 input) {
        if (ops.compressMaps()) {
            return ops.getNumberValue(input).flatMap(index -> {
                int i = index.intValue();
                if (i < 0 || i >= this.values.length) {
                    return DataResult.error(() -> "Unknown " + this.name + " with index: " + i);
                }
                return DataResult.success(Pair.of(this.values[i], input));
            });
        } else {
            return ops.getStringValue(input).flatMap(valueName -> {
                for (T value : this.values) {
                    if (this.toString.apply(value).equalsIgnoreCase(valueName)) {
                        return DataResult.success(Pair.of(value, input));
                    }
                }
                return DataResult.error(() -> "Unknown " + this.name + ": " + valueName + ". Valid options: " + this.valid);
            });
        }
    }

    @Override
    public <T1> DataResult<T1> encode(T input, DynamicOps<T1> ops, T1 prefix) {
        if (ops.compressMaps()) {
            return DataResult.success(ops.createInt(input.ordinal()));
        } else {
            return DataResult.success(ops.createString(this.toString.apply(input)));
        }
    }

    public static <T extends Enum<?>> Builder<T> builder(String name) {
        return new Builder<>(name);
    }

    /**
     * Builder for creating a new {@link EnumCodec}.
     *
     * @param <T> The enum type to encode/decode
     * @author Ocelot
     */
    @SuppressWarnings("unchecked")
    public static class Builder<T extends Enum<?>> {

        private static final Function<Enum<?>, String> LOWERCASE = e -> e.name().toLowerCase(Locale.ROOT);
        private static final Function<Enum<?>, String> UPPERCASE = e -> e.name().toUpperCase(Locale.ROOT);

        private final String name;
        private T[] values;
        private Function<T, String> toString;

        public Builder(String name) {
            this.name = name;
            this.values = null;
            this.toString = (Function<T, String>) LOWERCASE;
        }

        /**
         * Sets the valid values of this codec to all the enum constants provided by the specified class.
         *
         * @param clazz The class to get the constants of
         */
        public Builder<T> values(Class<T> clazz) {
            this.values = clazz.getEnumConstants();
            return this;
        }

        /**
         * Sets the valid values of this codec to the specified values.
         *
         * @param values The values to use
         */
        @SafeVarargs
        public final Builder<T> values(T... values) {
            this.values = values;
            return this;
        }

        /**
         * Specifies what function to use when getting the name of an enum constant.
         *
         * @param toString The new function
         */
        public Builder<T> toStringFunction(Function<T, String> toString) {
            this.toString = Objects.requireNonNull(toString, "toString");
            return this;
        }

        /**
         * Specifies the standard names to be uppercase.
         */
        public Builder<T> uppercase() {
            this.toString = (Function<T, String>) UPPERCASE;
            return this;
        }

        /**
         * Specifies the standard names to be lowercase.
         */
        public Builder<T> lowercase() {
            this.toString = (Function<T, String>) LOWERCASE;
            return this;
        }

        /**
         * @return A new enum codec
         */
        public EnumCodec<T> build() {
            Objects.requireNonNull(this.values, "Values must be specified");
            if (this.values.length == 0) {
                throw new IllegalArgumentException("At least 1 value must be specified");
            }
            return new EnumCodec<>(this.name, this.values, this.toString);
        }
    }
}
