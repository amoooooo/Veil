package foundry.veil.api.util;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Codec for deserializing colors values. Supports decimal numbers, hexadecimal strings, and 3-4 length arrays of decimal or hexadecimal values.
 *
 * @author Ocelot
 */
public final class ColorCodec implements Codec<Integer> {

    /**
     * Allows only RGB components with a full alpha component.
     */
    public static final Codec<Integer> RGB_CODEC = new ColorCodec(false);

    /**
     * Allows ARGB components.
     */
    public static final Codec<Integer> ARGB_CODEC = new ColorCodec(true);

    private final boolean alpha;

    private ColorCodec(boolean alpha) {
        this.alpha = alpha;
    }

    private <T> DataResult<Integer> decodeElement(DynamicOps<T> ops, T input) {
        Optional<Number> numberValue = ops.getNumberValue(input).result();
        if (numberValue.isPresent()) {
            int color = numberValue.get().intValue();
            return DataResult.success(this.alpha ? color : 0xFFFFFF & color);
        }

        Optional<String> stringValue = ops.getStringValue(input).result();
        if (stringValue.isPresent()) {
            String colorString = stringValue.get();
            if (colorString.startsWith("#")) {
                try {
                    int color = Integer.parseInt(colorString.substring(1), 16);
                    return DataResult.success(this.alpha ? color : 0xFFFFFF & color);
                } catch (NumberFormatException e) {
                    return DataResult.error(() -> "Invalid color hex: " + colorString + ". " + e.getMessage());
                }
            } else if (colorString.startsWith("0x") || colorString.startsWith("0X")) {
                try {
                    int color = Integer.parseInt(colorString.substring(2), 16);
                    return DataResult.success(this.alpha ? color : 0xFFFFFF & color);
                } catch (NumberFormatException e) {
                    return DataResult.error(() -> "Invalid color hex: " + colorString + ". " + e.getMessage());
                }
            } else {
                try {
                    int color = Integer.parseInt(colorString);
                    return DataResult.success(this.alpha ? color : 0xFFFFFF & color);
                } catch (NumberFormatException e) {
                    return DataResult.error(() -> "Invalid color int: " + colorString + ". " + e.getMessage());
                }
            }
        }

        return DataResult.error(() -> "Not a color int, hex, or string");
    }

    @Override
    public <T> DataResult<Pair<Integer, T>> decode(DynamicOps<T> ops, T input) {
        DataResult<Integer> numberElement = this.decodeElement(ops, input);
        if (numberElement.isSuccess()) {
            return numberElement.map(col -> Pair.of(col, input));
        }

        Optional<Consumer<Consumer<T>>> listValue = ops.getList(input).result();
        if (listValue.isPresent()) {
            List<T> values = new ArrayList<>(4);
            listValue.get().accept(values::add);

            if (values.size() < 3 || values.size() > 4) {
                return DataResult.error(() -> "Expected RGB and optionally A components");
            }

            int result = 0;
            for (int i = 0; i < 3; i++) {
                DataResult<Integer> colorElement = this.decodeElement(ops, values.get(i));
                if (!colorElement.isSuccess()) {
                    int index = i;
                    return colorElement.map(col -> Pair.of(col, input)).mapError(s -> s + " at index " + index);
                }
                result |= (colorElement.getOrThrow() & 0xFF) << (16 - i * 8);
            }
            if (this.alpha) {
                if (values.size() == 4) {
                    DataResult<Integer> colorElement = this.decodeElement(ops, values.get(3));
                    if (!colorElement.isSuccess()) {
                        return colorElement.map(col -> Pair.of(col, input)).mapError(s -> s + " at index 3");
                    }
                    result |= (colorElement.getOrThrow() & 0xFF) << 24;
                } else {
                    result |= 0xFF000000;
                }
            }

            return DataResult.success(Pair.of(result, input));
        }

        return DataResult.error(() -> "Not a color int, hex, string, or list of values");
    }

    @Override
    public <T> DataResult<T> encode(Integer input, DynamicOps<T> ops, T prefix) {
        return DataResult.success(ops.createInt(this.alpha ? input : 0xFFFFFF & input));
    }
}
