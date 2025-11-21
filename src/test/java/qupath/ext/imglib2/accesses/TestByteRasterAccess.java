package qupath.ext.imglib2.accesses;

import net.imglib2.img.basictypeaccess.ByteAccess;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import qupath.ext.imglib2.Utils;

import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;

public class TestByteRasterAccess {

    @Test
    void Check_Pixels() {
        int width = 3;
        int height = 2;
        int nChannels = 2;
        byte[][] pixels = new byte[][] {
                new byte[] {
                        -3, 6, 8,
                        4, 56, 7
                },
                new byte[] {
                        34, 46, 0,
                        65, 7, -90
                },
        };
        byte[] expectedPixels = new byte[] {
                -3, 6, 8,
                4, 56, 7,

                34, 46, 0,
                65, 7, -90
        };
        DataBuffer dataBuffer = new DataBufferByte(pixels, nChannels);
        Raster raster = Utils.createRaster(dataBuffer, width, height, nChannels);

        ByteRasterAccess bufferedImageAccess = new ByteRasterAccess(raster);

        assertArrayEqualsByteAccess(expectedPixels, bufferedImageAccess);
    }

    private void assertArrayEqualsByteAccess(byte[] expectedArray, ByteAccess access) {
        for (int i=0; i<expectedArray.length; i++) {
            Assertions.assertEquals(expectedArray[i], access.getValue(i));
        }
    }
}
