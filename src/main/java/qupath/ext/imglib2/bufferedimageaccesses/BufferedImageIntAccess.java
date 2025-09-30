package qupath.ext.imglib2.bufferedimageaccesses;

import net.imglib2.img.basictypeaccess.IntAccess;
import qupath.ext.imglib2.SizableDataAccess;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;

/**
 * An {@link IntAccess} whose elements are computed from a {@link BufferedImage}.
 * <p>
 * This {@link IntAccess} is immutable; any attempt to changes its values will result in a
 * {@link UnsupportedOperationException}.
 */
public class BufferedImageIntAccess implements IntAccess, SizableDataAccess {

    private final Raster raster;
    private final DataBuffer dataBuffer;
    private final int width;
    private final int planeSize;
    private final boolean canUseDataBuffer;
    private final int size;

    /**
     * Create the buffered image access.
     *
     * @param image the image containing the values to return. Its pixels are expected to be stored in the int format
     * @throws NullPointerException if the provided image is null
     */
    public BufferedImageIntAccess(BufferedImage image) {
        this.raster = image.getRaster();
        this.dataBuffer = this.raster.getDataBuffer();

        this.width = this.raster.getWidth();
        this.planeSize = width * this.raster.getHeight();

        this.canUseDataBuffer = this.dataBuffer instanceof DataBufferInt &&
                BufferedImageAccessTools.isSampleModelDirectlyUsable(this.raster);

        this.size = BufferedImageAccessTools.getSizeOfDataBufferInBytes(this.dataBuffer);
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
}
