import foundry.veil.api.client.color.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class ColorTest {

    @Test
    public void testSet() {
        Color color = new Color(Color.WHITE);
        assertEquals(Color.WHITE, color);
        assertEquals(Color.WHITE.argb(), color.argb());

        color.setARGB(0xFFFF00FF);
        assertEquals(0xFF00FF, color.rgb());
        assertEquals(0xFFFF00FF, color.argb());

        assertNotEquals(Color.WHITE, color);
        assertNotEquals(Color.WHITE.argb(), color.argb());
    }

    @Test
    public void testLerp() {
        Color first = new Color(Color.RED);
        Color second = new Color(Color.WHITE);
        Color lerp = first.lerp(second, 0.5F, new Color());

        assertEquals(1.0, lerp.red());
        assertEquals(0.5, lerp.green());
        assertEquals(0.5, lerp.blue());
        assertEquals(1.0, lerp.alpha());
    }

    @Test
    public void testHSV() {
        Color color = new Color().setInt(24, 98, 118);
        assertEquals(24, color.redInt());
        assertEquals(98, color.greenInt());
        assertEquals(118, color.blueInt());
        assertEquals(193, color.hue(), 0.5);
        assertEquals(0.67, color.saturation(), 0.01);
        assertEquals(0.28, color.luminance(), 0.01);

        color.setHSV(193, 0.67F, 0.28F);
        assertEquals(24, color.redInt());
        assertEquals(98, color.greenInt());
        assertEquals(119, color.blueInt());
        assertEquals(193, color.hue(), 0.1);
        assertEquals(0.67, color.saturation(), 0.01);
        assertEquals(0.28, color.luminance(), 0.01);
    }
}
