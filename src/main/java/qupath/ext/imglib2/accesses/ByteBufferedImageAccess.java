package qupath.ext.imglib2.accesses;

import net.imglib2.img.basictypeaccess.array.ByteArray;
import net.imglib2.img.basictypeaccess.volatiles.VolatileAccess;
import qupath.ext.imglib2.SizableDataAccess;
import qupath.lib.common.ColorTools;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.awt.image.SinglePixelPackedSampleModel;

/**
 * An {@link ByteArray} whose elements are computed from an RGB {@link BufferedImage}.
 * <p>
 * The alpha component is not taken into account.
 * <p>
 * This {@link ByteArray} is immutable; any attempt to changes its values will result in a
 * {@link UnsupportedOperationException}.
 * <p>
 * This data access is marked as volatile but always contain valid data.
 */
public class ByteBufferedImageAccess extends ByteArray implements SizableDataAccess, VolatileAccess {

    private final int size;

    /**
     * Create the byte buffered image access.
     *
     * @param image the image containing the values to return. It is expected to be (A)RGB
     * @throws NullPointerException if the provided image is null
     */
    public ByteBufferedImageAccess(BufferedImage image) {
        super(createArrayFromImage(image));

        this.size = AccessTools.getSizeOfDataBufferInBytes(image.getRaster().getDataBuffer());
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

    private static byte[] createArrayFromImage(BufferedImage image) {
        Raster raster = image.getRaster();
        int width = raster.getWidth();
        int height = raster.getHeight();
        int planeSize = width * height;
        int numBands = 3;

        byte[] array = new byte[planeSize * numBands];
        if (raster.getSampleModel() instanceof SinglePixelPackedSampleModel && raster.getDataBuffer() instanceof DataBufferInt) {
            DataBuffer dataBuffer = raster.getDataBuffer();

            for (int b=0; b<numBands; b++) {
                for (int i=0; i<planeSize; i++) {
                    int pixel = dataBuffer.getElem(0, i);

                    array[i + b * planeSize] = (byte) switch (b) {
                        case 0 -> ColorTools.red(pixel);
                        case 1 -> ColorTools.green(pixel);
                        case 2 -> ColorTools.blue(pixel);
                        default -> throw new IllegalArgumentException(String.format("The provided channel %d is out of bounds", b));
                    };
                }
            }
        } else {
            for (int b=0; b<numBands; b++) {
                for (int y=0; y<height; y++) {
                    for (int x=0; x<width; x++) {
                        int pixel = image.getRGB(x, y);

                        array[x + y * width + b * planeSize] = (byte) switch (b) {
                            case 0 -> ColorTools.red(pixel);
                            case 1 -> ColorTools.green(pixel);
                            case 2 -> ColorTools.blue(pixel);
                            default -> throw new IllegalArgumentException(String.format("The provided channel %d is out of bounds", b));
                        };
                    }
                }
            }
        }

        return array;
    }
}
