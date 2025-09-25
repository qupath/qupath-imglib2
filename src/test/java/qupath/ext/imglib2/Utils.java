package qupath.ext.imglib2;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import org.junit.jupiter.api.Assertions;

public class Utils {

    @FunctionalInterface
    public interface PixelGetter {
        double get(int x, int y, int channel, int z, int t);
    }

    public static <T extends RealType<T>> void assertRandomAccessibleEquals(RandomAccessibleInterval<T> accessible, PixelGetter pixelGetter, double downsample) {
        int[] position = new int[accessible.numDimensions()];
        Cursor<T> cursor = accessible.localizingCursor();

        while (cursor.hasNext()) {
            T pixel = cursor.next();
            cursor.localize(position);

            Assertions.assertEquals(
                    pixelGetter.get(
                            (int) (position[ImgCreator.getIndexOfDimension(Dimension.X)] * downsample),
                            (int) (position[ImgCreator.getIndexOfDimension(Dimension.Y)] * downsample),
                            position[ImgCreator.getIndexOfDimension(Dimension.CHANNEL)],
                            position[ImgCreator.getIndexOfDimension(Dimension.Z)],
                            position[ImgCreator.getIndexOfDimension(Dimension.TIME)]
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
                            (int) (position[ImgCreator.getIndexOfDimension(Dimension.X)] * downsample),
                            (int) (position[ImgCreator.getIndexOfDimension(Dimension.Y)] * downsample),
                            position[ImgCreator.getIndexOfDimension(Dimension.CHANNEL)],
                            position[ImgCreator.getIndexOfDimension(Dimension.Z)],
                            position[ImgCreator.getIndexOfDimension(Dimension.TIME)]
                    ),
                    pixel.get()
            );
        }
    }

    public static <T extends RealType<T>> void assertRandomAccessibleEquals(RandomAccessibleInterval<T> accessible, double[] expectedPixels) {
        Assertions.assertEquals(expectedPixels.length, accessible.size());

        int[] position = new int[accessible.numDimensions()];
        int width = Math.toIntExact(accessible.dimension(ImgCreator.getIndexOfDimension(Dimension.X)));
        Cursor<T> cursor = accessible.localizingCursor();

        while (cursor.hasNext()) {
            T pixel = cursor.next();
            cursor.localize(position);
            int x = position[ImgCreator.getIndexOfDimension(Dimension.X)];
            int y = position[ImgCreator.getIndexOfDimension(Dimension.Y)];

            Assertions.assertEquals(
                    expectedPixels[x + width * y],
                    pixel.getRealDouble()
            );
        }
    }
}
