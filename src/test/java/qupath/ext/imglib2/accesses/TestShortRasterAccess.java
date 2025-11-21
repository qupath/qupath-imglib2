package qupath.ext.imglib2.accesses;

import net.imglib2.img.basictypeaccess.ShortAccess;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import qupath.ext.imglib2.Utils;

import java.awt.image.DataBuffer;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;

public class TestShortRasterAccess {

    @Test
    void Check_Pixels_With_DataBufferShort() {
        int width = 3;
        int height = 2;
        int nChannels = 2;
        short[][] pixels = new short[][] {
                new short[] {
                        -355, 6, 8,
                        4, 556, 7
                },
                new short[] {
                        34, 446, 0,
                        65, 7, -790
                },
        };
        short[] expectedPixels = new short[] {
                -355, 6, 8,
                4, 556, 7,

                34, 446, 0,
                65, 7, -790
        };
        DataBuffer dataBuffer = new DataBufferShort(pixels, nChannels);
        Raster raster = Utils.createRaster(dataBuffer, width, height, nChannels);

        ShortRasterAccess bufferedImageAccess = new ShortRasterAccess(raster);

        assertArrayEqualsShortAccess(expectedPixels, bufferedImageAccess);
    }

    @Test
    void Check_Pixels_With_DataBufferUShort() {
        int width = 3;
        int height = 2;
        int nChannels = 2;
        short[][] pixels = new short[][] {
                new short[] {
                        -355, 6, 8,
                        4, 556, 7
                },
                new short[] {
                        34, 446, 0,
                        65, 7, -790
                },
        };
        short[] expectedPixels = new short[] {
                -355, 6, 8,
                4, 556, 7,

                34, 446, 0,
                65, 7, -790
        };
        DataBuffer dataBuffer = new DataBufferUShort(pixels, nChannels);
        Raster raster = Utils.createRaster(dataBuffer, width, height, nChannels);

        ShortRasterAccess bufferedImageAccess = new ShortRasterAccess(raster);

        assertArrayEqualsShortAccess(expectedPixels, bufferedImageAccess);
    }

    private void assertArrayEqualsShortAccess(short[] expectedArray, ShortAccess access) {
        for (int i=0; i<expectedArray.length; i++) {
            Assertions.assertEquals(expectedArray[i], access.getValue(i));
        }
    }
}
