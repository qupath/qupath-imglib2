package qupath.ext.imglib2.accesses;

import net.imglib2.img.basictypeaccess.DoubleAccess;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import qupath.ext.imglib2.Utils;

import java.awt.image.DataBuffer;
import java.awt.image.DataBufferDouble;
import java.awt.image.Raster;

public class TestDoubleRasterAccess {

    @Test
    void Check_Pixels() {
        int width = 3;
        int height = 2;
        int nChannels = 2;
        double[][] pixels = new double[][] {
                new double[] {
                        -3.4, 6, 8,
                        4, 55.6, 7
                },
                new double[] {
                        34, -4.56, 0,
                        65, 7, -7.9
                },
        };
        double[] expectedPixels = new double[] {
                -3.4, 6, 8,
                4, 55.6, 7,

                34, -4.56, 0,
                65, 7, -7.9
        };
        DataBuffer dataBuffer = new DataBufferDouble(pixels, nChannels);
        Raster raster = Utils.createRaster(dataBuffer, width, height, nChannels);

        DoubleRasterAccess bufferedImageAccess = new DoubleRasterAccess(raster);

        assertArrayEqualsDoubleAccess(expectedPixels, bufferedImageAccess);
    }

    private void assertArrayEqualsDoubleAccess(double[] expectedArray, DoubleAccess access) {
        for (int i=0; i<expectedArray.length; i++) {
            Assertions.assertEquals(expectedArray[i], access.getValue(i));
        }
    }
}
