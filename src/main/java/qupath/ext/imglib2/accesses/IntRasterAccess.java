package qupath.ext.imglib2.accesses;

import net.imglib2.img.basictypeaccess.array.IntArray;
import net.imglib2.img.basictypeaccess.volatiles.VolatileAccess;
import qupath.ext.imglib2.SizableDataAccess;

import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;

/**
 * An {@link IntArray} whose elements are computed from a {@link Raster}.
 * <p>
 * This {@link IntArray} is immutable; any attempt to changes its values will result in a
 * {@link UnsupportedOperationException}.
 * <p>
 * This data access is marked as volatile but always contain valid data.
 */
public class IntRasterAccess extends IntArray implements SizableDataAccess, VolatileAccess {

    private final int size;

    /**
     * Create the int raster access.
     *
     * @param raster the raster containing the values to return. Its pixels are expected to be stored in the int format
     * @throws NullPointerException if the provided raster is null
     */
    public IntRasterAccess(Raster raster) {
        super(createArrayFromRaster(raster));

        this.size = AccessTools.getSizeOfDataBufferInBytes(raster.getDataBuffer());
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

    private static int[] createArrayFromRaster(Raster raster) {
        int width = raster.getWidth();
        int height = raster.getHeight();
        int planeSize = width * height;
        int numBands = raster.getNumBands();

        int[] array = new int[planeSize * numBands];
        if (AccessTools.isSampleModelDirectlyUsable(raster) && raster.getDataBuffer() instanceof DataBufferInt) {
            DataBuffer dataBuffer = raster.getDataBuffer();

            for (int b=0; b<numBands; b++) {
                for (int i=0; i<planeSize; i++) {
                    array[i + b * planeSize] = dataBuffer.getElem(b, i);
                }
            }
        } else {
            for (int b=0; b<numBands; b++) {
                for (int y=0; y<height; y++) {
                    for (int x=0; x<width; x++) {
                        array[x + y * width + b * planeSize] = raster.getSample(x, y, b);
                    }
                }
            }
        }

        return array;
    }
}
