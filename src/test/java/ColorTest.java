import dev.jackraidenph.libraomni.common.ColorUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ColorTest {

    @Test
    public void rgbToLinearConsistency() {
        int argb = 0xFF112233;
        assertEquals(
                Integer.toHexString(argb),
                Integer.toHexString(ColorUtil.linearToARGB(ColorUtil.argbToLinear(argb)))
        );
    }

    @Test
    public void rgbToXYZConsistency() {
        int argb = 0xFF112233;
        assertEquals(
                Integer.toHexString(argb),
                Integer.toHexString(ColorUtil.xyzToARGB(ColorUtil.argbToXYZ(argb)))
        );
    }

    @Test
    public void rgbToLABConsistency() {
        int argb = 0xFF112233;
        assertEquals(
                Integer.toHexString(argb),
                Integer.toHexString(ColorUtil.labToARGB(ColorUtil.argbToLAB(argb)))
        );
    }

    @Test
    public void xyzToOKLABConsistency() {
        float[] xyz = new float[]{0f, 0f, 1f};
        float[] oklab = ColorUtil.xyzToOKLAB(xyz);
        float[] xyzNew = ColorUtil.oklabToXYZ(oklab);
        assertArrayEquals(
                xyz,
                xyzNew,
                1E-6f
        );
    }

}
