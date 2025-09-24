package qupath.ext.imglib2;

import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.realtransform.RealViews;
import net.imglib2.realtransform.Scale2D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.view.Views;

import java.util.Arrays;
import java.util.stream.LongStream;

/**
 * A collection of static functions to scale {@link RandomAccessibleInterval}.
 */
public class AccessibleScaler {

    private AccessibleScaler() {
        throw new AssertionError("This class is not instantiable.");
    }

    /**
     * Scale the two first dimensions of the provided {@link RandomAccessibleInterval} using a linear interpolation.
     *
     * @param input the input {@link RandomAccessibleInterval} to scale. Its {@link RandomAccessibleInterval#min(long[]) min indexes}
     *              should be 0 on all dimensions, and it should have at least two dimensions
     * @param scale the scale to apply to the first two dimensions of the input {@link RandomAccessibleInterval}. Shouldn't be
     *              less than or equal to 0
     * @return the input if the provided scale is 1, or a new scaled {@link RandomAccessibleInterval} otherwise
     * @param <T> the type of elements of the random accessible interval
     * @throws IllegalArgumentException if the input interval has at least one minimum different from 0, if the provided scale is less
     * than or equal to 0, or if the input interval has less than two dimensions
     */
    public static <T extends NativeType<T> & NumericType<T>> RandomAccessibleInterval<T> scaleWithLinearInterpolation(
            RandomAccessibleInterval<T> input,
            double scale
    ) {
        return scale(input, scale, new NLinearInterpolatorFactory<>());
    }

    /**
     * Scale the two first dimensions of the provided {@link RandomAccessibleInterval} using the nearest neighbor interpolation.
     *
     * @param input the input {@link RandomAccessibleInterval} to scale. Its {@link RandomAccessibleInterval#min(long[]) min indexes}
     *              should be 0 on all dimensions, and it should have at least two dimensions
     * @param scale the scale to apply to the first two dimensions of the input {@link RandomAccessibleInterval}. Shouldn't be
     *              less than or equal to 0
     * @return the input if the provided scale is 1, or a new scaled {@link RandomAccessibleInterval} otherwise
     * @param <T> the type of elements of the random accessible interval
     * @throws IllegalArgumentException if the input interval has at least one minimum different from 0, if the provided scale is less
     * than or equal to 0, or if the input interval has less than two dimensions
     */
    public static <T extends NativeType<T> & NumericType<T>> RandomAccessibleInterval<T> scaleWithNearestNeighborInterpolation(
            RandomAccessibleInterval<T> input,
            double scale
    ) {
        return scale(input, scale, new NearestNeighborInterpolatorFactory<>());
    }

    /**
     * Scale the two first dimensions of the provided {@link RandomAccessibleInterval} using the provided interpolation.
     *
     * @param input the input {@link RandomAccessibleInterval} to scale. Its {@link RandomAccessibleInterval#min(long[]) min indexes}
     *              should be 0 on all dimensions, and it should have at least two dimensions
     * @param scale the scale to apply to the first two dimensions of the input {@link RandomAccessibleInterval}. Shouldn't be
     *              less than or equal to 0
     * @param interpolatorFactory the interpolation to use
     * @return the input if the provided scale is 1, or a new scaled {@link RandomAccessibleInterval} otherwise
     * @param <T> the type of elements of the random accessible interval
     * @throws IllegalArgumentException if the input interval has at least one minimum different from 0, if the provided scale is less
     * than or equal to 0, or if the input interval has less than two dimensions
     */
    public static <T extends NativeType<T> & NumericType<T>> RandomAccessibleInterval<T> scale(
            RandomAccessibleInterval<T> input,
            double scale,
            InterpolatorFactory<T, RandomAccessible<T>> interpolatorFactory
    ) {
        if (Arrays.stream(input.minAsLongArray()).anyMatch(min -> min != 0)) {
            throw new IllegalArgumentException(String.format(
                    "The input interval has a minimum different from 0 on at least one dimension (%s)",
                    Arrays.toString(input.minAsLongArray())
            ));
        }
        if (scale <= 0) {
            throw new IllegalArgumentException(String.format("The provided scale %f is less than or equal to 0", scale));
        }
        if (input.numDimensions() < 2) {
            throw new IllegalArgumentException(String.format("The provided accessible has less than 2 dimensions (%d)", input.numDimensions()));
        }

        if (scale == 1) {
            return input;
        } else {
            return scaleWithoutChecks(input, scale, interpolatorFactory);
        }
    }

    private static <T extends NativeType<T> & NumericType<T>> RandomAccessibleInterval<T> scaleWithoutChecks(
            RandomAccessibleInterval<T> input,
            double scale,
            InterpolatorFactory<T, RandomAccessible<T>> interpolatorFactory
    ) {
        // Directly applying the scale on a multidimensional image involves too many useless computation
        // Instead, the multidimensional image is decomposed into 2D images, the scale is applied to the 2D images, and the 2D images are stacked back
        // to form a multidimensional image

        int numDimensions = input.numDimensions();

        if (numDimensions == 2) {
            long[] max = input.maxAsLongArray();
            max[0] = (int) ((max[0] + 1) * scale - 1);
            max[1] = (int) ((max[1] + 1) * scale - 1);

            return Views.interval(
                    Views.raster(
                            RealViews.affine(
                                    Views.interpolate(
                                            Views.extendMirrorDouble(input),
                                            interpolatorFactory
                                    ),
                                    new Scale2D(scale, scale)
                            )
                    ),
                    new long[] {0, 0},
                    max
            );
        } else {
            int dimensionToDivide = numDimensions - 1;

            return Views.stack(LongStream.range(0, input.dimension(dimensionToDivide))
                    .mapToObj(i -> Views.hyperSlice(input, dimensionToDivide, i))
                    .map(view -> scaleWithLinearInterpolation(view, scale))
                    .toList()
            );
        }
    }
}
