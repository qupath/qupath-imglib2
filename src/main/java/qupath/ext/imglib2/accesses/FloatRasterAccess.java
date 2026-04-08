package qupath.ext.imglib2.accesses;

import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.img.basictypeaccess.volatiles.VolatileAccess;
import qupath.ext.imglib2.SizableDataAccess;

import java.awt.image.DataBuffer;
import java.awt.image.DataBufferFloat;
import java.awt.image.Raster;

/**
 * A {@link FloatArray} whose elements are computed from a {@link Raster}.
 * <p>
 * This {@link FloatArray} is immutable; any attempt to changes its values will result in a
 * {@link UnsupportedOperationException}.
 * <p>
 * This data access is marked as volatile but always contain valid data.
 */
public class FloatRasterAccess extends FloatArray implements SizableDataAccess, VolatileAccess {

    private final int size;

    /**
     * Create the float raster access.
     *
     * @param raster the raster containing the values to return. Its pixels are expected to be stored in the float format
     * @throws NullPointerException if the provided image is null
     */
    public FloatRasterAccess(Raster raster) {
        super(createArrayFromRaster(raster));

        this.size = AccessTools.getSizeOfDataBufferInBytes(raster.getDataBuffer());
    }

    @Override
    public void setValue(int index, float value) {
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

    private static float[] createArrayFromRaster(Raster raster) {
        int width = raster.getWidth();
        int height = raster.getHeight();
        int planeSize = width * height;
        int numBands = raster.getNumBands();

        float[] array = new float[planeSize * numBands];
        if (AccessTools.isSampleModelDirectlyUsable(raster) && raster.getDataBuffer() instanceof DataBufferFloat) {
            DataBuffer dataBuffer = raster.getDataBuffer();

            for (int b=0; b<numBands; b++) {
                for (int i=0; i<planeSize; i++) {
                    array[i + b * planeSize] = dataBuffer.getElemFloat(b, i);
                }
            }
        } else {
            for (int b=0; b<numBands; b++) {
                for (int y=0; y<height; y++) {
                    for (int x=0; x<width; x++) {
                        array[x + y * width + b * planeSize] = raster.getSampleFloat(x, y, b);
                    }
                }
            }
        }

        return array;
    }
}
