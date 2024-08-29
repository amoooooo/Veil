package foundry.veil.api.client.color;

import net.minecraft.util.Mth;

import com.mojang.serialization.Codec;

/**
 * A simple color class that can be used to represent a color in RGBA format with utility functions.
 */
public class Color {
    public static Codec<Color> COLOR_CODEC = Codec.INT.xmap(Color::new, Color::getRGB);

    public static final Color WHITE = new Color(255, 255, 255, 255);
    public static final Color BLACK = new Color(0, 0, 0, 255);
    public static final Color RED = new Color(255, 0, 0, 255);
    public static final Color GREEN = new Color(0, 255, 0, 255);
    public static final Color BLUE = new Color(0, 0, 255, 255);
    public static final Color CLEAR = new Color(0, 0, 0, 0);

    //public static final Color RAINBOW = new Color(255,255,255,255);

    public static final Color VANILLA_TOOLTIP_BACKGROUND = new Color(0xf0_100010);
    public static final Color VANILLA_TOOLTIP_BORDER_TOP = new Color(0x50_5000ff);
    public static final Color VANILLA_TOOLTIP_BORDER_BOTTOM = new Color(0x50_28007f);
    float r, g, b, a;

    public static Color of(int col) {
        return new Color(col);
    }

    public Color(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    public static void tickRainbow(int ticks, float partialTick) {
        // slowly change the hue of the rainbow color, it should take 5 seconds to complete a full cycle using sin
        //RAINBOW.setHue((float) (Math.sin((ticks+partialTick) * 0.05f) * 0.5f + 0.5f));
    }

    public void lerp(Color other, float t) {
        r = Mth.lerp(t, r, other.r);
        g = Mth.lerp(t, g, other.g);
        b = Mth.lerp(t, b, other.b);
        a = Mth.lerp(t, a, other.a);
    }

    public void setHue(float hue) {
        float h = hue * 6.0f;
        int i = (int) Math.floor(h);
        float f = h - i;
        float p = 1.0f - a;
        float q = 1.0f - f * a;
        float t = 1.0f - (1.0f - f) * a;
        switch (i) {
            case 0 -> {
                r = a;
                g = t;
                b = p;
            }
            case 1 -> {
                r = q;
                g = a;
                b = p;
            }
            case 2 -> {
                r = p;
                g = a;
                b = t;
            }
            case 3 -> {
                r = p;
                g = q;
                b = a;
            }
            case 4 -> {
                r = t;
                g = p;
                b = a;
            }
            case 5 -> {
                r = a;
                g = p;
                b = q;
            }
        }
    }

    public Color(float r, float g, float b) {
        this(r, g, b, 1.0f);
    }

    public Color(int r, int g, int b, int a) {
        this(r / 255.0f, g / 255.0f, b / 255.0f, a / 255.0f);
    }

    public Color(int r, int g, int b) {
        this(r, g, b, 255);
    }

    public Color(int hex) {
        this((hex >> 16) & 0xFF, (hex >> 8) & 0xFF, hex & 0xFF, (hex >> 24) & 0xFF);
    }

    public Color(int hex, boolean hasAlpha) {
        this(hex);
        if (!hasAlpha)
            a = 1.0f;
    }

    public Color() {
        this(0, 0, 0, 0);
    }

    public Color(String hex) {
        this(Integer.parseInt(hex, 16));
    }

    public float getRed() {
        return r;
    }

    public Color add(int col) {
        r += (col >> 16) & 0xFF;
        g += (col >> 8) & 0xFF;
        b += col & 0xFF;
        a += (col >> 24) & 0xFF;
        return this;
    }

    public float getGreen() {
        return g;
    }

    public float getBlue() {
        return b;
    }

    public float getAlpha() {
        return a;
    }

    public int getRedInt() {
        return (int) (r * 255);
    }

    public int getGreenInt() {
        return (int) (g * 255);
    }

    public int getBlueInt() {
        return (int) (b * 255);
    }

    public int getAlphaInt() {
        return (int) (a * 255);
    }

    public int getHex() {
        return (getAlphaInt() << 24) | (getRedInt() << 16) | (getGreenInt() << 8) | getBlueInt();
    }

    public String getHexStr() {
        return String.format("%08X", getHex());
    }

    public void mix(Color color, float amount) {
        r = (r * (1.0f - amount)) + (color.r * amount);
        g = (g * (1.0f - amount)) + (color.g * amount);
        b = (b * (1.0f - amount)) + (color.b * amount);
        a = (a * (1.0f - amount)) + (color.a * amount);
    }

    public Color mixCopy(Color color, float amount) {
        Color newColor = new Color(r, g, b, a);
        newColor.mix(color, amount);
        return newColor;
    }

    public void lighten(float amount) {
        mix(Color.WHITE, amount);
    }

    public Color lightenCopy(float amount) {
        return mixCopy(Color.WHITE, amount);
    }

    public void darken(float amount) {
        mix(Color.BLACK, amount);
    }

    public Color darkenCopy(float amount) {
        return mixCopy(Color.BLACK, amount);
    }

    public void saturate(float amount) {
        float gray = (r + g + b) / 3.0f;
        r = (r * (1.0f - amount)) + (gray * amount);
        g = (g * (1.0f - amount)) + (gray * amount);
        b = (b * (1.0f - amount)) + (gray * amount);
    }

    public Color saturateCopy(float amount) {
        Color newColor = new Color(r, g, b, a);
        newColor.saturate(amount);
        return newColor;
    }

    public Color multiply(float r, float g, float b, float a) {
        return new Color(this.r * r, this.g * g, this.b * b, this.a * a);
    }

    public Color multiply(int col) {
        return multiply((col >> 16) & 0xFF, (col >> 8) & 0xFF, col & 0xFF, (col >> 24) & 0xFF);
    }

    public void desaturate(float amount) {
        saturate(-amount);
    }

    public Color desaturateCopy(float amount) {
        return saturateCopy(-amount);
    }

    public void invert() {
        r = 1.0f - r;
        g = 1.0f - g;
        b = 1.0f - b;
    }

    public Color invertCopy() {
        Color newColor = new Color(r, g, b, a);
        newColor.invert();
        return newColor;
    }

    public int getRGB() {
        return (getRedInt() << 16) | (getGreenInt() << 8) | getBlueInt();
    }

    public int getRGBA() {
        return (getRedInt() << 24) | (getGreenInt() << 16) | (getBlueInt() << 8) | getAlphaInt();
    }
}
