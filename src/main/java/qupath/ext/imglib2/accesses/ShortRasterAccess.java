package qupath.ext.imglib2.accesses;

import net.imglib2.img.basictypeaccess.array.ShortArray;
import net.imglib2.img.basictypeaccess.volatiles.VolatileAccess;
import qupath.ext.imglib2.SizableDataAccess;

import java.awt.image.DataBuffer;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;

/**
 * A {@link ShortArray} whose elements are computed from a {@link Raster}.
 * <p>
 * This {@link ShortArray} is immutable; any attempt to changes its values will result in a
 * {@link UnsupportedOperationException}.
 * <p>
 * This data access is marked as volatile but always contain valid data.
 */
public class ShortRasterAccess extends ShortArray implements SizableDataAccess, VolatileAccess {

    private final int size;

    /**
     * Create the short raster access.
     *
     * @param raster the raster containing the values to return. Its pixels are expected to be stored in the short format
     * @throws NullPointerException if the provided image is null
     */
    public ShortRasterAccess(Raster raster) {
        super(createArrayFromRaster(raster));

        this.size = AccessTools.getSizeOfDataBufferInBytes(raster.getDataBuffer());
    }

    @Override
    public void setValue(int index, short value) {
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

    private static short[] createArrayFromRaster(Raster raster) {
        int width = raster.getWidth();
        int height = raster.getHeight();
        int planeSize = width * height;
        int numBands = raster.getNumBands();

        short[] array = new short[planeSize * numBands];
        if (AccessTools.isSampleModelDirectlyUsable(raster) && (raster.getDataBuffer() instanceof DataBufferUShort || raster.getDataBuffer() instanceof DataBufferShort)) {
            DataBuffer dataBuffer = raster.getDataBuffer();

            for (int b=0; b<numBands; b++) {
                for (int i=0; i<planeSize; i++) {
                    array[i + b * planeSize] = (short) dataBuffer.getElem(b, i);
                }
            }
        } else {
            for (int b=0; b<numBands; b++) {
                for (int y=0; y<height; y++) {
                    for (int x=0; x<width; x++) {
                        array[x + y * width + b * planeSize] = (short) raster.getSample(x, y, b);
                    }
                }
            }
        }

        return array;
    }
}
