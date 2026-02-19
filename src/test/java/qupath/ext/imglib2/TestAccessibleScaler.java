package qupath.ext.imglib2;

import net.imglib2.Interval;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.real.DoubleType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestAccessibleScaler {

    @Test
    void Check_Min_Different_From_Zero() {
        double scale = 2;
        RandomAccessibleInterval<DoubleType> accessible = new SampleDoubleAccessible(new long[] {0, 3, 0});

        Assertions.assertThrows(IllegalArgumentException.class, () -> AccessibleScaler.scaleWithLinearInterpolation(accessible, scale));
    }

    @Test
    void Check_Negative_Scale() {
        double scale = -2;
        RandomAccessibleInterval<DoubleType> accessible = new SampleDoubleAccessible();

        Assertions.assertThrows(IllegalArgumentException.class, () -> AccessibleScaler.scaleWithLinearInterpolation(accessible, scale));
    }

    @Test
    void Check_Less_Than_Two_Dimensions() {
        double scale = 2;
        RandomAccessibleInterval<DoubleType> accessible = new SampleDoubleAccessible(new long[] {0});

        Assertions.assertThrows(IllegalArgumentException.class, () -> AccessibleScaler.scaleWithLinearInterpolation(accessible, scale));
    }

    @Test
    void Check_Linear_Interpolation_Scale_With_2_by_4_Double_Array() {
        RandomAccessibleInterval<DoubleType> accessible = new SampleDoubleAccessible(new double[][] {
                new double[] {0.80, 0.97, 0.40, 0.99},
                new double[] {0.95, 0.22, 0.63, 0.25}
        });
        double scale = 0.5;
        double[][] expectedPixels = new double[][] {
                new double[] {(0.80 + 0.97 + 0.95 + 0.22) / 4, (0.40 + 0.99 + 0.63 + 0.25) / 4}
        };

        RandomAccessibleInterval<DoubleType> scaledAccessible = AccessibleScaler.scaleWithLinearInterpolation(accessible, scale);

        Utils.assertRandomAccessibleEquals(scaledAccessible, expectedPixels);
    }

    @Test
    void Check_Linear_Interpolation_Scale_With_1_by_3_Double_Array() {
        RandomAccessibleInterval<DoubleType> accessible = new SampleDoubleAccessible(new double[][] {
                new double[] {0.26, 0.66, 0.34}
        });
        double scale = 0.5;
        double[][] expectedPixels = new double[][] {
                new double[] {(0.26 + 0.66) / 2}
        };

        RandomAccessibleInterval<DoubleType> scaledAccessible = AccessibleScaler.scaleWithLinearInterpolation(accessible, scale);

        Utils.assertRandomAccessibleEquals(scaledAccessible, expectedPixels);
    }

    @Test
    void Check_Linear_Interpolation_Interpolation_Scale_With_5_by_5_Double_Array() {
        RandomAccessibleInterval<DoubleType> accessible = new SampleDoubleAccessible(new double[][] {
                new double[] {0.80, 0.72, 0.48, 0.27, 0.68},
                new double[] {0.41, 0.21, 0.60, 0.47, 0.86},
                new double[] {0.94, 0.32, 0.55, 0.22, 0.46},
                new double[] {0.43, 0.83, 0.49, 0.67, 0.42},
                new double[] {0.49, 0.75, 0.85, 0.46, 0.89},
        });
        double scale = 0.5;
        double[][] expectedPixels = new double[][] {
                new double[] {(0.80 + 0.72 + 0.41 + 0.21) / 4, (0.48 + 0.27 + 0.60 + 0.47) / 4},
                new double[] {(0.94 + 0.32 + 0.43 + 0.83) / 4, (0.55 + 0.22 + 0.49 + 0.67) / 4}
        };

        RandomAccessibleInterval<DoubleType> scaledAccessible = AccessibleScaler.scaleWithLinearInterpolation(accessible, scale);

        Utils.assertRandomAccessibleEquals(scaledAccessible, expectedPixels);
    }

    @Test
    void Check_Linear_Interpolation_Scale_With_2_by_4_Argb_Array() {
        RandomAccessibleInterval<ARGBType> accessible = new SampleArgbAccessible(new int[][] {
                new int[] {ARGBType.rgba(82, 79, 61, 46), ARGBType.rgba(67, 94, 89, 62), ARGBType.rgba(13, 33, 3, 42), ARGBType.rgba(92, 41, 35, 54)},
                new int[] {ARGBType.rgba(32, 64, 90, 99), ARGBType.rgba(58, 55, 73, 84), ARGBType.rgba(81, 63, 45, 11), ARGBType.rgba(22, 24, 37, 34)}
        });
        double scale = 0.5;
        int[][] expectedPixels = new int[][] {
                new int[] {
                        ARGBType.rgba(Math.round((82 + 67 + 32 + 58) / 4.), Math.round((79 + 94 + 64 + 55) / 4.), Math.round((61 + 89 + 90 + 73) / 4.), Math.round((46 + 62 + 99 + 84) / 4.)),
                        ARGBType.rgba(Math.round((13 + 92 + 22 + 81) / 4.), Math.round((33 + 41 + 24 + 63) / 4.), Math.round((3 + 35 + 37 + 45) / 4.), Math.round((42 + 54 + 34 + 11) / 4.))
                }
        };

        RandomAccessibleInterval<ARGBType> scaledAccessible = AccessibleScaler.scaleWithLinearInterpolation(accessible, scale);

        Utils.assertArgbRandomAccessibleEquals(scaledAccessible, expectedPixels);
    }

    @Test
    void Check_Nearest_Neighbor_Interpolation_Scale_With_2_by_4_Array() {
        RandomAccessibleInterval<DoubleType> accessible = new SampleDoubleAccessible(new double[][] {
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
        RandomAccessibleInterval<DoubleType> accessible = new SampleDoubleAccessible(new double[][] {
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
        RandomAccessibleInterval<DoubleType> accessible = new SampleDoubleAccessible(new double[][] {
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

    private static class SampleDoubleAccessible implements RandomAccessibleInterval<DoubleType> {

        private final long[] min;
        private final long[] max;
        private final double[][] values;

        public SampleDoubleAccessible() {
            this(new long[] {0, 0});
        }

        public SampleDoubleAccessible(double[][] values) {
            this(new long[] {0, 0}, values);
        }

        public SampleDoubleAccessible(long[] min) {
            this(min, new double[][] {
                    new double[] {}
            });
        }

        private SampleDoubleAccessible(long[] min, double[][] values) {
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
            return new SampleDoubleAccess(values);
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

    private static class SampleDoubleAccess extends Point implements RandomAccess<DoubleType> {

        private final DoubleType value = new DoubleType();
        private final double[][] values;

        public SampleDoubleAccess(double[][] values) {
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
            return new SampleDoubleAccess(values);
        }
    }

    private static class SampleArgbAccessible implements RandomAccessibleInterval<ARGBType> {

        private final long[] min = new long[] {0, 0};
        private final long[] max;
        private final int[][] values;

        public SampleArgbAccessible(int[][] values) {
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
        public RandomAccess<ARGBType> randomAccess() {
            return new SampleArgbAccess(values);
        }

        @Override
        public RandomAccess<ARGBType> randomAccess(Interval interval) {
            return randomAccess();
        }

        @Override
        public int numDimensions() {
            return min.length;
        }
    }

    private static class SampleArgbAccess extends Point implements RandomAccess<ARGBType> {

        private final ARGBType value = new ARGBType();
        private final int[][] values;

        public SampleArgbAccess(int[][] values) {
            super(2);

            this.values = values;
        }

        @Override
        public ARGBType get() {
            value.set(values[(int) position[0]][(int) position[1]]);
            return value;
        }

        @Override
        public RandomAccess<ARGBType> copy() {
            return new SampleArgbAccess(values);
        }
    }
}
