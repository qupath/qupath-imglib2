package qupath.ext.imglib2.bufferedimageaccesses;

import net.imglib2.img.basictypeaccess.IntAccess;
import qupath.ext.imglib2.SizableDataAccess;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.SinglePixelPackedSampleModel;

/**
 * An {@link IntAccess} whose elements are computed from an (A)RGB {@link BufferedImage}.
 * <p>
 * This {@link IntAccess} is immutable; any attempt to changes its values will result in a
 * {@link UnsupportedOperationException}.
 */
public class ArgbBufferedImageAccess implements IntAccess, SizableDataAccess {

    private final BufferedImage image;
    private final DataBuffer dataBuffer;
    private final int width;
    private final int planeSize;
    private final boolean canUseDataBuffer;
    private final int size;

    /**
     * Create the buffered image access.
     *
     * @param image the image containing the values to return. It is expected to be (A)RGB
     * @throws NullPointerException if the provided image is null
     */
    public ArgbBufferedImageAccess(BufferedImage image) {
        this.image = image;
        this.dataBuffer = this.image.getRaster().getDataBuffer();

        this.width = this.image.getWidth();
        this.planeSize = width * this.image.getHeight();

        this.canUseDataBuffer = image.getRaster().getDataBuffer() instanceof DataBufferInt &&
                image.getRaster().getSampleModel() instanceof SinglePixelPackedSampleModel;

        this.size = AccessTools.getSizeOfDataBufferInBytes(this.dataBuffer);
    }

    @Override
    public int getValue(int index) {
        int b = index / planeSize;
        int xyIndex = index % planeSize;

        if (canUseDataBuffer) {
            return dataBuffer.getElem(b, xyIndex);
        } else {
            return image.getRGB(xyIndex % width, xyIndex / width);
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
