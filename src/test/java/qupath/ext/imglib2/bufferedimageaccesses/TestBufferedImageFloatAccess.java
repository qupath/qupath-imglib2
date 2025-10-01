package qupath.ext.imglib2.bufferedimageaccesses;

import net.imglib2.img.basictypeaccess.FloatAccess;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import qupath.ext.imglib2.Utils;
import qupath.lib.images.servers.PixelType;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferFloat;

public class TestBufferedImageFloatAccess {

    @Test
    void Check_Pixels() {
        int width = 3;
        int height = 2;
        int nChannels = 2;
        float[][] pixels = new float[][] {
                new float[] {
                        3.4f, -6, 8,
                        -4, 55.6f, 7
                },
                new float[] {
                        34, 4.56f, 0,
                        -65, 7, 7.9f
                },
        };
        float[] expectedPixels = new float[] {
                3.4f, -6, 8,
                -4, 55.6f, 7,

                34, 4.56f, 0,
                -65, 7, 7.9f
        };
        DataBuffer dataBuffer = new DataBufferFloat(pixels, nChannels);
        BufferedImage image = Utils.createBufferedImage(dataBuffer, width, height, nChannels, PixelType.FLOAT32);

        BufferedImageFloatAccess bufferedImageAccess = new BufferedImageFloatAccess(image);

        assertArrayEqualsFloatAccess(expectedPixels, bufferedImageAccess);
    }

    private void assertArrayEqualsFloatAccess(float[] expectedArray, FloatAccess access) {
        for (int i=0; i<expectedArray.length; i++) {
            Assertions.assertEquals(expectedArray[i], access.getValue(i));
        }
    }
}
