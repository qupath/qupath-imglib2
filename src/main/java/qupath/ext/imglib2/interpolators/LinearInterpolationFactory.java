package qupath.ext.imglib2.interpolators;

import net.imglib2.RandomAccessible;
import net.imglib2.RealInterval;
import net.imglib2.RealRandomAccess;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;

/**
 * An {@link InterpolatorFactory} that implements a 2D linear interpolation.
 * <p>
 * This factory can only work with {@link RandomAccessible} that have 2 dimensions and the {@link ARGBType} or {@link RealType}
 * type. If {@link #create(RandomAccessible)} is called with an invalid {@link RandomAccessible}, an {@link IllegalArgumentException}
 * is thrown.
 *
 * @param <T> the type of {@link RandomAccessible} to interpolate
 */
public class LinearInterpolationFactory<T extends NumericType<T>> implements InterpolatorFactory<T, RandomAccessible<T>> {

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public RealRandomAccess<T> create(RandomAccessible randomAccessible) {
        if (randomAccessible.numDimensions() != 2) {
            throw new IllegalArgumentException(String.format(
                    "The provided accessible has not 2 dimensions (found %d)",
                    randomAccessible.numDimensions()
            ));
        }

        Object type = randomAccessible.randomAccess().get();
        if (type instanceof ARGBType) {
            return (RealRandomAccess<T>) new ArgbLinearInterpolator(randomAccessible);
        } else if (type instanceof RealType<?>) {
            return (RealRandomAccess<T>) new RealLinearInterpolator<>(randomAccessible);
        } else {
            throw new IllegalArgumentException(String.format(
                    "The type of the provided accessible %s is unexpected",
                    type
            ));
        }
    }

    @Override
    public RealRandomAccess<T> create(RandomAccessible<T> randomAccessible, RealInterval interval) {
        return create(randomAccessible);
    }
}
