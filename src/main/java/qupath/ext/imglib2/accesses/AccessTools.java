package qupath.ext.imglib2.accesses;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BandedSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferDouble;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * A collection of helpers methods for buffered image and raster data accesses.
 */
class AccessTools {

    private static final Logger logger = LoggerFactory.getLogger(AccessTools.class);

    private AccessTools() {
        throw new AssertionError("This class is not instantiable.");
    }

    /**
     * Check whether the sample model of the provided raster stores pixels the following way: a pixel located at [x;y;b]
     * is accessible by looking at bank b and index x + width*y + bankOffset of the data buffer
     *
     * @param raster the raster containing the sample model and the pixels
     * @return whether the sample model of the provided raster stores pixels in the way described above
     */
    public static boolean isSampleModelDirectlyUsable(Raster raster) {
        return raster.getSampleModel() instanceof BandedSampleModel bandedSampleModel &&
                Arrays.equals(bandedSampleModel.getBankIndices(), IntStream.range(0, raster.getNumBands()).toArray());
    }

    /**
     * Get the size of the provided data buffer in bytes. This only consider the internal arrays of the data buffer.
     *
     * @param dataBuffer the data buffer whose size should be computed
     * @return the size of the provided data buffer in bytes
     */
    public static int getSizeOfDataBufferInBytes(DataBuffer dataBuffer) {
        int bytesPerPixel;
        if (dataBuffer instanceof DataBufferByte) {
            bytesPerPixel = 1;
        } else if (dataBuffer instanceof DataBufferShort || dataBuffer instanceof DataBufferUShort) {
            bytesPerPixel = 2;
        } else if (dataBuffer instanceof DataBufferInt || dataBuffer instanceof DataBufferFloat) {
            bytesPerPixel = 4;
        } else  if (dataBuffer instanceof DataBufferDouble) {
            bytesPerPixel = 8;
        } else {
            logger.warn("Unexpected data buffer {}. Considering each element of it takes 1 byte", dataBuffer);
            bytesPerPixel = 1;
        }

        return bytesPerPixel * dataBuffer.getSize() * dataBuffer.getNumBanks();
    }
}
