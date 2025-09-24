package qupath.ext.imglib2.immutablearrays;

import net.imglib2.img.basictypeaccess.array.ShortArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link ShortArray} whose content cannot be modified after initialization.
 * <p>
 * More precisely, {@link #setValue(int, short)} will throw an {@link UnsupportedOperationException}
 * and {@link #getCurrentStorageArray()} returns a copy of the internal array.
 */
public class ImmutableShortArray extends ShortArray {

    private static final Logger logger = LoggerFactory.getLogger(ImmutableShortArray.class);

    /**
     * Create an instance of this class containing the provided array.
     *
     * @param data the array to store
     */
    public ImmutableShortArray(short[] data) {
        super(data);
    }

    @Override
    public void setValue(int index, short value) {
        throw new UnsupportedOperationException("This array is not mutable");
    }

    @Override
    public short[] getCurrentStorageArray() {
        logger.warn("getCurrentStorageArray() called. Returning defensive copy of the array");
        return super.getCurrentStorageArray().clone();      // TODO: data cannot be directly used because of https://github.com/imglib/imglib2/pull/384
                                                            // TODO: when this PR is merged, data should be used instead of super.getCurrentStorageArray()
    }
}
