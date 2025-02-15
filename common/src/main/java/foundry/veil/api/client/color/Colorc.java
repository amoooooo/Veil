package foundry.veil.api.client.color;

import org.jetbrains.annotations.Contract;

/**
 * An immutable view of a {@link Color}. All methods in this interface are pure.
 *
 * @author Ocelot
 * @see Color
 * @since 1.0.0
 */
public interface Colorc {

    /**
     * @return The red component of this color
     */
    @Contract(pure = true)
    float red();

    /**
     * @return The green component of this color
     */
    @Contract(pure = true)
    float green();

    /**
     * @return The blue component of this color
     */
    @Contract(pure = true)
    float blue();

    /**
     * @return The alpha component of this color
     */
    @Contract(pure = true)
    float alpha();

    /**
     * @return The value of red as an int from 0 to 255
     */
    @Contract(pure = true)
    default int redInt() {
        return (int) (this.red() * 255.0F);
    }

    /**
     * @return The value of green as an int from 0 to 255
     */
    @Contract(pure = true)
    default int greenInt() {
        return (int) (this.green() * 255.0F);
    }

    /**
     * @return The value of blue as an int from 0 to 255
     */
    @Contract(pure = true)
    default int blueInt() {
        return (int) (this.blue() * 255.0F);
    }

    /**
     * @return The value of alpha as an int from 0 to 255
     */
    @Contract(pure = true)
    default int alphaInt() {
        return (int) (this.alpha() * 255.0F);
    }

    /**
     * @return This color formatted as an int (RRGGBB)
     */
    @Contract(pure = true)
    default int rgb() {
        return ((int) (this.red() * 255.0F) & 0xFF) << 16 | ((int) (this.green() * 255.0F) & 0xFF) << 8 | (int) (this.blue() * 255.0F) & 0xFF;
    }

    /**
     * @return This color formatted as an int (AARRGGBB)
     */
    @Contract(pure = true)
    default int argb() {
        return ((int) (this.alpha() * 255.0F) & 0xFF) << 24 | ((int) (this.red() * 255.0F) & 0xFF) << 16 | ((int) (this.green() * 255.0F) & 0xFF) << 8 | (int) (this.blue() * 255.0F) & 0xFF;
    }

    /**
     * @return The smallest color component (red, green, or blue)
     */
    @Contract(pure = true)
    default float minComponent() {
        return Math.min(this.red(), Math.min(this.green(), this.blue()));
    }

    /**
     * @return The largest color component (red, green, or blue)
     */
    @Contract(pure = true)
    default float maxComponent() {
        return Math.max(this.red(), Math.max(this.green(), this.blue()));
    }

    /**
     * @return The angle in degrees around the color wheel
     */
    @Contract(pure = true)
    default float hue() {
        float min = this.minComponent();
        float max = this.maxComponent();

        float hue;
        if (this.red() == max) {
            hue = 60.0F * (this.green() - this.blue()) / (max - min);
        } else if (this.green() == max) {
            hue = 60.0F * (2.0F + (this.blue() - this.red()) / (max - min));
        } else {
            hue = 60.0F * (4.0F + (this.red() - this.green()) / (max - min));
        }

        hue %= 360;
        if (hue < 0) {
            hue += 360;
        }
        return hue;
    }

    /**
     * @return The color saturation percentage
     */
    @Contract(pure = true)
    default float saturation() {
        float min = this.minComponent();
        float max = this.maxComponent();
        if (min == max) {
            return 0.0F;
        }

        float luminance = this.luminance();
        if (luminance <= 0.5) {
            return (max - min) / (max + min);
        } else {
            return (max - min) / (2.0F - max - min);
        }
    }

    /**
     * @return The luminance percentage
     */
    @Contract(pure = true)
    default float luminance() {
        return (this.minComponent() + this.maxComponent()) / 2.0F;
    }

    /**
     * Linearly interpolates between this color and the specified color.
     *
     * @param other The other color to store in
     * @param delta The delta from 0 to 1
     * @param store The color to store in
     * @return The passed in color
     */
    @Contract(pure = true)
    default Color lerp(Colorc other, float delta, Color store) {
        return store.set(
                this.red() + (other.red() - this.red()) * delta,
                this.green() + (other.green() - this.green()) * delta,
                this.blue() + (other.blue() - this.blue()) * delta,
                this.alpha() + (other.alpha() - this.alpha()) * delta);
    }

    /**
     * Mixes this color with the specified color.
     *
     * @param color  The color to mix with
     * @param amount The amount of that color to mix from 0 to 1
     * @param store  The color to store in
     * @return The passed in color
     */
    @Contract(pure = true)
    default Color mix(Colorc color, float amount, Color store) {
        return store.set(
                this.red() * (1.0f - amount) + color.red() * amount,
                this.green() * (1.0f - amount) + color.green() * amount,
                this.blue() * (1.0f - amount) + color.blue() * amount,
                this.alpha() * (1.0f - amount) + color.alpha() * amount);
    }

    /**
     * Mixes this color with white to "brighten" it.
     *
     * @param amount The amount of white to add
     * @param store  The color to store in
     * @return The passed in color
     */
    @Contract(pure = true)
    default Color lighten(float amount, Color store) {
        return this.mix(Color.WHITE, amount, store);
    }

    /**
     * Mixes this color with black to "darken" it.
     *
     * @param amount The amount of black to add
     * @param store  The color to store in
     * @return The passed in color
     */
    @Contract(pure = true)
    default Color darken(float amount, Color store) {
        return this.mix(Color.BLACK, amount, store);
    }

    /**
     * Inverts this color.
     *
     * @param store The color to store in
     * @return The passed in color
     */
    @Contract(pure = true)
    default Color invert(Color store) {
        float max = Math.max(1.0F, this.maxComponent());
        return store.set(max - this.red(), max - this.green(), max - this.blue());
    }

    /**
     * Applies a grayscale filter to this color.
     *
     * @param store The color to store in
     * @return The passed in color
     */
    @Contract(pure = true)
    default Color grayscale(Color store) {
        return this.setHSV(0.0F, 1.0F, this.luminance(), store);
    }

    /**
     * Applies a sepia filter to this color.
     *
     * @param store The color to store in
     * @return The passed in color
     */
    @Contract(pure = true)
    default Color sepia(Color store) {
        float red = this.red();
        float green = this.green();
        float blue = this.blue();
        return store.set(
                red * 0.393F + green * 0.769F + blue * 0.189F,
                red * 0.349F + green * 0.686f + blue * 0.168F,
                red * 0.272F + green * 0.534F + blue * 0.131F);
    }

    /**
     * Converts the specified hue value (HSV) to RGB.
     *
     * @param hue   The hue angle from 0 to 360 degrees
     * @param store The color to store the result in
     * @return The passed in color
     */
    @Contract(pure = true)
    default Color setHue(float hue, Color store) {
        return this.setHSV(hue, this.saturation(), this.luminance(), store);
    }

    /**
     * Converts the specified saturation value (HSV) to RGB.
     *
     * @param saturation The saturation percentage from 0 to 1
     * @param store      The color to store the result in
     * @return The passed in color
     */
    @Contract(pure = true)
    default Color setSaturation(float saturation, Color store) {
        return this.setHSV(this.hue(), saturation, this.luminance(), store);
    }

    /**
     * Converts the specified luminance value (HSV) to RGB.
     *
     * @param luminance The brightness percentage from 0 to 1
     * @param store     The color to store the result in
     * @return The passed in color
     */
    @Contract(pure = true)
    default Color setLuminance(float luminance, Color store) {
        return this.setHSV(this.hue(), this.saturation(), luminance, store);
    }

    /**
     * Converts the specified hue, saturation, and luminance values (HSV) to RGB.
     *
     * @param hue        The hue angle from 0 to 360 degrees
     * @param saturation The saturation percentage from 0 to 1
     * @param luminance  The brightness percentage from 0 to 1
     * @param store      The color to store the result in
     * @return The passed in color
     */
    @Contract(pure = true)
    default Color setHSV(float hue, float saturation, float luminance, Color store) {
        // https://www.niwa.nu/2013/05/math-behind-colorspace-conversions-rgb-hsl/
        if (saturation <= 0.0F) {
            return store.set(luminance, luminance, luminance);
        }

        // Validate inputs
        hue /= 360.0F;
        hue %= 1.0F;
        if (hue < 0) {
            hue += 1.0F;
        }
        saturation = Math.min(saturation, 1.0F);
        luminance = luminance < 0.0F ? 0.0F : Math.min(luminance, 1.0F);

        float temp1 = luminance < 0.5 ? luminance * (1.0F + saturation) : luminance + saturation - luminance * saturation;
        float temp2 = 2 * luminance - temp1;
        float tempR = hue + 1.0F / 3.0F;
        float tempG = hue;
        float tempB = hue - 1.0F / 3.0F;

        if (tempR < 0) {
            tempR++;
        } else if (tempR > 1) {
            tempR--;
        }

        if (tempG < 0) {
            tempG++;
        } else if (tempG > 1) {
            tempG--;
        }

        if (tempB < 0) {
            tempB++;
        } else if (tempB > 1) {
            tempB--;
        }

        // Red
        if (6.0 * tempR < 1.0) {
            store.red(temp2 + (temp1 - temp2) * 6.0F * tempR);
        } else if (2.0 * tempR < 1.0) {
            store.red(temp1);
        } else if (3.0 * tempR < 2.0) {
            store.red(temp2 + (temp1 - temp2) * (2.0F / 3.0F - tempR) * 6);
        }

        // Green
        if (6.0 * tempG < 1.0) {
            store.green(temp2 + (temp1 - temp2) * 6.0F * tempG);
        } else if (2.0 * tempG < 1.0) {
            store.green(temp1);
        } else if (3.0 * tempG < 2.0) {
            store.green(temp2 + (temp1 - temp2) * (2.0F / 3.0F - tempG) * 6);
        }

        // Blue
        if (6.0 * tempB < 1.0) {
            store.blue(temp2 + (temp1 - temp2) * 6.0F * tempB);
        } else if (2.0 * tempB < 1.0) {
            store.blue(temp1);
        } else if (3.0 * tempB < 2.0) {
            store.blue(temp2 + (temp1 - temp2) * (2.0F / 3.0F - tempB) * 6);
        }

        return store;
    }
}
