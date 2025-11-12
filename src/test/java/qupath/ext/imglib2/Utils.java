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

    public static BufferedImage createArgbBufferedImage(int width, int height, int[] argb) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, width, height, argb, 0, width);
        return image;
    }

    public static <T extends NativeType<T> & RealType<T>> Img<T> createImg(long[] dimensions, double[] pixels, T type) {
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

    public static Img<ARGBType> createArgbImg(long[] dimensions, int[] pixels) {
        Img<ARGBType> img = new ArrayImgFactory<>(new ARGBType()).create(dimensions);

        Cursor<ARGBType> cursor = img.localizingCursor();
        int[] position = new int[dimensions.length];
        while (cursor.hasNext()) {
            ARGBType value = cursor.next();

            cursor.localize(position);
            int index = Math.toIntExact(
                    position[ImgCreator.AXIS_X] +
                            position[ImgCreator.AXIS_Y] * dimensions[ImgCreator.AXIS_X] +
                            position[ImgCreator.AXIS_CHANNEL] * dimensions[ImgCreator.AXIS_X] * dimensions[ImgCreator.AXIS_Y] +
                            position[ImgCreator.AXIS_Z] * dimensions[ImgCreator.AXIS_X] * dimensions[ImgCreator.AXIS_Y] * dimensions[ImgCreator.AXIS_CHANNEL] +
                            position[ImgCreator.AXIS_TIME] * dimensions[ImgCreator.AXIS_X] * dimensions[ImgCreator.AXIS_Y] * dimensions[ImgCreator.AXIS_CHANNEL] * dimensions[ImgCreator.AXIS_Z]
            );

            value.set(pixels[index]);
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

        if (expectedImage.getType() == BufferedImage.TYPE_INT_ARGB && actualImage.getType() == BufferedImage.TYPE_INT_ARGB) {
            int[] expectedRgb = expectedImage.getRGB(
                    0,
                    0,
                    expectedImage.getWidth(),
                    expectedImage.getHeight(),
                    null,
                    0,
                    expectedImage.getWidth()
            );
            int[] actualRgb = actualImage.getRGB(
                    0,
                    0,
                    actualImage.getWidth(),
                    actualImage.getHeight(),
                    null,
                    0,
                    actualImage.getWidth()
            );

            for (int i=0; i<expectedRgb.length; i++) {
                // some mismatch may occur due to interpolation in AbstractTileableImageServer, hence the delta of 1
                Assertions.assertEquals(ARGBType.alpha(expectedRgb[i]), ARGBType.alpha(actualRgb[i]));
                Assertions.assertEquals(ARGBType.red(expectedRgb[i]), ARGBType.red(actualRgb[i]), 1);
                Assertions.assertEquals(ARGBType.green(expectedRgb[i]), ARGBType.green(actualRgb[i]));
                Assertions.assertEquals(ARGBType.blue(expectedRgb[i]), ARGBType.blue(actualRgb[i]));
            }
        } else {
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
}
