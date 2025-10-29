package qupath.ext.imglib2.bufferedimageaccesses;

import net.imglib2.img.basictypeaccess.IntAccess;
import net.imglib2.img.basictypeaccess.volatiles.VolatileAccess;
import qupath.ext.imglib2.SizableDataAccess;

import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;

/**
 * An {@link IntAccess} whose elements are computed from a {@link Raster}.
 * <p>
 * This {@link IntAccess} is immutable; any attempt to changes its values will result in a
 * {@link UnsupportedOperationException}.
 * <p>
 * This data access is marked as volatile but always contain valid data.
 */
public class IntRasterAccess implements IntAccess, SizableDataAccess, VolatileAccess {

    private final Raster raster;
    private final DataBuffer dataBuffer;
    private final int width;
    private final int planeSize;
    private final boolean canUseDataBuffer;
    private final int size;

    /**
     * Create the int raster access.
     *
     * @param raster the raster containing the values to return. Its pixels are expected to be stored in the int format
     * @throws NullPointerException if the provided image is null
     */
    public IntRasterAccess(Raster raster) {
        this.raster = raster;
        this.dataBuffer = this.raster.getDataBuffer();

        this.width = this.raster.getWidth();
        this.planeSize = width * this.raster.getHeight();

        this.canUseDataBuffer = this.dataBuffer instanceof DataBufferInt &&
                AccessTools.isSampleModelDirectlyUsable(this.raster);

        this.size = AccessTools.getSizeOfDataBufferInBytes(this.dataBuffer);
    }

    @Override
    public int getValue(int index) {
        int b = index / planeSize;
        int xyIndex = index % planeSize;

        if (canUseDataBuffer) {
            return dataBuffer.getElem(b, xyIndex);
        } else {
            return raster.getSample(xyIndex % width, xyIndex / width, b);
        }
    }

    @Override
    public void setValue(int index, int value) {
        throw new UnsupportedOperationException("This access is not mutable");
    }

    @Override
    public int getSizeBytes() {
        return size;
    }

    @Override
    public boolean isValid() {
        return true;
    }
}
