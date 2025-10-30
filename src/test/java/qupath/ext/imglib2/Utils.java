package qupath.ext.imglib2;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import org.junit.jupiter.api.Assertions;
import qupath.lib.color.ColorModelFactory;
import qupath.lib.images.servers.ImageChannel;
import qupath.lib.images.servers.PixelType;

import java.awt.image.BandedSampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
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

    public static <T extends RealType<T> & NativeType<T>> Img<T> createImg(long[] dimensions, double[] pixels, T type) {
        Img<T> img = new ArrayImgFactory<>(type).create(dimensions);

        Cursor<T> cursor = img.localizingCursor();
        int[] position = new int[dimensions.length];
        while (cursor.hasNext()) {
            T value = cursor.next();

            cursor.localize(position);
            int index = Math.toIntExact(
                    position[ImgCreator.AXIS_X] +
                            position[ImgCreator.AXIS_Y] * dimensions[ImgCreator.AXIS_X] +
                            position[ImgCreator.AXIS_CHANNEL] * dimensions[ImgCreator.AXIS_X] * dimensions[ImgCreator.AXIS_Y] +
                            position[ImgCreator.AXIS_Z] * dimensions[ImgCreator.AXIS_X] * dimensions[ImgCreator.AXIS_Y] * dimensions[ImgCreator.AXIS_CHANNEL] +
                            position[ImgCreator.AXIS_TIME] * dimensions[ImgCreator.AXIS_X] * dimensions[ImgCreator.AXIS_Y] * dimensions[ImgCreator.AXIS_CHANNEL] * dimensions[ImgCreator.AXIS_Z]
            );

            value.setReal(pixels[index]);
        }

        return img;
    }

    public static <T extends RealType<T>> void assertRandomAccessibleEquals(RandomAccessibleInterval<T> accessible, PixelGetter pixelGetter, double downsample) {
        int[] position = new int[accessible.numDimensions()];
        Cursor<T> cursor = accessible.localizingCursor();

        while (cursor.hasNext()) {
            T pixel = cursor.next();
            cursor.localize(position);

            Assertions.assertEquals(
                    pixelGetter.get(
                            (int) (position[ImgCreator.AXIS_X] * downsample),
                            (int) (position[ImgCreator.AXIS_Y] * downsample),
                            position[ImgCreator.AXIS_CHANNEL],
                            position[ImgCreator.AXIS_Z],
                            position[ImgCreator.AXIS_TIME]
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
                            (int) (position[ImgCreator.AXIS_X] * downsample),
                            (int) (position[ImgCreator.AXIS_Y] * downsample),
                            position[ImgCreator.AXIS_CHANNEL],
                            position[ImgCreator.AXIS_Z],
                            position[ImgCreator.AXIS_TIME]
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

    public static void assertBufferedImagesEqual(BufferedImage expectedImage, BufferedImage actualImage, double delta) {
        Assertions.assertEquals(expectedImage.getWidth(), actualImage.getWidth());
        Assertions.assertEquals(expectedImage.getHeight(), actualImage.getHeight());
        Assertions.assertEquals(expectedImage.getRaster().getNumBands(), actualImage.getRaster().getNumBands());

        for (int x = 0; x < expectedImage.getWidth(); x++) {
            for (int y = 0; y < expectedImage.getHeight(); y++) {
                for (int b=0; b< expectedImage.getRaster().getNumBands(); b++) {
                    Assertions.assertEquals(
                            expectedImage.getRaster().getSampleDouble(x, y, b),
                            actualImage.getRaster().getSampleDouble(x, y, b),
                            delta
                    );
                }
            }
        }
    }
}
