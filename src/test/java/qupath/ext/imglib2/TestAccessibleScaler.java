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
    void Check_Linear_Interpolation_Scale_With_2_by_4_Array() {
        RandomAccessibleInterval<DoubleType> accessible = new SampleAccessible(new double[][] {
                new double[] {0.80, 0.97, 0.40, 0.99},
                new double[] {0.95, 0.22, 0.63, 0.25}
        });
        double scale = 0.5;
        double[][] expectedPixels = new double[][] {
                new double[] {0.735, 0.5675}
        };

        RandomAccessibleInterval<DoubleType> scaledAccessible = AccessibleScaler.scaleWithLinearInterpolation(accessible, scale);

        Utils.assertRandomAccessibleEquals(scaledAccessible, expectedPixels);
    }

    @Test
    void Check_Linear_Interpolation_Scale_With_1_by_3_Array() {
        RandomAccessibleInterval<DoubleType> accessible = new SampleAccessible(new double[][] {
                new double[] {0.26, 0.66, 0.34}
        });
        double scale = 0.5;
        double[][] expectedPixels = new double[][] {
                new double[] {0.46}
        };

        RandomAccessibleInterval<DoubleType> scaledAccessible = AccessibleScaler.scaleWithLinearInterpolation(accessible, scale);

        Utils.assertRandomAccessibleEquals(scaledAccessible, expectedPixels);
    }

    @Test
    void Check_Linear_Interpolation_Interpolation_Scale_With_5_by_5_Array() {
        RandomAccessibleInterval<DoubleType> accessible = new SampleAccessible(new double[][] {
                new double[] {0.80, 0.72, 0.48, 0.27, 0.68},
                new double[] {0.41, 0.21, 0.60, 0.47, 0.86},
                new double[] {0.94, 0.32, 0.55, 0.22, 0.46},
                new double[] {0.43, 0.83, 0.49, 0.67, 0.42},
                new double[] {0.49, 0.75, 0.85, 0.46, 0.89},
        });
        double scale = 0.5;
        double[][] expectedPixels = new double[][] {
                new double[] {0.535, 0.455},
                new double[] {0.63, 0.4825}
        };

        RandomAccessibleInterval<DoubleType> scaledAccessible = AccessibleScaler.scaleWithLinearInterpolation(accessible, scale);

        Utils.assertRandomAccessibleEquals(scaledAccessible, expectedPixels);
    }

    @Test
    void Check_Nearest_Neighbor_Interpolation_Scale_With_2_by_4_Array() {
        RandomAccessibleInterval<DoubleType> accessible = new SampleAccessible(new double[][] {
                new double[] {0.80, 0.97, 0.40, 0.99},
                new double[] {0.95, 0.22, 0.63, 0.25}
        });
        double scale = 0.5;
        double[][] expectedPixels = new double[][] {
                new double[] {0.22, 0.25}
        };

        RandomAccessibleInterval<DoubleType> scaledAccessible = AccessibleScaler.scaleWithNearestNeighborInterpolation(accessible, scale);

        Utils.assertRandomAccessibleEquals(scaledAccessible, expectedPixels);
    }

    @Test
    void Check_Nearest_Neighbor_Interpolation_Scale_With_1_by_3_Array() {
        RandomAccessibleInterval<DoubleType> accessible = new SampleAccessible(new double[][] {
                new double[] {0.26, 0.66, 0.34}
        });
        double scale = 0.5;
        double[][] expectedPixels = new double[][] {
                new double[] {0.66}
        };

        RandomAccessibleInterval<DoubleType> scaledAccessible = AccessibleScaler.scaleWithNearestNeighborInterpolation(accessible, scale);

        Utils.assertRandomAccessibleEquals(scaledAccessible, expectedPixels);
    }

    @Test
    void Check_Nearest_Neighbor_Interpolation_Scale_With_5_by_5_Array() {
        RandomAccessibleInterval<DoubleType> accessible = new SampleAccessible(new double[][] {
                new double[] {0.80, 0.72, 0.48, 0.27, 0.68},
                new double[] {0.41, 0.21, 0.60, 0.47, 0.86},
                new double[] {0.94, 0.32, 0.55, 0.22, 0.46},
                new double[] {0.43, 0.83, 0.49, 0.67, 0.42},
                new double[] {0.49, 0.75, 0.85, 0.46, 0.89},
        });
        double scale = 0.5;
        double[][] expectedPixels = new double[][] {
                new double[] {0.21, 0.47},
                new double[] {0.83, 0.67}
        };

        RandomAccessibleInterval<DoubleType> scaledAccessible = AccessibleScaler.scaleWithNearestNeighborInterpolation(accessible, scale);

        Utils.assertRandomAccessibleEquals(scaledAccessible, expectedPixels);
    }

    private static class SampleAccessible implements RandomAccessibleInterval<DoubleType> {

        private final long[] min;
        private final long[] max;
        private final double[][] values;

        public SampleAccessible() {
            this(new long[] {0, 0});
        }

        public SampleAccessible(double[][] values) {
            this(new long[] {0, 0}, values);
        }

        public SampleAccessible(long[] min) {
            this(min, new double[][] {
                    new double[] {}
            });
        }

        private SampleAccessible(long[] min, double[][] values) {
            this.min = min;
            this.max = new long[] {values.length - 1, values[0].length - 1};
            this.values = values;
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
            value.setReal(values[(int) position[0]][(int) position[1]]);
            return value;
        }

        @Override
        public RandomAccess<DoubleType> copy() {
            return new SampleAccess(values);
        }
    }
}
