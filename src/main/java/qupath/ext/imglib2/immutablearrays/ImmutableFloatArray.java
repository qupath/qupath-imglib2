package qupath.ext.imglib2.immutablearrays;

import net.imglib2.img.basictypeaccess.array.FloatArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link FloatArray} whose content cannot be modified after initialization.
 * <p>
 * More precisely, {@link #setValue(int, float)} will throw an {@link UnsupportedOperationException}
 * and {@link #getCurrentStorageArray()} returns a copy of the internal array.
 */
public class ImmutableFloatArray extends FloatArray {

    private static final Logger logger = LoggerFactory.getLogger(ImmutableFloatArray.class);

    /**
     * Create an instance of this class containing the provided array.
     *
     * @param data the array to store
     */
    public ImmutableFloatArray(float[] data) {
        super(data);
    }

    @Override
    public void setValue(int index, float value) {
        throw new UnsupportedOperationException("This array is not mutable");
    }

    @Override
    public float[] getCurrentStorageArray() {
        logger.warn("getCurrentStorageArray() called. Returning defensive copy of the array");
        return data.clone();
    }
}
