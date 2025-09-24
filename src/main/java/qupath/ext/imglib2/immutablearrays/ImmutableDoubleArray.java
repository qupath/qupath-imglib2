package qupath.ext.imglib2.immutablearrays;

import net.imglib2.img.basictypeaccess.array.DoubleArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link DoubleArray} whose content cannot be modified after initialization.
 * <p>
 * More precisely, {@link #setValue(int, double)} will throw an {@link UnsupportedOperationException}
 * and {@link #getCurrentStorageArray()} returns a copy of the internal array.
 */
public class ImmutableDoubleArray extends DoubleArray {

    private static final Logger logger = LoggerFactory.getLogger(ImmutableDoubleArray.class);

    /**
     * Create an instance of this class containing the provided array.
     *
     * @param data the array to store
     */
    public ImmutableDoubleArray(double[] data) {
        super(data);
    }

    @Override
    public void setValue(int index, double value) {
        throw new UnsupportedOperationException("This array is not mutable");
    }

    @Override
    public double[] getCurrentStorageArray() {
        logger.warn("getCurrentStorageArray() called. Returning defensive copy of the array");
        return data.clone();
    }
}
