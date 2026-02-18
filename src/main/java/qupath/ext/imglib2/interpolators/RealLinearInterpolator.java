package qupath.ext.imglib2.interpolators;

import net.imglib2.RandomAccessible;
import net.imglib2.interpolation.randomaccess.NLinearInterpolator2D;
import net.imglib2.type.numeric.RealType;

/**
 * A {@link NLinearInterpolator2D} that performs intermediary operations with doubles, to avoid precision errors.
 */
class RealLinearInterpolator<T extends RealType<T>> extends NLinearInterpolator2D<T> {

    /**
     * Create an instance of this class.
     *
     * @param randomAccessible the accessible to interpolate
     */
    public RealLinearInterpolator(RandomAccessible<T> randomAccessible) {
        super(randomAccessible);
    }

    @Override
    public T get()
    {
        fillWeights();

        double value = target.get().getRealDouble() * weights[0];

        target.fwd(0);
        value += target.get().getRealDouble() * weights[1];

        target.fwd(1);
        value += target.get().getRealDouble() * weights[3];

        target.bck(0);
        value += target.get().getRealDouble() * weights[2];

        target.bck(1);

        accumulator.setReal(value);

        return accumulator;
    }
}
