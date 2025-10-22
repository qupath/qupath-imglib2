package qupath.ext.imglib2;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import org.junit.jupiter.api.Assertions;
import qupath.lib.color.ColorModelFactory;
import qupath.lib.images.servers.ImageChannel;
import qupath.lib.images.servers.PixelType;

import java.awt.image.BandedSampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;

public class Utils {

    @FunctionalInterface
    public interface PixelGetter {
        double get(int x, int y, int channel, int z, int t);
    }

    public static WritableRaster createRaster(DataBuffer dataBuffer, int width, int height, int nChannels) {
        return WritableRaster.createWritableRaster(
                new BandedSampleModel(
                        dataBuffer.getDataType(),
                        width,
                        height,
                        nChannels
                ),
                dataBuffer,
                null
        );
    }

    public static BufferedImage createBufferedImage(DataBuffer dataBuffer, int width, int height, int nChannels, PixelType pixelType) {
        return new BufferedImage(
                ColorModelFactory.createColorModel(pixelType, ImageChannel.getDefaultChannelList(nChannels)),
                createRaster(dataBuffer, width, height, nChannels),
                false,
                null
        );
    }

    public static <T extends RealType<T>> void assertRandomAccessibleEquals(RandomAccessibleInterval<T> accessible, PixelGetter pixelGetter, double downsample) {
        int[] position = new int[accessible.numDimensions()];
        Cursor<T> cursor = accessible.localizingCursor();

        while (cursor.hasNext()) {
            T pixel = cursor.next();
            cursor.localize(position);

            Assertions.assertEquals(
                    pixelGetter.get(
                            (int) (position[ImgCreator.X_DIMENSION] * downsample),
                            (int) (position[ImgCreator.Y_DIMENSION] * downsample),
                            position[ImgCreator.CHANNEL_DIMENSION],
                            position[ImgCreator.Z_DIMENSION],
                            position[ImgCreator.TIME_DIMENSION]
                    ),
                    pixel.getRealDouble()
            );
        }
    }

    public static void assertArgbRandomAccessibleEquals(RandomAccessibleInterval<ARGBType> accessible, PixelGetter pixelGetter, double downsample) {
        int[] position = new int[accessible.numDimensions()];
        Cursor<ARGBType> cursor = accessible.localizingCursor();

        while (cursor.hasNext()) {
            ARGBType pixel = cursor.next();
            cursor.localize(position);

            Assertions.assertEquals(
                    pixelGetter.get(
                            (int) (position[ImgCreator.X_DIMENSION] * downsample),
                            (int) (position[ImgCreator.Y_DIMENSION] * downsample),
                            position[ImgCreator.CHANNEL_DIMENSION],
                            position[ImgCreator.Z_DIMENSION],
                            position[ImgCreator.TIME_DIMENSION]
                    ),
                    pixel.get()
            );
        }
    }

    public static <T extends RealType<T>> void assertRandomAccessibleEquals(RandomAccessibleInterval<T> accessible, double[][] expectedPixels) {
        Assertions.assertEquals(expectedPixels.length, accessible.dimension(0));
        Assertions.assertEquals(expectedPixels[0].length, accessible.dimension(1));

        int[] position = new int[accessible.numDimensions()];
        Cursor<T> cursor = accessible.localizingCursor();

        while (cursor.hasNext()) {
            T pixel = cursor.next();
            cursor.localize(position);
            int x = position[1];
            int y = position[0];

            Assertions.assertEquals(
                    expectedPixels[y][x],
                    pixel.getRealDouble(),
                    0.0000000001        // to avoid rounding errors
            );
        }
    }
}
