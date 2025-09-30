package qupath.ext.imglib2;

import net.imglib2.type.numeric.ARGBType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import qupath.ext.imglib2.immutablearrays.ImmutableByteArray;
import qupath.ext.imglib2.immutablearrays.ImmutableDoubleArray;
import qupath.ext.imglib2.immutablearrays.ImmutableFloatArray;
import qupath.ext.imglib2.immutablearrays.ImmutableIntArray;
import qupath.ext.imglib2.immutablearrays.ImmutableShortArray;

import java.awt.image.BandedSampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferDouble;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.Arrays;

public class TestBufferedImagePixelsExtractor {

    @Test
    void Check_Pixels_Of_Argb_Image() {
        int width = 5;
        int height = 5;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int[] rgbArray = new int[width * height];
        Arrays.fill(rgbArray, ARGBType.rgba(255, 125, 45, 0));
        image.setRGB(0, 0, width, height, rgbArray, 0, width);
        ImmutableIntArray expectedArgb = new ImmutableIntArray(rgbArray);

        ImmutableIntArray argb = BufferedImagePixelsExtractor.getArgb(image);

        Assertions.assertArrayEquals(expectedArgb.getCurrentStorageArray(), argb.getCurrentStorageArray());
    }

    @Test
    void Check_Pixels_Of_Byte_Raster() {
        int width = 3;
        int height = 2;
        int nChannels = 2;
        byte[][] pixels = new byte[][] {
                new byte[] {
                        3, 6, 8,
                        4, 56, 7
                },
                new byte[] {
                        34, 46, 0,
                        65, 7, 90
                },
        };
        ImmutableByteArray expectedBytes = new ImmutableByteArray(new byte[] {
                3, 6, 8,
                4, 56, 7,

                34, 46, 0,
                65, 7, 90
        });
        DataBuffer dataBuffer = new DataBufferByte(pixels, nChannels);
        Raster raster = WritableRaster.createWritableRaster(
                new BandedSampleModel(
                        dataBuffer.getDataType(),
                        width,
                        height,
                        nChannels
                ),
                dataBuffer,
                null
        );

        ImmutableByteArray bytes = BufferedImagePixelsExtractor.getBytes(raster);

        Assertions.assertArrayEquals(expectedBytes.getCurrentStorageArray(), bytes.getCurrentStorageArray());
    }

    @Test
    void Check_Pixels_Of_Short_Raster() {
        int width = 3;
        int height = 2;
        int nChannels = 2;
        short[][] pixels = new short[][] {
                new short[] {
                        355, 6, 8,
                        4, 556, 7
                },
                new short[] {
                        34, 446, 0,
                        65, 7, 790
                },
        };
        ImmutableShortArray expectedShorts = new ImmutableShortArray(new short[] {
                355, 6, 8,
                4, 556, 7,

                34, 446, 0,
                65, 7, 790
        });
        DataBuffer dataBuffer = new DataBufferShort(pixels, nChannels);
        Raster raster = WritableRaster.createWritableRaster(
                new BandedSampleModel(
                        dataBuffer.getDataType(),
                        width,
                        height,
                        nChannels
                ),
                dataBuffer,
                null
        );

        ImmutableShortArray shorts = BufferedImagePixelsExtractor.getShorts(raster);

        Assertions.assertArrayEquals(expectedShorts.getCurrentStorageArray(), shorts.getCurrentStorageArray());
    }

    @Test
    void Check_Pixels_Of_Integer_Raster() {
        int width = 3;
        int height = 2;
        int nChannels = 2;
        int[][] pixels = new int[][] {
                new int[] {
                        35584984, 6, 8,
                        4, 556, 7
                },
                new int[] {
                        34, 446456, 0,
                        65, 7, 790
                },
        };
        ImmutableIntArray expectedIntegers = new ImmutableIntArray(new int[] {
                35584984, 6, 8,
                4, 556, 7,

                34, 446456, 0,
                65, 7, 790
        });
        DataBuffer dataBuffer = new DataBufferInt(pixels, nChannels);
        Raster raster = WritableRaster.createWritableRaster(
                new BandedSampleModel(
                        dataBuffer.getDataType(),
                        width,
                        height,
                        nChannels
                ),
                dataBuffer,
                null
        );

        ImmutableIntArray integers = BufferedImagePixelsExtractor.getIntegers(raster);

        Assertions.assertArrayEquals(expectedIntegers.getCurrentStorageArray(), integers.getCurrentStorageArray());
    }

    @Test
    void Check_Pixels_Of_Float_Raster() {
        int width = 3;
        int height = 2;
        int nChannels = 2;
        float[][] pixels = new float[][] {
                new float[] {
                        3.4f, 6, 8,
                        4, 55.6f, 7
                },
                new float[] {
                        34, 4.56f, 0,
                        65, 7, 7.9f
                },
        };
        ImmutableFloatArray expectedFloats = new ImmutableFloatArray(new float[] {
                3.4f, 6, 8,
                4, 55.6f, 7,

                34, 4.56f, 0,
                65, 7, 7.9f
        });
        DataBuffer dataBuffer = new DataBufferFloat(pixels, nChannels);
        Raster raster = WritableRaster.createWritableRaster(
                new BandedSampleModel(
                        dataBuffer.getDataType(),
                        width,
                        height,
                        nChannels
                ),
                dataBuffer,
                null
        );

        ImmutableFloatArray floats = BufferedImagePixelsExtractor.getFloats(raster);

        Assertions.assertArrayEquals(expectedFloats.getCurrentStorageArray(), floats.getCurrentStorageArray());
    }

    @Test
    void Check_Pixels_Of_Double_Raster() {
        int width = 3;
        int height = 2;
        int nChannels = 2;
        double[][] pixels = new double[][] {
                new double[] {
                        3.4, 6, 8,
                        4, 55.6, 7
                },
                new double[] {
                        34, 4.56, 0,
                        65, 7, 7.9
                },
        };
        ImmutableDoubleArray expectedDoubles = new ImmutableDoubleArray(new double[] {
                3.4, 6, 8,
                4, 55.6, 7,

                34, 4.56, 0,
                65, 7, 7.9
        });
        DataBuffer dataBuffer = new DataBufferDouble(pixels, nChannels);
        Raster raster = WritableRaster.createWritableRaster(
                new BandedSampleModel(
                        dataBuffer.getDataType(),
                        width,
                        height,
                        nChannels
                ),
                dataBuffer,
                null
        );

        ImmutableDoubleArray doubles = BufferedImagePixelsExtractor.getDoubles(raster);

        Assertions.assertArrayEquals(expectedDoubles.getCurrentStorageArray(), doubles.getCurrentStorageArray());
    }
}
