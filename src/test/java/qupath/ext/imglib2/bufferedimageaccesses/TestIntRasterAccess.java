package qupath.ext.imglib2.bufferedimageaccesses;

import net.imglib2.img.basictypeaccess.IntAccess;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import qupath.ext.imglib2.Utils;

import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;

public class TestIntRasterAccess {

    @Test
    void Check_Pixels() {
        int width = 3;
        int height = 2;
        int nChannels = 2;
        int[][] pixels = new int[][] {
                new int[] {
                        35584984, -6, 8,
                        4, 556, 7
                },
                new int[] {
                        34, -446456, 0,
                        65, 7, 790
                },
        };
        int[] expectedPixels = new int[] {
                35584984, -6, 8,
                4, 556, 7,

                34, -446456, 0,
                65, 7, 790
        };
        DataBuffer dataBuffer = new DataBufferInt(pixels, nChannels);
        Raster raster = Utils.createRaster(dataBuffer, width, height, nChannels);

        IntRasterAccess bufferedImageAccess = new IntRasterAccess(raster);

        assertArrayEqualsIntAccess(expectedPixels, bufferedImageAccess);
    }

    private void assertArrayEqualsIntAccess(int[] expectedArray, IntAccess access) {
        for (int i=0; i<expectedArray.length; i++) {
            Assertions.assertEquals(expectedArray[i], access.getValue(i));
        }
    }
}
