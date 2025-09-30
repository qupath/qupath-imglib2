package qupath.ext.imglib2.bufferedimageaccesses;

import net.imglib2.img.basictypeaccess.ShortAccess;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import qupath.ext.imglib2.Utils;
import qupath.lib.images.servers.PixelType;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferShort;

public class TestBufferedImageShortAccess {

    @Test
    void Check_Pixels() {
        int width = 3;
        int height = 2;
        int nChannels = 2;
        short[][] pixels = new short[][] {
                new short[] {
                        355, 6, 8,
                        4, 556, 7
                },
                new short[] {
                        34, 446, 0,
                        65, 7, 790
                },
        };
        short[] expectedPixels = new short[] {
                355, 6, 8,
                4, 556, 7,

                34, 446, 0,
                65, 7, 790
        };
        DataBuffer dataBuffer = new DataBufferShort(pixels, nChannels);
        BufferedImage image = Utils.createBufferedImage(dataBuffer, width, height, nChannels, PixelType.INT16);

        BufferedImageShortAccess bufferedImageAccess = new BufferedImageShortAccess(image);

        assertArrayEqualsShortAccess(expectedPixels, bufferedImageAccess);
    }

    private void assertArrayEqualsShortAccess(short[] expectedArray, ShortAccess access) {
        for (int i=0; i<expectedArray.length; i++) {
            Assertions.assertEquals(expectedArray[i], access.getValue(i));
        }
    }
}
