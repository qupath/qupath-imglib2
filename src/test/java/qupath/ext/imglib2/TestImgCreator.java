package qupath.ext.imglib2;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.ShortType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import qupath.lib.color.ColorModelFactory;
import qupath.lib.common.ColorTools;
import qupath.lib.images.servers.AbstractImageServer;
import qupath.lib.images.servers.ImageChannel;
import qupath.lib.images.servers.ImageServer;
import qupath.lib.images.servers.ImageServerBuilder;
import qupath.lib.images.servers.ImageServerMetadata;
import qupath.lib.images.servers.PixelType;
import qupath.lib.regions.RegionRequest;

import java.awt.image.BandedSampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferDouble;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.WritableRaster;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TestImgCreator {

    @Test
    void Check_Rgb_Server() throws Exception {
        boolean isRgb = true;
        PixelType pixelType = PixelType.UINT8;
        ImageServer<BufferedImage> imageServer = new GenericImageServer(isRgb, pixelType);

        Img<ARGBType> img = ImgCreator.builder(imageServer, new ARGBType()).build().createForLevel(0);

        Utils.assertArgbRandomAccessibleEquals(img, (x, y, channel, z, t) -> ARGBType.rgba(255, 0, 0, 0), 1);

        imageServer.close();
    }

    @Test
    void Check_Uint8_Server() throws Exception {
        boolean isRgb = false;
        PixelType pixelType = PixelType.UINT8;
        ImageServer<BufferedImage> imageServer = new GenericImageServer(isRgb, pixelType);

        Img<UnsignedByteType> img = ImgCreator.builder(imageServer, new UnsignedByteType()).build().createForLevel(0);

        Utils.assertRandomAccessibleEquals(img, (x, y, channel, z, t) -> 1, 1);

        imageServer.close();
    }

    @Test
    void Check_Int8_Server() throws Exception {
        boolean isRgb = false;
        PixelType pixelType = PixelType.INT8;
        ImageServer<BufferedImage> imageServer = new GenericImageServer(isRgb, pixelType);

        Img<ByteType> img = ImgCreator.builder(imageServer, new ByteType()).build().createForLevel(0);

        Utils.assertRandomAccessibleEquals(img, (x, y, channel, z, t) -> 1, 1);

        imageServer.close();
    }

    @Test
    void Check_Uint16_Server() throws Exception {
        boolean isRgb = false;
        PixelType pixelType = PixelType.UINT16;
        ImageServer<BufferedImage> imageServer = new GenericImageServer(isRgb, pixelType);

        Img<UnsignedShortType> img = ImgCreator.builder(imageServer, new UnsignedShortType()).build().createForLevel(0);

        Utils.assertRandomAccessibleEquals(img, (x, y, channel, z, t) -> 1, 1);

        imageServer.close();
    }

    @Test
    void Check_Int16_Server() throws Exception {
        boolean isRgb = false;
        PixelType pixelType = PixelType.INT16;
        ImageServer<BufferedImage> imageServer = new GenericImageServer(isRgb, pixelType);

        Img<ShortType> img = ImgCreator.builder(imageServer, new ShortType()).build().createForLevel(0);

        Utils.assertRandomAccessibleEquals(img, (x, y, channel, z, t) -> 1, 1);

        imageServer.close();
    }

    @Test
    void Check_Uint32_Server() throws Exception {
        boolean isRgb = false;
        PixelType pixelType = PixelType.UINT32;
        ImageServer<BufferedImage> imageServer = new GenericImageServer(isRgb, pixelType);

        Img<UnsignedIntType> img = ImgCreator.builder(imageServer, new UnsignedIntType()).build().createForLevel(0);

        Utils.assertRandomAccessibleEquals(img, (x, y, channel, z, t) -> 1, 1);

        imageServer.close();
    }

    @Test
    void Check_Int32_Server() throws Exception {
        boolean isRgb = false;
        PixelType pixelType = PixelType.INT32;
        ImageServer<BufferedImage> imageServer = new GenericImageServer(isRgb, pixelType);

        Img<IntType> img = ImgCreator.builder(imageServer, new IntType()).build().createForLevel(0);

        Utils.assertRandomAccessibleEquals(img, (x, y, channel, z, t) -> 1, 1);

        imageServer.close();
    }

    @Test
    void Check_Float32_Server() throws Exception {
        boolean isRgb = false;
        PixelType pixelType = PixelType.FLOAT32;
        ImageServer<BufferedImage> imageServer = new GenericImageServer(isRgb, pixelType);

        Img<FloatType> img = ImgCreator.builder(imageServer, new FloatType()).build().createForLevel(0);

        Utils.assertRandomAccessibleEquals(img, (x, y, channel, z, t) -> 1, 1);

        imageServer.close();
    }

    @Test
    void Check_Float64_Server() throws Exception {
        boolean isRgb = false;
        PixelType pixelType = PixelType.FLOAT64;
        ImageServer<BufferedImage> imageServer = new GenericImageServer(isRgb, pixelType);

        Img<DoubleType> img = ImgCreator.builder(imageServer, new DoubleType()).build().createForLevel(0);

        Utils.assertRandomAccessibleEquals(img, (x, y, channel, z, t) -> 1, 1);

        imageServer.close();
    }

    @Test
    void Check_X_Dimension_Size() throws Exception {
        ImageServer<BufferedImage> imageServer = new ComplexDoubleImageServer();
        Dimension dimension = Dimension.X;
        int expectedSize = imageServer.getWidth();
        Img<?> img = ImgCreator.builder(imageServer).build().createForLevel(0);
        int dimensionIndex = ImgCreator.getIndexOfDimension(dimension);

        Assertions.assertEquals(expectedSize, img.dimension(dimensionIndex));

        imageServer.close();
    }

    @Test
    void Check_Y_Dimension_Size() throws Exception {
        ImageServer<BufferedImage> imageServer = new ComplexDoubleImageServer();
        Dimension dimension = Dimension.Y;
        int expectedSize = imageServer.getHeight();
        Img<?> img = ImgCreator.builder(imageServer).build().createForLevel(0);
        int dimensionIndex = ImgCreator.getIndexOfDimension(dimension);

        Assertions.assertEquals(expectedSize, img.dimension(dimensionIndex));

        imageServer.close();
    }

    @Test
    void Check_Channel_Dimension_Size() throws Exception {
        ImageServer<BufferedImage> imageServer = new ComplexDoubleImageServer();
        Dimension dimension = Dimension.CHANNEL;
        int expectedSize = imageServer.nChannels();
        Img<?> img = ImgCreator.builder(imageServer).build().createForLevel(0);
        int dimensionIndex = ImgCreator.getIndexOfDimension(dimension);

        Assertions.assertEquals(expectedSize, img.dimension(dimensionIndex));

        imageServer.close();
    }

    @Test
    void Check_Z_Dimension_Size() throws Exception {
        ImageServer<BufferedImage> imageServer = new ComplexDoubleImageServer();
        Dimension dimension = Dimension.Z;
        int expectedSize = imageServer.getMetadata().getSizeZ();
        Img<?> img = ImgCreator.builder(imageServer).build().createForLevel(0);
        int dimensionIndex = ImgCreator.getIndexOfDimension(dimension);

        Assertions.assertEquals(expectedSize, img.dimension(dimensionIndex));

        imageServer.close();
    }

    @Test
    void Check_Time_Dimension_Size() throws Exception {
        ImageServer<BufferedImage> imageServer = new ComplexDoubleImageServer();
        Dimension dimension = Dimension.TIME;
        int expectedSize = imageServer.getMetadata().getSizeT();
        Img<?> img = ImgCreator.builder(imageServer).build().createForLevel(0);
        int dimensionIndex = ImgCreator.getIndexOfDimension(dimension);

        Assertions.assertEquals(expectedSize, img.dimension(dimensionIndex));

        imageServer.close();
    }

    @Test
    void Check_Pixels_Of_Level_0() throws Exception {
        int level = 0;
        ImageServer<BufferedImage> imageServer = new ComplexDoubleImageServer();
        double downsample = imageServer.getDownsampleForResolution(level);

        Img<DoubleType> img = ImgCreator.builder(imageServer, new DoubleType()).build().createForLevel(level);

        Utils.assertRandomAccessibleEquals(img, ComplexDoubleImageServer::getPixel, downsample);

        imageServer.close();
    }

    @Test
    void Check_Pixels_Of_Level_1() throws Exception {
        int level = 1;
        ImageServer<BufferedImage> imageServer = new ComplexDoubleImageServer();
        double downsample = imageServer.getDownsampleForResolution(level);

        Img<DoubleType> img = ImgCreator.builder(imageServer, new DoubleType()).build().createForLevel(level);

        Utils.assertRandomAccessibleEquals(img, ComplexDoubleImageServer::getPixel, downsample);

        imageServer.close();
    }

    @Test
    void Check_Pixels_Of_Downsample_1() throws Exception {
        int level = 0;
        ImageServer<BufferedImage> imageServer = new ComplexDoubleImageServer();
        double downsample = imageServer.getDownsampleForResolution(level);

        RandomAccessibleInterval<DoubleType> img = ImgCreator.builder(imageServer, new DoubleType()).build().createForDownsample(downsample);

        Utils.assertRandomAccessibleEquals(img, ComplexDoubleImageServer::getPixel, downsample);

        imageServer.close();
    }

    @Test
    void Check_Pixels_Of_Downsample_4() throws Exception {
        int level = 1;
        ImageServer<BufferedImage> imageServer = new ComplexDoubleImageServer();
        double downsample = imageServer.getDownsampleForResolution(level);

        RandomAccessibleInterval<DoubleType> img = ImgCreator.builder(imageServer, new DoubleType()).build().createForDownsample(downsample);

        Utils.assertRandomAccessibleEquals(img, ComplexDoubleImageServer::getPixel, downsample);

        imageServer.close();
    }

    private static class GenericImageServer extends AbstractImageServer<BufferedImage> {

        private static final AtomicInteger counter = new AtomicInteger(0);
        private final ImageServerMetadata metadata;
        private final String id;

        public GenericImageServer(boolean isRgb, PixelType pixelType) {
            super(BufferedImage.class);

            this.metadata = new ImageServerMetadata.Builder()
                    .width(1)
                    .height(1)
                    .channels(List.of(ImageChannel.RED))
                    .rgb(isRgb)
                    .pixelType(pixelType)
                    .build();

            // Each test uses the same cache, so each created server must have different IDs
            this.id = String.format("Generic server %d", counter.incrementAndGet());
        }

        @Override
        protected ImageServerBuilder.ServerBuilder<BufferedImage> createServerBuilder() {
            return null;
        }

        @Override
        protected String createID() {
            return id;
        }

        @Override
        public Collection<URI> getURIs() {
            return List.of();
        }

        @Override
        public BufferedImage readRegion(RegionRequest request) {
            if (getMetadata().isRGB()) {
                BufferedImage image = new BufferedImage(request.getWidth(), request.getHeight(), BufferedImage.TYPE_INT_ARGB);
                int[] rgbArray = new int[request.getWidth() * request.getHeight()];
                Arrays.fill(rgbArray, ARGBType.rgba(255, 0, 0, 0));
                image.setRGB(0, 0, request.getWidth(), request.getHeight(), rgbArray, 0, request.getWidth());
                return image;
            } else {
                DataBuffer dataBuffer = createDataBuffer(request);

                return new BufferedImage(
                        ColorModelFactory.createColorModel(getMetadata().getPixelType(), getMetadata().getChannels()),
                        WritableRaster.createWritableRaster(
                                new BandedSampleModel(
                                        dataBuffer.getDataType(),
                                        request.getWidth(),
                                        request.getHeight(),
                                        nChannels()
                                ),
                                dataBuffer,
                                null
                        ),
                        false,
                        null
                );
            }
        }

        @Override
        public String getServerType() {
            return "";
        }

        @Override
        public ImageServerMetadata getOriginalMetadata() {
            return metadata;
        }

        private DataBuffer createDataBuffer(RegionRequest request) {
            return switch (getMetadata().getPixelType()) {
                case UINT8, INT8 -> {
                    byte[][] array = new byte[nChannels()][];

                    for (int c = 0; c < array.length; c++) {
                        array[c] = new byte[request.getWidth() * request.getHeight()];
                        Arrays.fill(array[c], (byte) 1);
                    }

                    yield new DataBufferByte(array, array[0].length);
                }
                case UINT16, INT16 -> {
                    short[][] array = new short[nChannels()][];

                    for (int c = 0; c < array.length; c++) {
                        array[c] = new short[request.getWidth() * request.getHeight()];
                        Arrays.fill(array[c], (short) 1);
                    }

                    yield getMetadata().getPixelType().equals(PixelType.UINT16) ?
                            new DataBufferUShort(array, array[0].length) :
                            new DataBufferShort(array, array[0].length);
                }
                case UINT32, INT32 -> {
                    int[][] array = new int[nChannels()][];

                    for (int c = 0; c < array.length; c++) {
                        array[c] = new int[request.getWidth() * request.getHeight()];
                        Arrays.fill(array[c], 1);
                    }

                    yield new DataBufferInt(array, array[0].length);
                }
                case FLOAT32 -> {
                    float[][] array = new float[nChannels()][];

                    for (int c = 0; c < array.length; c++) {
                        array[c] = new float[request.getWidth() * request.getHeight()];
                        Arrays.fill(array[c], 1);
                    }

                    yield new DataBufferFloat(array, array[0].length);
                }
                case FLOAT64 -> {
                    double[][] array = new double[nChannels()][];

                    for (int c = 0; c < array.length; c++) {
                        array[c] = new double[request.getWidth() * request.getHeight()];
                        Arrays.fill(array[c], 1);
                    }

                    yield new DataBufferDouble(array, array[0].length);
                }
            };
        }
    }

    private static class ComplexDoubleImageServer extends AbstractImageServer<BufferedImage> {

        private static final int IMAGE_WIDTH = 64;
        private static final int IMAGE_HEIGHT = 64;
        private static final ImageServerMetadata metadata = new ImageServerMetadata.Builder()
                .width(IMAGE_WIDTH)
                .height(IMAGE_HEIGHT)
                .sizeZ(3)
                .sizeT(2)
                .pixelType(PixelType.FLOAT64)
                .channels(List.of(
                        ImageChannel.getInstance("c1", ColorTools.CYAN),
                        ImageChannel.getInstance("c2", ColorTools.BLUE),
                        ImageChannel.getInstance("c3", ColorTools.RED),
                        ImageChannel.getInstance("c4", ColorTools.GREEN),
                        ImageChannel.getInstance("c5", ColorTools.MAGENTA)
                ))
                .levelsFromDownsamples(1, 4)
                .build();
        private static final AtomicInteger counter = new AtomicInteger(0);
        private final String id;

        public ComplexDoubleImageServer() {
            super(BufferedImage.class);

            // Each test uses the same cache, so each created server must have different IDs
            this.id = String.format("Complex double server %d", counter.incrementAndGet());
        }

        @Override
        protected ImageServerBuilder.ServerBuilder<BufferedImage> createServerBuilder() {
            return null;
        }

        @Override
        protected String createID() {
            return id;
        }

        @Override
        public Collection<URI> getURIs() {
            return List.of();
        }

        @Override
        public String getServerType() {
            return "";
        }

        @Override
        public ImageServerMetadata getOriginalMetadata() {
            return metadata;
        }

        @Override
        public BufferedImage readRegion(RegionRequest request) {
            DataBuffer dataBuffer = createDataBuffer(request);

            return new BufferedImage(
                    ColorModelFactory.createColorModel(getMetadata().getPixelType(), getMetadata().getChannels()),
                    WritableRaster.createWritableRaster(
                            new BandedSampleModel(
                                    dataBuffer.getDataType(),
                                    (int) (request.getWidth() / request.getDownsample()),
                                    (int) (request.getHeight() / request.getDownsample()),
                                    nChannels()
                            ),
                            dataBuffer,
                            null
                    ),
                    false,
                    null
            );
        }

        public static double getPixel(int x, int y, int channel, int z, int t) {
            return z + t + channel + ((double) x / IMAGE_WIDTH + (double) y / IMAGE_HEIGHT) / 2;
        }

        private DataBuffer createDataBuffer(RegionRequest request) {
            double[][] array = new double[nChannels()][];

            for (int c = 0; c < array.length; c++) {
                array[c] = getPixels(request, c);
            }

            return new DataBufferDouble(array, (int) ((double) (request.getWidth() * request.getHeight()) / (request.getDownsample() * request.getDownsample())));
        }

        private double[] getPixels(RegionRequest request, int channel) {
            int width = (int) (request.getWidth() / request.getDownsample());
            int height = (int) (request.getHeight() / request.getDownsample());
            double[] pixels = new double[width * height];

            for (int y=0; y<height; y++) {
                for (int x=0; x<width; x++) {
                    pixels[y*width + x] = getPixel(
                            (int) ((x + request.getX()) * request.getDownsample()),
                            (int) ((y + request.getY()) * request.getDownsample()),
                            channel,
                            request.getZ(),
                            request.getT()
                    );
                }
            }

            return pixels;
        }
    }
}
