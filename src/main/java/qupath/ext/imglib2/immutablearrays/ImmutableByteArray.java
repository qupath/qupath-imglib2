package qupath.ext.imglib2.immutablearrays;

import net.imglib2.img.basictypeaccess.array.ByteArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link ByteArray} whose content cannot be modified after initialization.
 * <p>
 * More precisely, {@link #setValue(int, byte)} will throw an {@link UnsupportedOperationException}
 * and {@link #getCurrentStorageArray()} returns a copy of the internal array.
 */
public class ImmutableByteArray extends ByteArray {

    private static final Logger logger = LoggerFactory.getLogger(ImmutableByteArray.class);

    /**
     * Create an instance of this class containing the provided array.
     *
     * @param data the array to store
     */
    public ImmutableByteArray(byte[] data) {
        super(data);
    }

    @Override
    public void setValue(int index, byte value) {
        throw new UnsupportedOperationException("This array is not mutable");
    }

    @Override
    public byte[] getCurrentStorageArray() {
        logger.warn("getCurrentStorageArray() called. Returning defensive copy of the array");
        return data.clone();
    }
}
