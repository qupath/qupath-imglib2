package qupath.ext.imglib2.bufferedimageaccesses;

import net.imglib2.img.basictypeaccess.FloatAccess;
import qupath.ext.imglib2.SizableDataAccess;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferFloat;
import java.awt.image.Raster;

/**
 * A {@link FloatAccess} whose elements are computed from a {@link BufferedImage}.
 * <p>
 * This {@link FloatAccess} is immutable; any attempt to changes its values will result in a
 * {@link UnsupportedOperationException}.
 */
public class BufferedImageFloatAccess implements FloatAccess, SizableDataAccess {

    private final Raster raster;
    private final DataBuffer dataBuffer;
    private final int width;
    private final int planeSize;
    private final boolean canUseDataBuffer;
    private final int size;

    /**
     * Create the buffered image access.
     *
     * @param image the image containing the values to return. Its pixels are expected to be stored in the float format
     * @throws NullPointerException if the provided image is null
     */
    public BufferedImageFloatAccess(BufferedImage image) {
        this.raster = image.getRaster();
        this.dataBuffer = this.raster.getDataBuffer();

        this.width = this.raster.getWidth();
        this.planeSize = width * this.raster.getHeight();

        this.canUseDataBuffer = this.dataBuffer instanceof DataBufferFloat &&
                BufferedImageAccessTools.isSampleModelDirectlyUsable(this.raster);

        this.size = BufferedImageAccessTools.getSizeOfDataBufferInBytes(this.dataBuffer);
    }

    @Override
    public float getValue(int index) {
        int b = index / planeSize;
        int xyIndex = index % planeSize;

        if (canUseDataBuffer) {
            return dataBuffer.getElemFloat(b, xyIndex);
        } else {
            return raster.getSampleFloat(xyIndex % width, xyIndex / width, b);
        }
    }

    @Override
    public void setValue(int index, float value) {
        throw new UnsupportedOperationException("This access is not mutable");
    }

    @Override
    public int getSizeBytes() {
        return size;
    }
}
