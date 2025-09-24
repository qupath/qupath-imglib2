package qupath.ext.imglib2;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.img.cell.LazyCellImg;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.ShortType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import qupath.ext.imglib2.immutablearrays.ImmutableByteArray;
import qupath.ext.imglib2.immutablearrays.ImmutableDoubleArray;
import qupath.ext.imglib2.immutablearrays.ImmutableFloatArray;
import qupath.ext.imglib2.immutablearrays.ImmutableIntArray;
import qupath.ext.imglib2.immutablearrays.ImmutableShortArray;
import qupath.lib.images.servers.ImageServer;
import qupath.lib.images.servers.ImageServerMetadata;
import qupath.lib.images.servers.ServerTools;
import qupath.lib.images.servers.TileRequest;
import qupath.lib.images.servers.PixelType;

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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

/**
 * A class to create {@link Img} or {@link RandomAccessibleInterval} from an {@link ImageServer}.
 * <p>
 * The number of dimensions of accessibles created by this class will always be equal to the number of instances of {@link Dimension}.
 * See {@link #getIndexOfDimension(Dimension)} to find out the physical meaning of each dimension.
 * <p>
 * Warning: each accessible returned by this class is immutable. This means that any attempt to write data to them will either result in an
 * {@link UnsupportedOperationException} or be ignored.
 * <p>
 * Use a {@link Builder} to create an instance of this class.
 * <p>
 * This class is thread-safe.
 *
 * @param <T> the type of the returned accessibles
 * @param <A> the type contained in the input image
 */
public class ImgCreator<T extends NativeType<T> & NumericType<T>, A extends ArrayDataAccess<A>> {

    private final Map<Integer, LoadingCache<Long, Cell<A>>> caches = new ConcurrentHashMap<>();
    private final ImageServer<BufferedImage> server;
    private final T type;
    private final int cacheSizeMiB;
    private final int numberOfChannels;
    private final float inputTypeSizeMiB;

    private ImgCreator(Builder<T> builder) {
        this.server = builder.server;
        this.type = builder.type;
        this.cacheSizeMiB = builder.cacheSizeMiB;
        this.numberOfChannels = server.isRGB() ? 1 : server.nChannels();
        this.inputTypeSizeMiB = getInputTypeSizeBits(server) / 8f / (1024f * 1024f);
    }

    /**
     * Get the index of the provided dimension on accessibles returned by this class. The number of dimensions of the accessibles will always be
     * equal to the number of elements of {@link Dimension}.
     *
     * @param dimension the dimension whose index should be retrieved
     * @return the index of the provided dimension on accessibles returned by this class
     */
    public static int getIndexOfDimension(Dimension dimension) {
        return switch (dimension) {
            case X -> 0;
            case Y -> 1;
            case CHANNEL -> 2;
            case Z -> 3;
            case TIME -> 4;
        };
    }

    /**
     * Create an {@link Img} from the input image and the provided level. See the description of this class for more information.
     * <p>
     * Pixels of the returned image are lazily fetched.
     *
     * @param level the level to consider
     * @return an {@link Img} corresponding to the provided level of the input image
     * @throws IllegalArgumentException if the provided level does not match with a level of the input image
     */
    public Img<T> createForLevel(int level) {
        if (level < 0 || level >= server.getMetadata().nLevels()) {
            throw new IllegalArgumentException(String.format(
                    "The provided level %d is not within 0 and %d",
                    level,
                    server.getMetadata().nLevels() - 1
            ));
        }

        List<TileRequest> tiles = new ArrayList<>(server.getTileRequestManager().getTileRequestsForLevel(level));
        LoadingCache<Long, Cell<A>> cache = caches.computeIfAbsent(
                level,
                l -> Caffeine.newBuilder()
                        .weigher((Long index, Cell<A> cell) -> (int) (cell.getData().getArrayLength() * inputTypeSizeMiB))
                        .maximumWeight(cacheSizeMiB)
                        .softValues()
                        .build(i -> getCell(tiles.get(Math.toIntExact(i))))
        );

        return new LazyCellImg<>(
                new CellGrid(
                        new long[] {
                                server.getMetadata().getLevel(level).getWidth(),
                                server.getMetadata().getLevel(level).getHeight(),
                                numberOfChannels,
                                server.nZSlices(),
                                server.nTimepoints()
                        },
                        new int[] {
                                server.getMetadata().getPreferredTileWidth(),
                                server.getMetadata().getPreferredTileHeight(),
                                numberOfChannels,
                                1,
                                1
                        }
                ),
                type,
                cache::get
        );
    }

    /**
     * Create a {@link RandomAccessibleInterval} from the input image and the provided downsample. See the description of this
     * class for more information.
     * <p>
     * Values of the returned image are lazily fetched.
     * <p>
     * If the input image has to be scaled and its {@link ImageServerMetadata#getChannelType() channel type} is
     * {@link ImageServerMetadata.ChannelType#CLASSIFICATION}, then the nearest neighbor interpolation is used.
     * Otherwise, the linear interpolation is used.
     *
     * @param downsample the downsample to apply to the input image. Must be greater than 0
     * @return a {@link RandomAccessibleInterval} corresponding to the input image with the provided downsample applied
     * @throws IllegalArgumentException if the provided downsample is not greater than 0
     */
    public RandomAccessibleInterval<T> createForDownsample(double downsample) {
        if (downsample <= 0) {
            throw new IllegalArgumentException(String.format("The provided downsample %f is not greater than 0", downsample));
        }

        int level = ServerTools.getPreferredResolutionLevel(server, downsample);

        if (server.getMetadata().getChannelType() == ImageServerMetadata.ChannelType.CLASSIFICATION) {
            return AccessibleScaler.scaleWithNearestNeighborInterpolation(createForLevel(level), server.getDownsampleForResolution(level) / downsample);
        } else {
            return AccessibleScaler.scaleWithLinearInterpolation(createForLevel(level), server.getDownsampleForResolution(level) / downsample);
        }
    }

    /**
     * A builder to create an instance of {@link ImgCreator}.
     *
     * @param <T> the type of the returned accessibles of {@link ImgCreator} should have
     */
    public static class Builder<T extends NativeType<T> & NumericType<T>> {

        private final ImageServer<BufferedImage> server;
        private final T type;
        private int cacheSizeMiB = (int) (Runtime.getRuntime().maxMemory() * 0.5 / (1024 * 1024));

        /**
         * Create a builder from an {@link ImageServer}.
         * <p>
         * The type of the output image is not checked, which might lead to problems later when accessing pixel values of the
         * returned accessibles of this class. It is recommended to use {@link #Builder(ImageServer, NativeType)} instead.
         *
         * @param server the input image
         * @throws IllegalArgumentException if the provided image has less than one channel
         */
        public Builder(ImageServer<BufferedImage> server) {
            this(server, getTypeOfServer(server));
        }

        /**
         * Create a builder from an {@link ImageServer}. This doesn't create any accessibles yet.
         * <p>
         * The provided type must be compatible with the input image:
         * <ul>
         *     <li>If the input image is {@link ImageServer#isRGB() RGB}, the type must be {@link ARGBType}.</li>
         *     <li>
         *         Else:
         *         <ul>
         *             <li>
         *                 If the input image has the {@link PixelType#UINT8} {@link ImageServer#getPixelType() pixel type},
         *                 the type must be {@link UnsignedByteType}.
         *             </li>
         *             <li>
         *                 If the input image has the {@link PixelType#INT8} {@link ImageServer#getPixelType() pixel type},
         *                 the type must be {@link ByteType}.
         *             </li>
         *             <li>
         *                 If the input image has the {@link PixelType#UINT16} {@link ImageServer#getPixelType() pixel type},
         *                 the type must be {@link UnsignedShortType}.
         *             </li>
         *             <li>
         *                 If the input image has the {@link PixelType#INT16} {@link ImageServer#getPixelType() pixel type},
         *                 the type must be {@link ShortType}.
         *             </li>
         *             <li>
         *                 If the input image has the {@link PixelType#UINT32} {@link ImageServer#getPixelType() pixel type},
         *                 the type must be {@link UnsignedIntType}.
         *             </li>
         *             <li>
         *                 If the input image has the {@link PixelType#INT32} {@link ImageServer#getPixelType() pixel type},
         *                 the type must be {@link IntType}.
         *             </li>
         *             <li>
         *                 If the input image has the {@link PixelType#FLOAT32} {@link ImageServer#getPixelType() pixel type},
         *                 the type must be {@link FloatType}.
         *             </li>
         *             <li>
         *                 If the input image has the {@link PixelType#FLOAT64} {@link ImageServer#getPixelType() pixel type},
         *                 the type must be {@link DoubleType}.
         *             </li>
         *         </ul>
         *     </li>
         * </ul>
         *
         * @param server the input image
         * @param type the expected type of the output image
         * @throws IllegalArgumentException if the provided type is not compatible with the input image (see above), or if the provided image
         * has less than one channel
         */
        public Builder(ImageServer<BufferedImage> server, T type) {
            checkType(server, type);
            if (server.nChannels() <= 0) {
                throw new IllegalArgumentException(String.format("The provided image has less than one channel (%d)", server.nChannels()));
            }

            this.server = server;
            this.type = type;
        }

        /**
         * Accessibles returned by this class will be divided into cells, which will be cached to gain performance. This function sets the
         * maximal size of the cache in mebibyte (MiB). By default, half the amount of the {@link Runtime#maxMemory() max memory} is used.
         * <p>
         * Note that the actual size of the cache also depends on the current available memory. The cache will take up to the specified
         * space only there is enough available memory.
         *
         * @param cacheSizeMiB the maximal size of the cache in MiB
         * @return this builder
         */
        public Builder<T> cacheSizeMiB(int cacheSizeMiB) {
            this.cacheSizeMiB = cacheSizeMiB;
            return this;
        }

        /**
         * Build an instance of {@link ImgCreator}.
         *
         * @return a new instance of {@link ImgCreator}
         */
        public ImgCreator<T, ?> build() {
            return new ImgCreator<>(this);
        }

        @SuppressWarnings("unchecked")
        private static <T extends NativeType<T> & NumericType<T>> T getTypeOfServer(ImageServer<?> server) {
            if (server.isRGB()) {
                return (T) new ARGBType();
            }

            return switch (server.getPixelType()) {
                case UINT8 -> (T) new UnsignedByteType();
                case INT8 -> (T) new ByteType();
                case UINT16 -> (T) new UnsignedShortType();
                case INT16 -> (T) new ShortType();
                case UINT32 -> (T) new UnsignedIntType();
                case INT32 -> (T) new IntType();
                case FLOAT32 -> (T) new FloatType();
                case FLOAT64 -> (T) new DoubleType();
            };
        }

        private static <T> void checkType(ImageServer<?> server, T type) {
            if (server.isRGB()) {
                if (!(type instanceof ARGBType)) {
                    throw new IllegalArgumentException(String.format(
                            "The provided type %s is not an ARGBType, which is the one expected for RGB images",
                            type
                    ));
                }
            } else {
                switch (server.getPixelType()) {
                    case UINT8 -> {
                        if (!(type instanceof UnsignedByteType)) {
                            throw new IllegalArgumentException(String.format(
                                    "The provided type %s is not a ByteType, which is the one expected for non-RGB UINT8 images",
                                    type
                            ));
                        }
                    }
                    case INT8 -> {
                        if (!(type instanceof ByteType)) {
                            throw new IllegalArgumentException(String.format(
                                    "The provided type %s is not a UnsignedByteType, which is the one expected for non-RGB INT8 images",
                                    type
                            ));
                        }
                    }
                    case UINT16 -> {
                        if (!(type instanceof UnsignedShortType)) {
                            throw new IllegalArgumentException(String.format(
                                    "The provided type %s is not a UnsignedShortType, which is the one expected for non-RGB UINT16 images",
                                    type
                            ));
                        }
                    }
                    case INT16 -> {
                        if (!(type instanceof ShortType)) {
                            throw new IllegalArgumentException(String.format(
                                    "The provided type %s is not a ShortType, which is the one expected for non-RGB INT16 images",
                                    type
                            ));
                        }
                    }
                    case UINT32 -> {
                        if (!(type instanceof UnsignedIntType)) {
                            throw new IllegalArgumentException(String.format(
                                    "The provided type %s is not a UnsignedIntType, which is the one expected for non-RGB UINT32 images",
                                    type
                            ));
                        }
                    }
                    case INT32 -> {
                        if (!(type instanceof IntType)) {
                            throw new IllegalArgumentException(String.format(
                                    "The provided type %s is not a IntType, which is the one expected for non-RGB INT32 images",
                                    type
                            ));
                        }
                    }
                    case FLOAT32 -> {
                        if (!(type instanceof FloatType)) {
                            throw new IllegalArgumentException(String.format(
                                    "The provided type %s is not a FloatType, which is the one expected for non-RGB FLOAT32 images",
                                    type
                            ));
                        }
                    }
                    case FLOAT64 -> {
                        if (!(type instanceof DoubleType)) {
                            throw new IllegalArgumentException(String.format(
                                    "The provided type %s is not a DoubleType, which is the one expected for non-RGB FLOAT64 images",
                                    type
                            ));
                        }
                    }
                }
            }
        }
    }

    private static int getInputTypeSizeBits(ImageServer<?> server) {
        if (server.isRGB()) {
            return Integer.SIZE;
        } else {
            return switch (server.getPixelType()) {
                case UINT8, INT8 -> Byte.SIZE;
                case UINT16, INT16 -> Short.SIZE;
                case UINT32, INT32 -> Integer.SIZE;
                case FLOAT32 -> Float.SIZE;
                case FLOAT64 -> Double.SIZE;
            };
        }
    }

    private Cell<A> getCell(TileRequest tile) {
        BufferedImage image;
        try {
            image = server.readRegion(tile.getRegionRequest());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        A data;
        if (server.isRGB()) {
            data = getARGB(image);
        } else {
            data = switch (server.getPixelType()) {
                case UINT8, INT8 -> getBytes(image.getRaster());
                case UINT16, INT16 -> getShorts(image.getRaster());
                case UINT32, INT32 -> getIntegers(image.getRaster());
                case FLOAT32 -> getFloats(image.getRaster());
                case FLOAT64 -> getDoubles(image.getRaster());
            };
        }

        return new Cell<>(
                new int[]{ image.getWidth(), image.getHeight(), numberOfChannels, 1, 1 },
                new long[]{ tile.getTileX(), tile.getTileY(), 0, tile.getZ(), tile.getT()},
                data
        );
    }

    @SuppressWarnings("unchecked")
    private A getARGB(BufferedImage img) {
        int[] array;

        // Avoid calling img.getRGB() if possible, as this call makes a copy of the pixels
        if (img.getRaster().getDataBuffer() instanceof DataBufferInt dataBufferInt && img.getRaster().getSampleModel() instanceof SinglePixelPackedSampleModel) {
            array = dataBufferInt.getBankData()[0];    // SinglePixelPackedSampleModel contains all data array elements in the first bank of the DataBuffer
        } else {
            array = img.getRGB(0, 0, img.getWidth(), img.getHeight(), null, 0, img.getWidth());
        }

        return (A) new ImmutableIntArray(array);
    }

    @SuppressWarnings("unchecked")
    private A getBytes(Raster raster) {
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

        return (A) new ImmutableByteArray(array);
    }

    @SuppressWarnings("unchecked")
    private A getShorts(Raster raster) {
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

        return (A) new ImmutableShortArray(array);
    }

    @SuppressWarnings("unchecked")
    private A getIntegers(Raster raster) {
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

        return (A) new ImmutableIntArray(array);
    }

    @SuppressWarnings("unchecked")
    private A getFloats(Raster raster) {
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

        return (A) new ImmutableFloatArray(array);
    }

    @SuppressWarnings("unchecked")
    private A getDoubles(Raster raster) {
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

        return (A) new ImmutableDoubleArray(array);
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
