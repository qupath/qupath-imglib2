package qupath.ext.imglib2.accesses;

import net.imglib2.img.basictypeaccess.IntAccess;
import net.imglib2.img.basictypeaccess.volatiles.VolatileAccess;
import qupath.ext.imglib2.SizableDataAccess;
import qupath.lib.common.ColorTools;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.SinglePixelPackedSampleModel;

/**
 * An {@link IntAccess} whose elements are computed from an (A)RGB {@link BufferedImage}.
 * <p>
 * If the alpha component is not provided (e.g. if the {@link BufferedImage} has the {@link BufferedImage#TYPE_INT_RGB} type),
 * then the alpha component of each pixel is considered to be 255.
 * <p>
 * This {@link IntAccess} is immutable; any attempt to changes its values will result in a
 * {@link UnsupportedOperationException}.
 * <p>
 * This data access is marked as volatile but always contain valid data.
 */
public class ArgbBufferedImageAccess implements IntAccess, SizableDataAccess, VolatileAccess {

    private final BufferedImage image;
    private final DataBuffer dataBuffer;
    private final int width;
    private final int planeSize;
    private final boolean canUseDataBuffer;
    private final boolean alphaProvided;
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
        this.alphaProvided = image.getType() == BufferedImage.TYPE_INT_ARGB;

        this.size = AccessTools.getSizeOfDataBufferInBytes(this.dataBuffer);
    }

    @Override
    public int getValue(int index) {
        int b = index / planeSize;
        int xyIndex = index % planeSize;

        if (canUseDataBuffer) {
            int pixel = dataBuffer.getElem(b, xyIndex);

            if (alphaProvided) {
                return pixel;
            } else {
                return ColorTools.packARGB(
                        255,
                        ColorTools.red(pixel),
                        ColorTools.green(pixel),
                        ColorTools.blue(pixel)
                );
            }
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

    @Override
    public boolean isValid() {
        return true;
    }
}
