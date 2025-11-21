package qupath.ext.imglib2.accesses;

import net.imglib2.img.basictypeaccess.IntAccess;
import net.imglib2.type.numeric.ARGBType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

public class TestArgbBufferedImageAccess {

    @Test
    void Check_Pixels_With_Rgb_Image() {
        int width = 3;
        int height = 2;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        image.setRGB(
                0,
                0,
                width,
                height,
                new int[] {
                        ARGBType.rgba(12, 125, 44, 0), ARGBType.rgba(87, 125, 1, 0), ARGBType.rgba(69, 75, 98, 0),
                        ARGBType.rgba(54, 2, 43, 0), ARGBType.rgba(96, 25, 0, 0), ARGBType.rgba(9, 54, 5, 0)
                },
                0,
                width
        );
        int[] expectedPixels = new int[] {      // only the alpha changes
                ARGBType.rgba(12, 125, 44, 255), ARGBType.rgba(87, 125, 1, 255), ARGBType.rgba(69, 75, 98, 255),
                ARGBType.rgba(54, 2, 43, 255), ARGBType.rgba(96, 25, 0, 255), ARGBType.rgba(9, 54, 5, 255)
        };

        ArgbBufferedImageAccess argbBufferedImageAccess = new ArgbBufferedImageAccess(image);

        assertArrayEqualsIntAccess(expectedPixels, argbBufferedImageAccess);
    }

    @Test
    void Check_Pixels_With_Argb_Image() {
        int width = 3;
        int height = 2;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int[] expectedPixels = new int[] {
                ARGBType.rgba(12, 125, 44, 0), ARGBType.rgba(87, 125, 1, 0), ARGBType.rgba(69, 75, 98, 0),
                ARGBType.rgba(54, 2, 43, 0), ARGBType.rgba(96, 25, 0, 0), ARGBType.rgba(9, 54, 5, 0)
        };
        image.setRGB(0, 0, width, height, expectedPixels, 0, width);

        ArgbBufferedImageAccess argbBufferedImageAccess = new ArgbBufferedImageAccess(image);

        assertArrayEqualsIntAccess(expectedPixels, argbBufferedImageAccess);
    }

    private void assertArrayEqualsIntAccess(int[] expectedArray, IntAccess access) {
        for (int i=0; i<expectedArray.length; i++) {
            Assertions.assertEquals(expectedArray[i], access.getValue(i));
        }
    }
}
