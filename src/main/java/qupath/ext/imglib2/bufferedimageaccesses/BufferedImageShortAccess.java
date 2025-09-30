package qupath.ext.imglib2.bufferedimageaccesses;

import net.imglib2.img.basictypeaccess.ShortAccess;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;

/**
 * A {@link ShortAccess} whose elements are computed from a {@link BufferedImage}.
 * <p>
 * This {@link ShortAccess} is immutable; any attempt to changes its values will result in a
 * {@link UnsupportedOperationException}.
 */
public class BufferedImageShortAccess implements ShortAccess, SizableDataAccess {

    private final Raster raster;
    private final DataBuffer dataBuffer;
    private final int width;
    private final int planeSize;
    private final boolean canUseDataBuffer;
    private final int size;

    /**
     * Create the buffered image access.
     *
     * @param image the image containing the values to return. Its pixels are expected to be stored in the short format
     */
    public BufferedImageShortAccess(BufferedImage image) {
        this.raster = image.getRaster();
        this.dataBuffer = this.raster.getDataBuffer();

        this.width = this.raster.getWidth();
        this.planeSize = width * this.raster.getHeight();

        this.canUseDataBuffer = (this.dataBuffer instanceof DataBufferUShort || this.dataBuffer instanceof DataBufferShort) &&
                BufferedImageAccessTools.isSampleModelDirectlyUsable(this.raster);

        this.size = BufferedImageAccessTools.getSizeOfDataBufferInBytes(this.dataBuffer);
    }

    @Override
    public short getValue(int index) {
        int b = index / planeSize;
        int xyIndex = index % planeSize;

        if (canUseDataBuffer) {
            if (dataBuffer instanceof DataBufferShort dataBufferShort) {
                return dataBufferShort.getBankData()[b][xyIndex];
            } else {
                return ((DataBufferUShort) dataBuffer).getBankData()[b][xyIndex];
            }
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
}
