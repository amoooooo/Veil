package foundry.veil.api.client.color;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * A representation of color as four floating point elements.
 *
 * @author Ocelot
 * @see Colorc
 * @since 1.0.0
 */
public class Color implements Colorc {

    /**
     * Allows only RGB components with a full alpha component.
     */
    public static final Codec<Integer> RGB_INT_CODEC = new ColorCodec(false);
    /**
     * Allows ARGB components.
     */
    public static final Codec<Integer> ARGB_INT_CODEC = new ColorCodec(true);
    /**
     * Allows only RGB components with a full alpha component.
     */
    public static final Codec<Color> RGB_CODEC = RGB_INT_CODEC.xmap(Color::new, Colorc::argb);
    /**
     * Allows ARGB components.
     */
    public static final Codec<Color> ARGB_CODEC = ARGB_INT_CODEC.xmap(Color::new, Colorc::argb);

    public static final Colorc WHITE = new Color(0xFFFFFFFF);
    public static final Colorc BLACK = new Color(0xFF000000);
    public static final Colorc RED = new Color(0xFFFF0000);
    public static final Colorc GREEN = new Color(0xFF00FF00);
    public static final Colorc BLUE = new Color(0xFF0000FF);
    public static final Colorc TRANSPARENT = new Color(0x00000000);

    //public static final Color RAINBOW = new Color(0xFFFFFFFF);

    public static final Colorc VANILLA_TOOLTIP_BACKGROUND = new Color(0xF0100010);
    public static final Colorc VANILLA_TOOLTIP_BORDER_TOP = new Color(0x505000FF);
    public static final Colorc VANILLA_TOOLTIP_BORDER_BOTTOM = new Color(0x5028007F);

    private float red;
    private float green;
    private float blue;
    private float alpha;

    /**
     * Creates a new black color with full alpha.
     */
    public Color() {
        this.red = 0.0F;
        this.green = 0.0F;
        this.blue = 0.0F;
        this.alpha = 1.0F;
    }

    /**
     * Creates a new color with the specified RGB values.
     *
     * @param rgb The color values formatted as RRGGBB
     * @see Color#Color(int, boolean)
     */
    public Color(int rgb) {
        this(rgb, false);
    }

    /**
     * Creates a new color with the specified ARGB values.
     *
     * @param color The color values formatted as AARRGGBB
     * @param alpha Whether the color values contain an alpha component
     */
    public Color(int color, boolean alpha) {
        if (alpha) {
            this.setARGB(color);
        } else {
            this.setARGB(0xFF000000 | color);
        }
    }

    /**
     * Sets the red, green, and blue components of this color.
     *
     * @param red   The red amount
     * @param green The green amount
     * @param blue  The blue amount
     */
    public Color(float red, float green, float blue) {
        this(red, green, blue, 1.0F);
    }

    /**
     * Sets the red, green, blue, and alpha components of this color.
     *
     * @param red   The red amount
     * @param green The green amount
     * @param blue  The blue amount
     * @param alpha The alpha amount
     */
    public Color(float red, float green, float blue, float alpha) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
    }

    /**
     * Copies the color from the specified color.
     *
     * @param copy The color to copy from
     */
    public Color(Colorc copy) {
        this.red = copy.red();
        this.green = copy.green();
        this.blue = copy.blue();
        this.alpha = copy.alpha();
    }

    /**
     * Sets the value of the red component.
     *
     * @param red The new red value
     * @return This color
     */
    public Color red(float red) {
        this.red = red;
        return this;
    }

    /**
     * Sets the value of the green component.
     *
     * @param green The new green value
     * @return This color
     */
    public Color green(float green) {
        this.green = green;
        return this;
    }

    /**
     * Sets the value of the blue component.
     *
     * @param blue The new blue value
     * @return This color
     */
    public Color blue(float blue) {
        this.blue = blue;
        return this;
    }

    /**
     * Sets the value of the alpha component.
     *
     * @param alpha The new alpha value
     * @return This color
     */
    public Color alpha(float alpha) {
        this.alpha = alpha;
        return this;
    }

    /**
     * Sets the value of the red component from <code>0</code> to <code>255</code>.
     *
     * @param red The new red value
     * @return This color
     */
    public Color redInt(int red) {
        return this.red(red / 255.0F);
    }

    /**
     * Sets the value of the green component from <code>0</code> to <code>255</code>.
     *
     * @param green The new green value
     * @return This color
     */
    public Color greenInt(int green) {
        return this.green(green / 255.0F);
    }

    /**
     * Sets the value of the blue component from <code>0</code> to <code>255</code>.
     *
     * @param blue The new blue value
     * @return This color
     */
    public Color blueInt(int blue) {
        return this.blue(blue / 255.0F);
    }

    /**
     * Sets the value of the alpha component from <code>0</code> to <code>255</code>.
     *
     * @param alpha The new alpha value
     * @return This color
     */
    public Color alphaInt(int alpha) {
        return this.alpha(alpha / 255.0F);
    }

    /**
     * Sets the red, green, and blue components of this color.
     *
     * @param red   The new red amount
     * @param green The new green amount
     * @param blue  The new blue amount
     * @return This color
     */
    public Color set(float red, float green, float blue) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        return this;
    }

    /**
     * Sets the red, green, blue, and alpha components of this color.
     *
     * @param red   The new red amount
     * @param green The new green amount
     * @param blue  The new blue amount
     * @param alpha The new alpha amount
     * @return This color
     */
    public Color set(float red, float green, float blue, float alpha) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
        return this;
    }

    /**
     * Sets the red, green, and blue components of this color as ints from <code>0</code> to <code>255</code>.
     *
     * @param red   The new red amount
     * @param green The new green amount
     * @param blue  The new blue amount
     * @return This color
     */
    public Color setInt(int red, int green, int blue) {
        return this.set(red / 255.0F, green / 255.0F, blue / 255.0F);
    }

    /**
     * Sets the red, green, blue, and alpha components of this color as ints from <code>0</code> to <code>255</code>.
     *
     * @param red   The new red amount
     * @param green The new green amount
     * @param blue  The new blue amount
     * @param alpha The new alpha amount
     * @return This color
     */
    public Color setInt(int red, int green, int blue, int alpha) {
        return this.set(red / 255.0F, green / 255.0F, blue / 255.0F, alpha / 255.0F);
    }

    /**
     * Sets the red, green, and blue components of this color to the specified value.
     *
     * @param rgb The color values formatted as RRGGBB
     * @return This color
     */
    public Color setRGB(int rgb) {
        return this.setInt((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
    }

    /**
     * Sets the red, green, blue, and alpha components of this color to the specified value.
     *
     * @param argb The color values formatted as AARRGGBB
     * @return This color
     */
    public Color setARGB(int argb) {
        return this.setInt((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, (argb >> 24) & 0xFF);
    }

    @Override
    public float red() {
        return this.red;
    }

    @Override
    public float green() {
        return this.green;
    }

    @Override
    public float blue() {
        return this.blue;
    }

    @Override
    public float alpha() {
        return this.alpha;
    }

    /**
     * Linearly interpolates between this color and the specified color.
     *
     * @param other The other color to store in
     * @param delta The delta from 0 to 1
     * @return This color
     */
    public Color lerp(Colorc other, float delta) {
        return this.set(
                this.red() + (this.red() - other.red()) * delta,
                this.green() + (this.green() - other.green()) * delta,
                this.blue() + (this.blue() - other.blue()) * delta,
                this.alpha() + (this.alpha() - other.alpha()) * delta);
    }

    /**
     * Mixes this color with the specified color.
     *
     * @param color  The color to mix with
     * @param amount The amount of that color to mix from 0 to 1
     * @return This color
     */
    public Color mix(Colorc color, float amount) {
        return this.set(
                this.red() * (1.0f - amount) + color.red() * amount,
                this.green() * (1.0f - amount) + color.green() * amount,
                this.blue() * (1.0f - amount) + color.blue() * amount,
                this.alpha() * (1.0f - amount) + color.alpha() * amount);
    }

    /**
     * Mixes this color with white to "brighten" it.
     *
     * @param amount The amount of white to add
     * @return This color
     */
    public Color lighten(float amount) {
        return this.mix(Color.WHITE, amount, this);
    }

    /**
     * Mixes this color with black to "darken" it.
     *
     * @param amount The amount of black to add
     * @return This color
     */
    public Color darken(float amount) {
        return this.mix(Color.BLACK, amount, this);
    }

    /**
     * Inverts this color.
     *
     * @return This color
     */
    public Color invert() {
        return this.invert(this);
    }

    /**
     * Applies a grayscale filter to this color.
     *
     * @return The passed in color
     */
    public Color grayscale() {
        return this.grayscale(this);
    }

    /**
     * Applies a sepia filter to this color.
     *
     * @return The passed in color
     */
    public Color sepia() {
        return this.sepia(this);
    }

    /**
     * Converts the specified hue value (HSV) to RGB.
     *
     * @param hue The hue angle from 0 to 360 degrees
     * @return This color
     */
    public Color setHue(float hue) {
        return this.setHSV(hue, this.saturation(), this.luminance(), this);
    }

    /**
     * Converts the specified saturation value (HSV) to RGB.
     *
     * @param saturation The saturation percentage from 0 to 1
     * @return This color
     */
    public Color setSaturation(float saturation) {
        return this.setHSV(this.hue(), saturation, this.luminance(), this);
    }

    /**
     * Converts the specified luminance value (HSV) to RGB.
     *
     * @param luminance The brightness percentage from 0 to 1
     * @return This color
     */
    public Color setLuminance(float luminance) {
        return this.setHSV(this.hue(), this.saturation(), luminance, this);
    }

    /**
     * Converts the specified hue, saturation, and luminance values (HSV) to RGB.
     *
     * @param hue        The hue angle from 0 to 360 degrees
     * @param saturation The saturation percentage from 0 to 1
     * @param luminance  The brightness percentage from 0 to 1
     * @return This color
     */
    public Color setHSV(float hue, float saturation, float luminance) {
        return this.setHSV(hue, saturation, luminance, this);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Colorc color)) {
            return false;
        }

        return Float.compare(this.red, color.red()) == 0 &&
                Float.compare(this.green, color.green()) == 0 &&
                Float.compare(this.blue, color.blue()) == 0 &&
                Float.compare(this.alpha, color.alpha()) == 0;
    }

    @Override
    public int hashCode() {
        int result = Float.hashCode(this.red);
        result = 31 * result + Float.hashCode(this.green);
        result = 31 * result + Float.hashCode(this.blue);
        result = 31 * result + Float.hashCode(this.alpha);
        return result;
    }

    /**
     * Codec for deserializing colors values. Supports decimal numbers, hexadecimal strings, and 3-4 length arrays of decimal or hexadecimal values.
     *
     * @author Ocelot
     */
    private record ColorCodec(boolean alpha) implements Codec<Integer> {

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
                        int color = Integer.parseUnsignedInt(colorString.substring(1), 16);
                        return DataResult.success(this.alpha ? color : 0xFFFFFF & color);
                    } catch (NumberFormatException e) {
                        return DataResult.error(() -> "Invalid color hex: " + colorString + ". " + e.getMessage());
                    }
                } else if (colorString.startsWith("0x") || colorString.startsWith("0X")) {
                    try {
                        int color = Integer.parseUnsignedInt(colorString.substring(2), 16);
                        return DataResult.success(this.alpha ? color : 0xFFFFFF & color);
                    } catch (NumberFormatException e) {
                        return DataResult.error(() -> "Invalid color hex: " + colorString + ". " + e.getMessage());
                    }
                } else {
                    try {
                        int color = Integer.parseUnsignedInt(colorString);
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
}
