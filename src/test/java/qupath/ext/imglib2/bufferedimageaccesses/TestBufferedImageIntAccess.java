package qupath.ext.imglib2.bufferedimageaccesses;

import net.imglib2.img.basictypeaccess.IntAccess;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import qupath.ext.imglib2.Utils;
import qupath.lib.images.servers.PixelType;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;

public class TestBufferedImageIntAccess {

    @Test
    void Check_Pixels() {
        int width = 3;
        int height = 2;
        int nChannels = 2;
        int[][] pixels = new int[][] {
                new int[] {
                        35584984, 6, 8,
                        4, 556, 7
                },
                new int[] {
                        34, 446456, 0,
                        65, 7, 790
                },
        };
        int[] expectedPixels = new int[] {
                35584984, 6, 8,
                4, 556, 7,

                34, 446456, 0,
                65, 7, 790
        };
        DataBuffer dataBuffer = new DataBufferInt(pixels, nChannels);
        BufferedImage image = Utils.createBufferedImage(dataBuffer, width, height, nChannels, PixelType.INT32);

        BufferedImageIntAccess bufferedImageAccess = new BufferedImageIntAccess(image);

        assertArrayEqualsIntAccess(expectedPixels, bufferedImageAccess);
    }

    private void assertArrayEqualsIntAccess(int[] expectedArray, IntAccess access) {
        for (int i=0; i<expectedArray.length; i++) {
            Assertions.assertEquals(expectedArray[i], access.getValue(i));
        }
    }
}
