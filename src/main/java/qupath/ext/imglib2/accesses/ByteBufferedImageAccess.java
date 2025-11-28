package qupath.ext.imglib2.accesses;

import net.imglib2.img.basictypeaccess.ByteAccess;
import net.imglib2.img.basictypeaccess.volatiles.VolatileAccess;
import qupath.ext.imglib2.SizableDataAccess;
import qupath.lib.common.ColorTools;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.SinglePixelPackedSampleModel;

/**
 * An {@link ByteAccess} whose elements are computed from an RGB {@link BufferedImage}.
 * <p>
 * The alpha component is not taken into account.
 * <p>
 * This {@link ByteAccess} is immutable; any attempt to changes its values will result in a
 * {@link UnsupportedOperationException}.
 * <p>
 * This data access is marked as volatile but always contain valid data.
 */
public class ByteBufferedImageAccess implements ByteAccess, SizableDataAccess, VolatileAccess {

    private final BufferedImage image;
    private final DataBuffer dataBuffer;
    private final int width;
    private final int planeSize;
    private final boolean canUseDataBuffer;
    private final int size;

    /**
     * Create the byte buffered image access.
     *
     * @param image the image containing the values to return. It is expected to be (A)RGB
     * @throws NullPointerException if the provided image is null
     */
    public ByteBufferedImageAccess(BufferedImage image) {
        this.image = image;
        this.dataBuffer = this.image.getRaster().getDataBuffer();

        this.width = this.image.getWidth();
        this.planeSize = width * this.image.getHeight();

        this.canUseDataBuffer = image.getRaster().getDataBuffer() instanceof DataBufferInt &&
                image.getRaster().getSampleModel() instanceof SinglePixelPackedSampleModel;

        this.size = AccessTools.getSizeOfDataBufferInBytes(this.dataBuffer);
    }

    @Override
    public byte getValue(int index) {
        int channel = index / planeSize;
        int xyIndex = index % planeSize;

        int pixel = canUseDataBuffer ?
                dataBuffer.getElem(0, xyIndex) :
                image.getRGB(xyIndex % width, xyIndex / width);

        return switch (channel) {
            case 0 -> (byte) ColorTools.red(pixel);
            case 1 -> (byte) ColorTools.green(pixel);
            case 2 -> (byte) ColorTools.blue(pixel);
            default -> throw new IllegalArgumentException(String.format("The provided index %d is out of bounds", index));
        };
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
