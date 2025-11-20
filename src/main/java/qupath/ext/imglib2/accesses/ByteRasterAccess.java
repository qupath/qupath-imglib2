package qupath.ext.imglib2.accesses;

import net.imglib2.img.basictypeaccess.ByteAccess;
import net.imglib2.img.basictypeaccess.volatiles.VolatileAccess;
import qupath.ext.imglib2.SizableDataAccess;

import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;

/**
 * A {@link ByteAccess} whose elements are computed from a {@link Raster}.
 * <p>
 * This {@link ByteAccess} is immutable; any attempt to changes its values will result in a
 * {@link UnsupportedOperationException}.
 * <p>
 * This data access is marked as volatile but always contain valid data.
 */
public class ByteRasterAccess implements ByteAccess, SizableDataAccess, VolatileAccess {

    private final Raster raster;
    private final DataBuffer dataBuffer;
    private final int width;
    private final int planeSize;
    private final boolean canUseDataBuffer;
    private final int size;

    /**
     * Create the byte raster access.
     *
     * @param raster the raster containing the values to return. Its pixels are expected to be stored in the byte format
     * @throws NullPointerException if the provided image is null
     */
    public ByteRasterAccess(Raster raster) {
        this.raster = raster;
        this.dataBuffer = this.raster.getDataBuffer();

        this.width = this.raster.getWidth();
        this.planeSize = width * this.raster.getHeight();

        this.canUseDataBuffer = this.dataBuffer instanceof DataBufferByte &&
                AccessTools.isSampleModelDirectlyUsable(this.raster);

        this.size = AccessTools.getSizeOfDataBufferInBytes(this.dataBuffer);
    }

    @Override
    public byte getValue(int index) {
        int b = index / planeSize;
        int xyIndex = index % planeSize;

        if (canUseDataBuffer) {
            return (byte) dataBuffer.getElem(b, xyIndex);
        } else {
            return (byte) raster.getSample(xyIndex % width, xyIndex / width, b);
        }
    }

    @Override
    public void setValue(int index, byte value) {
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
