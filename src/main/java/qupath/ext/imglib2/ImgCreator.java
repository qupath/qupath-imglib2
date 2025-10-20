package qupath.ext.imglib2;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
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
import qupath.ext.imglib2.bufferedimageaccesses.ArgbBufferedImageAccess;
import qupath.ext.imglib2.bufferedimageaccesses.ByteRasterAccess;
import qupath.ext.imglib2.bufferedimageaccesses.DoubleRasterAccess;
import qupath.ext.imglib2.bufferedimageaccesses.FloatRasterAccess;
import qupath.ext.imglib2.bufferedimageaccesses.IntRasterAccess;
import qupath.ext.imglib2.bufferedimageaccesses.ShortRasterAccess;
import qupath.lib.color.ColorModelFactory;
import qupath.lib.images.servers.ImageChannel;
import qupath.lib.images.servers.ImageServer;
import qupath.lib.images.servers.ImageServerMetadata;
import qupath.lib.images.servers.ServerTools;
import qupath.lib.images.servers.TileRequest;
import qupath.lib.images.servers.PixelType;

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
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * A class to create {@link Img} or {@link RandomAccessibleInterval} from an {@link ImageServer}.
 * <p>
 * Warning: each accessible returned by this class is immutable. This means that any attempt to write data to them will either result in an
 * {@link UnsupportedOperationException}.
 * <p>
 * If the input server is RGB, then any {@link Img} or {@link RandomAccessibleInterval} created from it will have only one channel.
 * <p>
 * Use a {@link #builder(ImageServer)} or {@link #builder(ImageServer, NativeType)} to create an instance of this class.
 * <p>
 * This class is thread-safe.
 *
 * @param <T> the type of the returned accessibles
 * @param <A> the type contained in the input image
 */
public class ImgCreator<T extends NativeType<T> & NumericType<T>, A extends SizableDataAccess> {

    private final ImageServer<BufferedImage> server;
    private final T type;
    private final CellCache cellCache;
    private final Function<BufferedImage, A> cellCreator;
    private final int numberOfChannels;

    private ImgCreator(Builder<T> builder, Function<BufferedImage, A> cellCreator) {
        this.server = builder.server;
        this.type = builder.type;
        this.cellCache = builder.cellCache;
        this.cellCreator = cellCreator;
        this.numberOfChannels = server.isRGB() ? 1 : server.nChannels();
    }

    /**
     * Create a builder from an {@link ImageServer}. This doesn't create any accessibles yet.
     * <p>
     * The type of the output image is not checked, which might lead to problems later when accessing pixel values of the
     * returned accessibles of this class. It is recommended to use {@link #builder(ImageServer, NativeType)} instead.
     *
     * @param server the input image
     * @return a builder to create an instance of this class
     * @param <T> the type of the output image
     * @throws IllegalArgumentException if the provided image has less than one channel
     */
    public static <T extends NativeType<T> & NumericType<T>> Builder<T> builder(ImageServer<BufferedImage> server) {
        // Despite the potential warning, T is necessary, otherwise a cannot infer type arguments error occurs
        return new Builder<T>(server, getTypeOfServer(server));
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
     * @return a builder to create an instance of this class
     * @param <T> the type corresponding to the provided image
     * @throws IllegalArgumentException if the provided type is not compatible with the input image (see above), or if the provided image
     * has less than one channel
     */
    public static <T extends NativeType<T> & NumericType<T>> Builder<T> builder(ImageServer<BufferedImage> server, T type) {
        return new Builder<>(server, type);
    }

    /**
     * Get the index of the provided dimension on accessibles returned by {@link #createForLevel(int)}} or {@link #createForDownsample(double)}.
     * The number of dimensions of these accessibles will always be equal to the number of elements of {@link Dimension}.
     *
     * @param dimension the dimension whose index should be retrieved
     * @return the index of the provided dimension on accessibles returned by {@link #createForLevel(int)} or {@link #createForDownsample(double)}
     */
    public static int getIndexOfDimension(Dimension dimension) {
        return getIndexOfDimension(dimension, List.of());
    }

    /**
     * Get the index of the provided dimension on accessibles returned by {@link #createForLevel(int, Map)}} or {@link #createForDownsample(double, Map)}.
     * The number of dimensions of these accessibles will always be equal to the number of elements of {@link Dimension} minus the number of dropped dimensions
     * given to {@link #createForLevel(int, Map)} or {@link #createForDownsample(double, Map)}.
     *
     * @param dimension the dimension whose index should be retrieved
     * @param droppedDimensions a collection of dimensions that shouldn't be considered. Must not contain the provided dimension
     * @return the index of the provided dimension on accessibles returned by {@link #createForLevel(int, Map)} or {@link #createForDownsample(double, Map)}
     * @throws IllegalArgumentException if the provided collection of dropped dimensions contains the provided dimension
     */
    public static int getIndexOfDimension(Dimension dimension, Collection<Dimension> droppedDimensions) {
        if (droppedDimensions.contains(dimension)) {
            throw new IllegalArgumentException(String.format("The provided dimension %s is among the list of dropped dimensions %s", dimension, droppedDimensions));
        }

        Dimension[] values = Dimension.values();
        int dimensionIndex = dimension.ordinal();

        int indexesToRemove = 0;
        for (int i=0; i<dimensionIndex; i++) {
            if (droppedDimensions.contains(values[i])) {
                indexesToRemove++;
            }
        }

        return dimensionIndex - indexesToRemove;
    }

    /**
     * Create an {@link Img} from the input image and the provided level.
     * <p>
     * See {@link #getIndexOfDimension(Dimension)} to get information on the dimensions of the returned accessible.
     * <p>
     * See the description of this class for more information on the returned accessible.
     * <p>
     * Pixels of the returned image are lazily fetched.
     *
     * @param level the level to consider
     * @return an {@link Img} corresponding to the provided level of the input image
     * @throws IllegalArgumentException if the provided level does not match with a level of the input image
     */
    public Img<T> createForLevel(int level) {
        return createForLevel(level, Map.of());
    }

    /**
     * Create an {@link Img} from the input image, the provided level and by dropping some dimensions.
     * <p>
     * See {@link #getIndexOfDimension(Dimension, Collection)} to get information on the dimensions of the returned accessible.
     * <p>
     * See the description of this class for more information on the returned accessible.
     * <p>
     * Pixels of the returned image are lazily fetched.
     *
     * @param level the level to consider
     * @param dimensionsToDrop a map containing dimensions that won't be present in the output image. Each entry of the map should
     *                         represent a dimension to drop and the index of this dimension to keep. For example, to make a projection
     *                         on the first channel, use Map.of(Dimension.CHANNEL, 0). Dimension.X and Dimension.Y cannot be specified
     *                         here. Values of entries of this map cannot be out of bound (e.g. if Map.of(Dimension.CHANNEL, c) is provided,
     *                         c must be lower than the number of channels of the image)
     * @return an {@link Img} corresponding to the provided level and dimensions of the input image
     * @throws IllegalArgumentException if the provided level does not match with a level of the input image, if Dimension.X or Dimension.Y
     * is in the provided map of dimensions to drop, or if a value of one entry of the provided dimensions to drop is out of bound
     */
    public Img<T> createForLevel(int level, Map<Dimension, Integer> dimensionsToDrop) {
        if (level < 0 || level >= server.getMetadata().nLevels()) {
            throw new IllegalArgumentException(String.format(
                    "The provided level %d is not within 0 and %d",
                    level,
                    server.getMetadata().nLevels() - 1
            ));
        }
        for (var entry: dimensionsToDrop.entrySet()) {
            checkProjection(Dimension.CHANNEL, numberOfChannels, entry.getKey(), entry.getValue());
            checkProjection(Dimension.Z, server.getMetadata().getSizeZ(), entry.getKey(), entry.getValue());
            checkProjection(Dimension.TIME, server.getMetadata().getSizeT(), entry.getKey(), entry.getValue());
        }

        Stream<TileRequest> tilesStream = server.getTileRequestManager().getTileRequestsForLevel(level).stream();
        for (var entry: dimensionsToDrop.entrySet()) {
            tilesStream = switch (entry.getKey()) {
                case X -> throw new IllegalArgumentException(String.format("The provided dimensions to drop %s contain the X dimension", dimensionsToDrop));
                case Y -> throw new IllegalArgumentException(String.format("The provided dimensions to drop %s contain the Y dimension", dimensionsToDrop));
                case CHANNEL -> tilesStream;
                case Z -> tilesStream.filter(tileRequest -> tileRequest.getZ() == entry.getValue());
                case TIME -> tilesStream.filter(tileRequest -> tileRequest.getT() == entry.getValue());
            };
        }
        List<TileRequest> tiles = tilesStream.toList();

        return new LazyCellImg<>(
                new CellGrid(
                        getLongQuantityForEachDimension(
                                dimensionsToDrop.keySet(),
                                dimension -> switch (dimension) {
                                    case X -> (long) server.getMetadata().getLevel(level).getWidth();
                                    case Y -> (long) server.getMetadata().getLevel(level).getHeight();
                                    case CHANNEL -> (long) numberOfChannels;
                                    case Z -> (long) server.nZSlices();
                                    case TIME -> (long) server.nTimepoints();
                                }
                        ),
                        getIntQuantityForEachDimension(
                                dimensionsToDrop.keySet(),
                                dimension -> switch (dimension) {
                                    case X -> server.getMetadata().getPreferredTileWidth();
                                    case Y -> server.getMetadata().getPreferredTileHeight();
                                    case CHANNEL -> numberOfChannels;
                                    case Z, TIME -> 1;
                                }
                        )
                ),
                type,
                cellIndex -> cellCache.getCell(
                        tiles.get(Math.toIntExact(cellIndex)),
                        tileRequest -> createCell(tileRequest, dimensionsToDrop)
                )
        );
    }

    /**
     * Create a {@link RandomAccessibleInterval} from the input image and the provided downsample.
     * <p>
     * See {@link #getIndexOfDimension(Dimension)} to get information on the dimensions of the returned accessible.
     * <p>
     * See the description of this class for more information on the returned accessible.
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
        return createForDownsample(downsample, Map.of());
    }

    /**
     * Create a {@link RandomAccessibleInterval} from the input image, the provided downsample, and by dropping some dimensions.
     * <p>
     * See {@link #getIndexOfDimension(Dimension, Collection)} to get information on the dimensions of the returned accessible.
     * <p>
     * See the description of this class for more information on the returned accessible.
     * <p>
     * Values of the returned image are lazily fetched.
     * <p>
     * If the input image has to be scaled and its {@link ImageServerMetadata#getChannelType() channel type} is
     * {@link ImageServerMetadata.ChannelType#CLASSIFICATION}, then the nearest neighbor interpolation is used.
     * Otherwise, the linear interpolation is used.
     *
     * @param downsample the downsample to apply to the input image. Must be greater than 0
     * @param dimensionsToDrop a map containing dimensions that won't be present in the output image. Each entry of the map should
     *                         represent a dimension to drop and the index of this dimension to keep. For example, to make a projection
     *                         on the first channel, use Map.of(Dimension.CHANNEL, 0). Dimension.X and Dimension.Y cannot be specified
     *                         here
     * @return a {@link RandomAccessibleInterval} corresponding to the input image with the provided downsample and projections applied
     * @throws IllegalArgumentException if the provided downsample is not greater than 0, if Dimension.X or Dimension.Y is in the
     * provided map of dimensions to drop, or if a value of one entry of the provided dimensions to drop is out of bound
     */
    public RandomAccessibleInterval<T> createForDownsample(double downsample, Map<Dimension, Integer> dimensionsToDrop) {
        if (downsample <= 0) {
            throw new IllegalArgumentException(String.format("The provided downsample %f is not greater than 0", downsample));
        }

        int level = ServerTools.getPreferredResolutionLevel(server, downsample);

        if (server.getMetadata().getChannelType() == ImageServerMetadata.ChannelType.CLASSIFICATION) {
            return AccessibleScaler.scaleWithNearestNeighborInterpolation(
                    createForLevel(level, dimensionsToDrop), server.getDownsampleForResolution(level) / downsample
            );
        } else {
            return AccessibleScaler.scaleWithLinearInterpolation(
                    createForLevel(level, dimensionsToDrop), server.getDownsampleForResolution(level) / downsample
            );
        }
    }

    /**
     * A builder to create an instance of {@link ImgCreator}.
     *
     * @param <T> the type of the returned accessibles of {@link ImgCreator} should have
     */
    public static class Builder<T extends NativeType<T> & NumericType<T>> {

        private static final CellCache defaultCellCache = new CellCache((int) (Runtime.getRuntime().maxMemory() * 0.5 / (1024 * 1024)));
        private final ImageServer<BufferedImage> server;
        private final T type;
        private CellCache cellCache = defaultCellCache;

        private Builder(ImageServer<BufferedImage> server, T type) {
            checkType(server, type);
            if (server.nChannels() <= 0) {
                throw new IllegalArgumentException(String.format("The provided image has less than one channel (%d)", server.nChannels()));
            }

            this.server = server;
            this.type = type;
        }

        /**
         * Accessibles returned by this class will be divided into cells, which will be cached to gain performance. This function sets the
         * cache to use. By default, a static cache of maximal size half the amount of the {@link Runtime#maxMemory() max memory} is used.
         *
         * @param cellCache the cache to use
         * @return this builder
         * @throws NullPointerException if the provided cache is null
         */
        public Builder<T> cellCache(CellCache cellCache) {
            this.cellCache = Objects.requireNonNull(cellCache);
            return this;
        }

        /**
         * Build an instance of {@link ImgCreator}.
         *
         * @return a new instance of {@link ImgCreator}
         */
        public ImgCreator<T, ?> build() {
            if (server.isRGB()) {
                return new ImgCreator<>(this, ArgbBufferedImageAccess::new);
            } else {
                return switch (server.getPixelType()) {
                    case UINT8, INT8 -> new ImgCreator<>(this, image -> new ByteRasterAccess(image.getRaster()));
                    case UINT16, INT16 -> new ImgCreator<>(this, image -> new ShortRasterAccess(image.getRaster()));
                    case UINT32, INT32 -> new ImgCreator<>(this, image -> new IntRasterAccess(image.getRaster()));
                    case FLOAT32 -> new ImgCreator<>(this, image -> new FloatRasterAccess(image.getRaster()));
                    case FLOAT64 -> new ImgCreator<>(this, image -> new DoubleRasterAccess(image.getRaster()));
                };
            }
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

    private static void checkProjection(Dimension dimensionToCheck, int maxExclusiveNumberOfElements, Dimension dimensionToProject, int projectionIndex) {
        if (dimensionToProject.equals(dimensionToCheck) && projectionIndex >= maxExclusiveNumberOfElements) {
            throw new IllegalArgumentException(String.format(
                    "A projection on element %d of the %s axis was requested, but the image only has %d elements on this axis",
                    projectionIndex,
                    dimensionToCheck,
                    maxExclusiveNumberOfElements
            ));
        }
    }

    private static int[] getIntQuantityForEachDimension(Collection<Dimension> droppedDimensions, Function<Dimension, Integer> quantityCreator) {
        List<Dimension> dimensionsToKeep = Arrays.stream(Dimension.values())
                .filter(dimension -> !droppedDimensions.contains(dimension))
                .toList();
        int[] quantities = new int[dimensionsToKeep.size()];

        for (Dimension dimension : dimensionsToKeep) {
            int indexOfDimension = getIndexOfDimension(dimension, droppedDimensions);

            quantities[indexOfDimension] = quantityCreator.apply(dimension);
        }

        return quantities;
    }

    private static long[] getLongQuantityForEachDimension(Collection<Dimension> droppedDimensions, Function<Dimension, Long> quantityCreator) {
        List<Dimension> dimensionsToKeep = Arrays.stream(Dimension.values())
                .filter(dimension -> !droppedDimensions.contains(dimension))
                .toList();
        long[] quantities = new long[dimensionsToKeep.size()];

        for (Dimension dimension : dimensionsToKeep) {
            int indexOfDimension = getIndexOfDimension(dimension, droppedDimensions);

            quantities[indexOfDimension] = quantityCreator.apply(dimension);
        }

        return quantities;
    }

    private Cell<A> createCell(TileRequest tile, Map<Dimension, Integer> dimensionsToDrop) {
        BufferedImage image;
        try {
            image = server.readRegion(tile.getRegionRequest());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        int channelIndex = dimensionsToDrop.getOrDefault(Dimension.CHANNEL, -1);
        BufferedImage projectedImage;
        if (channelIndex > -1 && image.getRaster().getNumBands() > 1) {
            projectedImage = projectImageOnChannelAxis(image, channelIndex);
        } else {
            projectedImage = image;
        }

        return new Cell<>(
                getIntQuantityForEachDimension(
                        dimensionsToDrop.keySet(),
                        dimension -> switch (dimension) {
                            case X -> projectedImage.getWidth();
                            case Y -> projectedImage.getHeight();
                            case CHANNEL -> numberOfChannels;
                            case Z, TIME -> 1;
                        }
                ),
                getLongQuantityForEachDimension(
                        dimensionsToDrop.keySet(),
                        dimension -> switch (dimension) {
                            case X -> (long) tile.getTileX();
                            case Y -> (long) tile.getTileY();
                            case CHANNEL -> 0L;
                            case Z -> (long) tile.getZ();
                            case TIME -> (long) tile.getT();
                        }
                ),
                cellCreator.apply(image)
        );
    }

    private BufferedImage projectImageOnChannelAxis(BufferedImage image, int channelIndex) {
        DataBuffer dataBuffer = switch (image.getRaster().getDataBuffer()) {
            case DataBufferByte dataBufferByte -> {
                byte[] data = dataBufferByte.getData(channelIndex);
                yield new DataBufferByte(data, data.length);
            }
            case DataBufferShort dataBufferShort -> {
                short[] data = dataBufferShort.getData(channelIndex);
                yield new DataBufferShort(data, data.length);
            }
            case DataBufferUShort dataBufferUShort -> {
                short[] data = dataBufferUShort.getData(channelIndex);
                yield new DataBufferUShort(data, data.length);
            }
            case DataBufferInt dataBufferInt -> {
                int[] data = dataBufferInt.getData(channelIndex);
                yield new DataBufferInt(data, data.length);
            }
            case DataBufferFloat dataBufferFloat -> {
                float[] data = dataBufferFloat.getData(channelIndex);
                yield new DataBufferFloat(data, data.length);
            }
            case DataBufferDouble dataBufferDouble -> {
                double[] data = dataBufferDouble.getData(channelIndex);
                yield new DataBufferDouble(data, data.length);
            }
            default -> throw new IllegalArgumentException(String.format("Unexpected data buffer %s ", image.getRaster().getDataBuffer()));
        };

        return new BufferedImage(
                ColorModelFactory.createColorModel(server.getPixelType(), List.of(ImageChannel.RED)),
                WritableRaster.createWritableRaster(
                        new BandedSampleModel(
                                dataBuffer.getDataType(),
                                image.getWidth(),
                                image.getHeight(),
                                1
                        ),
                        dataBuffer,
                        null
                ),
                false,
                null
        );
    }
}
