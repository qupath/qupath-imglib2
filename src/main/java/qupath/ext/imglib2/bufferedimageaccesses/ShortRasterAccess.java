package qupath.ext.imglib2.bufferedimageaccesses;

import net.imglib2.img.basictypeaccess.ShortAccess;
import net.imglib2.img.basictypeaccess.volatiles.VolatileAccess;
import qupath.ext.imglib2.SizableDataAccess;

import java.awt.image.DataBuffer;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;

/**
 * A {@link ShortAccess} whose elements are computed from a {@link Raster}.
 * <p>
 * This {@link ShortAccess} is immutable; any attempt to changes its values will result in a
 * {@link UnsupportedOperationException}.
 * <p>
 * This data access is marked as volatile but always contain valid data.
 */
public class ShortRasterAccess implements ShortAccess, SizableDataAccess, VolatileAccess {

    private final Raster raster;
    private final DataBuffer dataBuffer;
    private final int width;
    private final int planeSize;
    private final boolean canUseDataBuffer;
    private final int size;

    /**
     * Create the short raster access.
     *
     * @param raster the raster containing the values to return. Its pixels are expected to be stored in the short format
     * @throws NullPointerException if the provided image is null
     */
    public ShortRasterAccess(Raster raster) {
        this.raster = raster;
        this.dataBuffer = this.raster.getDataBuffer();

        this.width = this.raster.getWidth();
        this.planeSize = width * this.raster.getHeight();

        this.canUseDataBuffer = (this.dataBuffer instanceof DataBufferUShort || this.dataBuffer instanceof DataBufferShort) &&
                AccessTools.isSampleModelDirectlyUsable(this.raster);

        this.size = AccessTools.getSizeOfDataBufferInBytes(this.dataBuffer);
    }

    @Override
    public short getValue(int index) {
        int b = index / planeSize;
        int xyIndex = index % planeSize;

        if (canUseDataBuffer) {
            return (short) dataBuffer.getElem(b, xyIndex);
        } else {
            return (short) raster.getSample(xyIndex % width, xyIndex / width, b);
        }
    }

    @Override
    public void setValue(int index, short value) {
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
