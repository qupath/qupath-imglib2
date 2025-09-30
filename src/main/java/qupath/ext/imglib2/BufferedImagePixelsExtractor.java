package qupath.ext.imglib2;

import qupath.ext.imglib2.immutablearrays.ImmutableByteArray;
import qupath.ext.imglib2.immutablearrays.ImmutableDoubleArray;
import qupath.ext.imglib2.immutablearrays.ImmutableFloatArray;
import qupath.ext.imglib2.immutablearrays.ImmutableIntArray;
import qupath.ext.imglib2.immutablearrays.ImmutableShortArray;

import java.awt.image.BandedSampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferDouble;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Utility functions to extract arrays of pixels from {@link BufferedImage}.
 */
public class BufferedImagePixelsExtractor {

    private BufferedImagePixelsExtractor() {
        throw new AssertionError("This class is not instantiable.");
    }

    /**
     * Create an immutable integer array containing (A)RGB pixels of the provided image.
     * <p>
     * This function will avoid making a copy of the pixels if possible.
     * <p>
     * This function expects the provided image to have the (A)RGB format.
     *
     * @param image the image containing the ARGB pixels to extract
     * @return an immutable integer array containing (A)RGB pixels of the provided image. The integer representation of an (A)RGB
     * pixel located at coordinates [x;y] is located at index [x + imageWidth * y] of the returned array
     */
    public static ImmutableIntArray getArgb(BufferedImage image) {
        int[] array;

        // Avoid calling img.getRGB() if possible, as this call makes a copy of the pixels
        if (image.getRaster().getDataBuffer() instanceof DataBufferInt dataBufferInt && image.getRaster().getSampleModel() instanceof SinglePixelPackedSampleModel) {
            array = dataBufferInt.getBankData()[0];    // SinglePixelPackedSampleModel contains all data array elements in the first bank of the DataBuffer
        } else {
            array = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        }

        return new ImmutableIntArray(array);
    }

    /**
     * Create an immutable byte array containing byte pixels of the provided raster.
     * <p>
     * This function will avoid making a copy of the pixels if possible.
     * <p>
     * This function expects pixels of the provided raster to be stored with the byte format.
     *
     * @param raster the raster containing the byte pixels to extract
     * @return an immutable byte array containing pixels of the provided image. The byte representation of a pixel located at
     * coordinates [x;y;c] is located at index [x + imageWidth * y + imageWidth * imageHeight * c] of the returned array
     */
    public static ImmutableByteArray getBytes(Raster raster) {
        int planeSize = getPlaneSize(raster);
        int nBands = raster.getNumBands();
        byte[] array;

        // Avoid calling raster.getSamples() if possible, as this call makes a copy of the pixels
        if (raster.getDataBuffer() instanceof DataBufferByte dataBufferByte && isSampleModelDirectlyUsable(raster.getSampleModel(), nBands)) {
            byte[][] pixels = dataBufferByte.getBankData();

            // If there's only one channel, no copy is necessary
            if (pixels.length == 1) {
                array = pixels[0];
            } else {
                array = new byte[planeSize * nBands];

                for (int bank=0; bank<nBands; bank++) {
                    System.arraycopy(pixels[bank], 0, array, bank * planeSize, planeSize);
                }
            }
        } else {
            array = new byte[planeSize * nBands];
            int[] source = null;
            int ind = 0;
            for (int band = 0; band < nBands; band++) {
                source = raster.getSamples(0, 0, raster.getWidth(), raster.getHeight(), band, source);
                for (int val : source) {
                    array[ind++] = (byte) val;
                }
            }
        }

        return new ImmutableByteArray(array);
    }

    /**
     * Create an immutable short array containing short pixels of the provided raster.
     * <p>
     * This function will avoid making a copy of the pixels if possible.
     * <p>
     * This function expects pixels of the provided raster to be stored with the short format.
     *
     * @param raster the raster containing the short pixels to extract
     * @return an immutable short array containing pixels of the provided image. The short representation of a pixel located at
     * coordinates [x;y;c] is located at index [x + imageWidth * y + imageWidth * imageHeight * c] of the returned array
     */
    public static ImmutableShortArray getShorts(Raster raster) {
        int planeSize = getPlaneSize(raster);
        int nBands = raster.getNumBands();
        short[] array;

        // See getBytes() for an explanation of the optimizations
        if (
                (raster.getDataBuffer() instanceof DataBufferShort || raster.getDataBuffer() instanceof DataBufferUShort) &&
                        isSampleModelDirectlyUsable(raster.getSampleModel(), nBands)
        ) {
            short[][] pixels;
            if (raster.getDataBuffer() instanceof DataBufferShort dataBufferShort) {
                pixels = dataBufferShort.getBankData();
            } else {
                pixels = ((DataBufferUShort) raster.getDataBuffer()).getBankData();
            }

            if (pixels.length == 1) {
                array = pixels[0];
            } else {
                array = new short[planeSize * nBands];

                for (int bank=0; bank<nBands; bank++) {
                    System.arraycopy(pixels[bank], 0, array, bank * planeSize, planeSize);
                }
            }
        } else {
            array = new short[planeSize * nBands];
            int[] source = null;
            int ind = 0;
            for (int band = 0; band < nBands; band++) {
                source = raster.getSamples(0, 0, raster.getWidth(), raster.getHeight(), band, source);
                for (int val : source) {
                    array[ind++] = (short) val;
                }
            }
        }

        return new ImmutableShortArray(array);
    }

    /**
     * Create an immutable integer array containing integer pixels of the provided raster.
     * <p>
     * This function will avoid making a copy of the pixels if possible.
     * <p>
     * This function expects pixels of the provided raster to be stored with the integer format.
     *
     * @param raster the raster containing the integer pixels to extract
     * @return an immutable integer array containing pixels of the provided image. The integer representation of a pixel located at
     * coordinates [x;y;c] is located at index [x + imageWidth * y + imageWidth * imageHeight * c] of the returned array
     */
    public static ImmutableIntArray getIntegers(Raster raster) {
        int planeSize = getPlaneSize(raster);
        int nBands = raster.getNumBands();
        int[] array;

        // See getBytes() for an explanation of the optimizations
        if (raster.getDataBuffer() instanceof DataBufferInt dataBufferInt && isSampleModelDirectlyUsable(raster.getSampleModel(), nBands)) {
            int[][] pixels = dataBufferInt.getBankData();

            if (pixels.length == 1) {
                array = pixels[0];
            } else {
                array = new int[planeSize * nBands];

                for (int bank=0; bank<nBands; bank++) {
                    System.arraycopy(pixels[bank], 0, array, bank * planeSize, planeSize);
                }
            }
        } else {
            array = new int[planeSize * nBands];
            int[] source = null;
            for (int band = 0; band < nBands; band++) {
                source = raster.getSamples(0, 0, raster.getWidth(), raster.getHeight(), band, source);
                System.arraycopy(source, 0, array, band * planeSize, planeSize);
            }
        }

        return new ImmutableIntArray(array);
    }

    /**
     * Create an immutable float array containing float pixels of the provided raster.
     * <p>
     * This function will avoid making a copy of the pixels if possible.
     * <p>
     * This function expects pixels of the provided raster to be stored with the float format.
     *
     * @param raster the raster containing the float pixels to extract
     * @return an immutable float array containing pixels of the provided image. The float representation of a pixel located at
     * coordinates [x;y;c] is located at index [x + imageWidth * y + imageWidth * imageHeight * c] of the returned array
     */
    public static ImmutableFloatArray getFloats(Raster raster) {
        int planeSize = getPlaneSize(raster);
        int nBands = raster.getNumBands();
        float[] array;

        // See getBytes() for an explanation of the optimizations
        if (raster.getDataBuffer() instanceof DataBufferFloat dataBufferFloat && isSampleModelDirectlyUsable(raster.getSampleModel(), nBands)) {
            float[][] pixels = dataBufferFloat.getBankData();

            if (pixels.length == 1) {
                array = pixels[0];
            } else {
                array = new float[planeSize * nBands];

                for (int bank=0; bank<nBands; bank++) {
                    System.arraycopy(pixels[bank], 0, array, bank * planeSize, planeSize);
                }
            }
        } else {
            array = new float[planeSize * nBands];
            float[] source = null;
            for (int band = 0; band < nBands; band++) {
                source = raster.getSamples(0, 0, raster.getWidth(), raster.getHeight(), band, source);
                System.arraycopy(source, 0, array, band * planeSize, planeSize);
            }
        }

        return new ImmutableFloatArray(array);
    }

    /**
     * Create an immutable double array containing double pixels of the provided raster.
     * <p>
     * This function will avoid making a copy of the pixels if possible.
     * <p>
     * This function expects pixels of the provided raster to be stored with the double format.
     *
     * @param raster the raster containing the double pixels to extract
     * @return an immutable double array containing pixels of the provided image. The double representation of a pixel located at
     * coordinates [x;y;c] is located at index [x + imageWidth * y + imageWidth * imageHeight * c] of the returned array
     */
    public static ImmutableDoubleArray getDoubles(Raster raster) {
        int planeSize = getPlaneSize(raster);
        int nBands = raster.getNumBands();
        double[] array;

        // See getBytes() for an explanation of the optimizations
        if (raster.getDataBuffer() instanceof DataBufferDouble dataBufferDouble && isSampleModelDirectlyUsable(raster.getSampleModel(), nBands)) {
            double[][] pixels = dataBufferDouble.getBankData();

            if (pixels.length == 1) {
                array = pixels[0];
            } else {
                array = new double[planeSize * nBands];

                for (int bank=0; bank<nBands; bank++) {
                    System.arraycopy(pixels[bank], 0, array, bank * planeSize, planeSize);
                }
            }
        } else {
            array = new double[planeSize * nBands];
            double[] source = null;
            for (int band = 0; band < nBands; band++) {
                source = raster.getSamples(0, 0, raster.getWidth(), raster.getHeight(), band, source);
                System.arraycopy(source, 0, array, band * planeSize, planeSize);
            }
        }

        return new ImmutableDoubleArray(array);
    }

    private static int getPlaneSize(Raster raster) {
        return raster.getWidth() * raster.getHeight();
    }

    private static boolean isSampleModelDirectlyUsable(SampleModel sampleModel, int nBands) {
        return sampleModel instanceof BandedSampleModel bandedSampleModel &&
                Arrays.stream(bandedSampleModel.getBandOffsets()).allMatch(offset -> offset == 0) &&
                Arrays.equals(bandedSampleModel.getBankIndices(), IntStream.range(0, nBands).toArray());
    }
}
