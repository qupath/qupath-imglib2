package qupath.ext.imglib2.accesses;

import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.img.basictypeaccess.volatiles.VolatileAccess;
import qupath.ext.imglib2.SizableDataAccess;

import java.awt.image.DataBuffer;
import java.awt.image.DataBufferDouble;
import java.awt.image.Raster;

/**
 * A {@link DoubleArray} whose elements are computed from a {@link Raster}.
 * <p>
 * This {@link DoubleArray} is immutable; any attempt to changes its values will result in a
 * {@link UnsupportedOperationException}.
 * <p>
 * This data access is marked as volatile but always contain valid data.
 */
public class DoubleRasterAccess extends DoubleArray implements SizableDataAccess, VolatileAccess {

    private final int size;

    /**
     * Create the double raster access.
     *
     * @param raster the raster containing the values to return. Its pixels are expected to be stored in the double format
     * @throws NullPointerException if the provided image is null
     */
    public DoubleRasterAccess(Raster raster) {
        super(createArrayFromRaster(raster));

        this.size = AccessTools.getSizeOfDataBufferInBytes(raster.getDataBuffer());
    }

    @Override
    public void setValue(int index, double value) {
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

    private static double[] createArrayFromRaster(Raster raster) {
        int width = raster.getWidth();
        int height = raster.getHeight();
        int planeSize = width * height;
        int numBands = raster.getNumBands();

        double[] array = new double[planeSize * numBands];
        if (AccessTools.isSampleModelDirectlyUsable(raster) && raster.getDataBuffer() instanceof DataBufferDouble) {
            DataBuffer dataBuffer = raster.getDataBuffer();

            for (int b=0; b<numBands; b++) {
                for (int i=0; i<planeSize; i++) {
                    array[i + b * planeSize] = dataBuffer.getElemDouble(b, i);
                }
            }
        } else {
            for (int b=0; b<numBands; b++) {
                for (int y=0; y<height; y++) {
                    for (int x=0; x<width; x++) {
                        array[x + y * width + b * planeSize] = raster.getSampleDouble(x, y, b);
                    }
                }
            }
        }

        return array;
    }
}
