package qupath.ext.imglib2.accesses;

import net.imglib2.img.basictypeaccess.array.IntArray;
import net.imglib2.img.basictypeaccess.volatiles.VolatileAccess;
import qupath.ext.imglib2.SizableDataAccess;
import qupath.lib.common.ColorTools;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.awt.image.SinglePixelPackedSampleModel;

/**
 * An {@link IntArray} whose elements are computed from an (A)RGB {@link BufferedImage}.
 * <p>
 * If the alpha component is not provided (e.g. if the {@link BufferedImage} has the {@link BufferedImage#TYPE_INT_RGB} type),
 * then the alpha component of each pixel is considered to be 255.
 * <p>
 * This {@link IntArray} is immutable; any attempt to changes its values will result in a
 * {@link UnsupportedOperationException}.
 * <p>
 * This data access is marked as volatile but always contain valid data.
 */
public class ArgbBufferedImageAccess extends IntArray implements SizableDataAccess, VolatileAccess {

    private final int size;

    /**
     * Create the buffered image access.
     *
     * @param image the image containing the values to return. It is expected to be (A)RGB
     * @throws NullPointerException if the provided image is null
     */
    public ArgbBufferedImageAccess(BufferedImage image) {
        super(createArrayFromImage(image));

        this.size = AccessTools.getSizeOfDataBufferInBytes(image.getRaster().getDataBuffer());
    }

    private static int[] createArrayFromImage(BufferedImage image) {
        Raster raster = image.getRaster();
        int width = raster.getWidth();
        int height = raster.getHeight();
        int planeSize = width * height;

        int[] array = new int[planeSize];
        if (raster.getSampleModel() instanceof SinglePixelPackedSampleModel && raster.getDataBuffer() instanceof DataBufferInt) {
            DataBuffer dataBuffer = raster.getDataBuffer();
            boolean alphaProvided = image.getType() == BufferedImage.TYPE_INT_ARGB;

            for (int i=0; i<planeSize; i++) {
                int pixel = dataBuffer.getElem(0, i);

                if (alphaProvided) {
                    array[i] = pixel;
                } else {
                    array[i] = ColorTools.packARGB(
                            255,
                            ColorTools.red(pixel),
                            ColorTools.green(pixel),
                            ColorTools.blue(pixel)
                    );
                }
            }
        } else {
            for (int y=0; y<height; y++) {
                for (int x=0; x<width; x++) {
                    array[x + y * width] = image.getRGB(x, y);
                }
            }
        }

        return array;
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
