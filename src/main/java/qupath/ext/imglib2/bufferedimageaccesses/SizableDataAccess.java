package qupath.ext.imglib2.bufferedimageaccesses;

import net.imglib2.img.basictypeaccess.DataAccess;

/**
 * A {@link DataAccess} whose size can be computed.
 */
public interface SizableDataAccess extends DataAccess {

    /**
     * Get the size of this {@link DataAccess} in bytes.
     *
     * @return the size of this {@link DataAccess} in bytes
     */
    int getSizeBytes();
}
