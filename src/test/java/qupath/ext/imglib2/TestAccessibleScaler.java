package qupath.ext.imglib2;

import net.imglib2.Interval;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.DoubleType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestAccessibleScaler {

    @Test
    void Check_Min_Different_From_Zero() {
        double scale = 2;
        RandomAccessibleInterval<DoubleType> accessible = new SampleAccessible(new long[] {0, 3, 0});

        Assertions.assertThrows(IllegalArgumentException.class, () -> AccessibleScaler.scaleWithLinearInterpolation(accessible, scale));
    }

    @Test
    void Check_Negative_Scale() {
        double scale = -2;
        RandomAccessibleInterval<DoubleType> accessible = new SampleAccessible();

        Assertions.assertThrows(IllegalArgumentException.class, () -> AccessibleScaler.scaleWithLinearInterpolation(accessible, scale));
    }

    @Test
    void Check_Less_Than_Two_Dimensions() {
        double scale = 2;
        RandomAccessibleInterval<DoubleType> accessible = new SampleAccessible(new long[] {0});

        Assertions.assertThrows(IllegalArgumentException.class, () -> AccessibleScaler.scaleWithLinearInterpolation(accessible, scale));
    }

    @Test
    void Check_Linear_Interpolation_Scale() {
        RandomAccessibleInterval<DoubleType> accessible = new SampleAccessible();
        double scale = 0.5;
        double[] expectedPixels = new double[] {
                0.47, 0.45, 0.57,
                0.65, 0.67, 0.55
        };

        RandomAccessibleInterval<DoubleType> scaledAccessible = AccessibleScaler.scaleWithLinearInterpolation(accessible, scale);

        Utils.assertRandomAccessibleEquals(scaledAccessible, expectedPixels);
    }

    @Test
    void Check_Nearest_Neighbor_Interpolation_Scale() {
        RandomAccessibleInterval<DoubleType> accessible = new SampleAccessible();
        double scale = 0.5;
        double[] expectedPixels = new double[] {
                0.47, 0.45, 0.57,
                0.65, 0.67, 0.55
        };

        RandomAccessibleInterval<DoubleType> scaledAccessible = AccessibleScaler.scaleWithNearestNeighborInterpolation(accessible, scale);

        Utils.assertRandomAccessibleEquals(scaledAccessible, expectedPixels);
    }

    private static class SampleAccessible implements RandomAccessibleInterval<DoubleType> {

        private static final double[][] values = new double[][] {
                new double[] {0.47, 0.17, 0.45, 0.57},
                new double[] {0.65, 0.25, 0.67, 0.55}
        };
        private final long[] max = new long[] {3, 1};
        private final long[] min;

        public SampleAccessible() {
            this(new long[] {0, 0});
        }

        public SampleAccessible(long[] min) {
            this.min = min;
        }

        @Override
        public long min(int d) {
            return min[d];
        }

        @Override
        public long max(int d) {
            return max[d];
        }

        @Override
        public RandomAccess<DoubleType> randomAccess() {
            return new SampleAccess(values);
        }

        @Override
        public RandomAccess<DoubleType> randomAccess(Interval interval) {
            return randomAccess();
        }

        @Override
        public int numDimensions() {
            return min.length;
        }
    }

    private static class SampleAccess extends Point implements RandomAccess<DoubleType> {

        private final DoubleType value = new DoubleType();
        private final double[][] values;

        public SampleAccess(double[][] values) {
            super(2);

            this.values = values;
        }

        @Override
        public DoubleType get() {
            value.setReal(values[(int) position[1]][(int) position[0]]);
            return value;
        }

        @Override
        public RandomAccess<DoubleType> copy() {
            return new SampleAccess(values);
        }
    }
}
