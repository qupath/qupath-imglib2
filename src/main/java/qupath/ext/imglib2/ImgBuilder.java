package qupath.ext.imglib2;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.img.cell.LazyCellImg;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.ShortType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import qupath.ext.imglib2.accesses.ArgbBufferedImageAccess;
import qupath.ext.imglib2.accesses.ByteBufferedImageAccess;
import qupath.ext.imglib2.accesses.ByteRasterAccess;
import qupath.ext.imglib2.accesses.DoubleRasterAccess;
import qupath.ext.imglib2.accesses.FloatRasterAccess;
import qupath.ext.imglib2.accesses.IntRasterAccess;
import qupath.ext.imglib2.accesses.ShortRasterAccess;
import qupath.lib.images.servers.ImageServer;
import qupath.lib.images.servers.ImageServerMetadata;
import qupath.lib.images.servers.PixelType;
import qupath.lib.images.servers.ServerTools;
import qupath.lib.images.servers.TileRequest;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * A class to create {@link Img} or {@link RandomAccessibleInterval} from an {@link ImageServer}.
 * <p>
 * Use {@link #createBuilder(ImageServer)}, {@link #createBuilder(ImageServer, NumericType)},
 * {@link #createRealBuilder(ImageServer)} or {@link #createRealBuilder(ImageServer, RealType)} to create an instance
 * of this class.
 * <p>
 * This class is thread-safe.
 *
 * @param <T> the type of the returned accessibles
 * @param <A> the type contained in the input image
 */
public class ImgBuilder<T extends NumericType<T> & NativeType<T>, A extends SizableDataAccess> {

    /**
     * The index of the X axis of accessibles returned by functions of this class
     */
    public static final int AXIS_X = 0;
    /**
     * The index of the Y axis of accessibles returned by functions of this class
     */
    public static final int AXIS_Y = 1;
    /**
     * The index of the channel axis of accessibles returned by functions of this class
     */
    public static final int AXIS_CHANNEL = 2;
    /**
     * The index of the Z axis of accessibles returned by functions of this class
     */
    public static final int AXIS_Z = 3;
    /**
     * The index of the time axis of accessibles returned by functions of this class
     */
    public static final int AXIS_TIME = 4;
    /**
     * The number of axes of accessibles returned by functions of this class
     */
    public static final int NUMBER_OF_AXES = 5;
    private static final CellCache DEFAULT_CELL_CACHE = new CellCache((int) (Runtime.getRuntime().maxMemory() * 0.5 / (1024 * 1024)));
    private final ImageServer<BufferedImage> server;
    private final Function<BufferedImage, A> cellCreator;
    private final int numberOfChannels;
    private final T type;
    private CellCache cellCache = DEFAULT_CELL_CACHE;

    private ImgBuilder(ImageServer<BufferedImage> server, T type, Function<BufferedImage, A> cellCreator, int numberOfChannels) {
        if (server.nChannels() <= 0) {
            throw new IllegalArgumentException(String.format("The provided image has less than one channel (%d)", server.nChannels()));
        }

        this.server = server;
        this.numberOfChannels = numberOfChannels;
        this.cellCreator = cellCreator;
        this.type = type;
    }

    /**
     * Create a builder from an {@link ImageServer}. This doesn't create any accessibles yet.
     * <p>
     * The type of the output image is not checked, which might lead to problems later when accessing pixel values of the
     * returned accessibles of this class. It is recommended to use {@link #createBuilder(ImageServer, NumericType)} instead.
     * See also this function to know which pixel type is used.
     *
     * @param server the input image
     * @return a builder to create an instance of this class
     * @throws IllegalArgumentException if the provided image has less than one channel
     */
    public static ImgBuilder<? extends NumericType<?>, ?> createBuilder(ImageServer<BufferedImage> server) {
        if (server.isRGB()) {
            return new ImgBuilder<>(server, new ARGBType(), ArgbBufferedImageAccess::new, 1);
        } else {
            return createRealBuilderFromNonRgbServer(server);
        }
    }

    /**
     * Create a builder from an {@link ImageServer}. This doesn't create any accessibles yet.
     * <p>
     * The provided type must be compatible with the input image:
     * <ul>
     *     <li>
     *         If the input image is {@link ImageServer#isRGB() RGB}, the type must be {@link ARGBType}. Images created
     *         by the returned builder will have one channel.
     *     </li>
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
     * @throws IllegalArgumentException if the provided type is not compatible with the input image (see above), or if
     * the provided image has less than one channel
     */
    public static <T extends NumericType<T> & NativeType<T>> ImgBuilder<T, ?> createBuilder(ImageServer<BufferedImage> server, T type) {
        checkType(server, type);

        if (server.isRGB()) {
            return new ImgBuilder<>(server, type, ArgbBufferedImageAccess::new, 1);
        } else {
            return switch (server.getPixelType()) {
                case UINT8, INT8 -> new ImgBuilder<>(
                        server,
                        type,
                        image -> new ByteRasterAccess(image.getRaster()),
                        server.nChannels()
                );
                case UINT16, INT16 -> new ImgBuilder<>(
                        server,
                        type,
                        image -> new ShortRasterAccess(image.getRaster()),
                        server.nChannels()
                );
                case UINT32, INT32 -> new ImgBuilder<>(
                        server,
                        type,
                        image -> new IntRasterAccess(image.getRaster()),
                        server.nChannels()
                );
                case FLOAT32 -> new ImgBuilder<>(
                        server,
                        type,
                        image -> new FloatRasterAccess(image.getRaster()),
                        server.nChannels()
                );
                case FLOAT64 -> new ImgBuilder<>(
                        server,
                        type,
                        image -> new DoubleRasterAccess(image.getRaster()),
                        server.nChannels()
                );
            };
        }
    }

    /**
     * Create a builder from an {@link ImageServer}. This doesn't create any accessibles yet.
     * <p>
     * The type of the output image is not checked, which might lead to problems later when accessing pixel values of the
     * returned accessibles of this class. It is recommended to use {@link #createRealBuilder(ImageServer, RealType)}
     * instead. See also this function to know which pixel type is used.
     *
     * @param server the input image
     * @return a builder to create an instance of this class
     * @throws IllegalArgumentException if the provided image has less than one channel
     */
    public static ImgBuilder<? extends RealType<?>, ?> createRealBuilder(ImageServer<BufferedImage> server) {
        if (server.isRGB()) {
            return new ImgBuilder<>(server, new UnsignedByteType(), ByteBufferedImageAccess::new, 3);
        } else {
            return createRealBuilderFromNonRgbServer(server);
        }
    }

    /**
     * Create a builder from an {@link ImageServer}. This doesn't create any accessibles yet.
     * <p>
     * The provided type must be compatible with the input image:
     * <ul>
     *     <li>
     *         If the input image is {@link ImageServer#isRGB() RGB}, the type must be {@link UnsignedByteType}. Images
     *         created by the returned builder will have 3 channels (RGB).
     *     </li>
     *     <li>Else, see {@link #createBuilder(ImageServer, NumericType)}.</li>
     * </ul>
     *
     * @param server the input image
     * @param type the expected type of the output image
     * @return a builder to create an instance of this class
     * @param <T> the type corresponding to the provided image
     * @throws IllegalArgumentException if the provided type is not compatible with the input image (see above), or if
     * the provided image has less than one channel
     */
    public static <T extends RealType<T> & NativeType<T>> ImgBuilder<T, ?> createRealBuilder(ImageServer<BufferedImage> server, T type) {
        checkRealType(server, type);

        if (server.isRGB()) {
            return new ImgBuilder<>(server, type, ByteBufferedImageAccess::new, 3);
        } else {
            return switch (server.getPixelType()) {
                case UINT8, INT8 -> new ImgBuilder<>(
                        server,
                        type,
                        image -> new ByteRasterAccess(image.getRaster()),
                        server.nChannels()
                );
                case UINT16, INT16 -> new ImgBuilder<>(
                        server,
                        type,
                        image -> new ShortRasterAccess(image.getRaster()),
                        server.nChannels()
                );
                case UINT32, INT32 -> new ImgBuilder<>(
                        server,
                        type,
                        image -> new IntRasterAccess(image.getRaster()),
                        server.nChannels()
                );
                case FLOAT32 -> new ImgBuilder<>(
                        server,
                        type,
                        image -> new FloatRasterAccess(image.getRaster()),
                        server.nChannels()
                );
                case FLOAT64 -> new ImgBuilder<>(
                        server,
                        type,
                        image -> new DoubleRasterAccess(image.getRaster()),
                        server.nChannels()
                );
            };
        }
    }

    /**
     * Accessibles returned by this class will be divided into cells, which will be cached to gain performance. This
     * function sets the cache to use. By default, a static cache of maximal size half the amount of the
     * {@link Runtime#maxMemory() max memory} is used.
     *
     * @param cellCache the cache to use
     * @return this builder
     * @throws NullPointerException if the provided cache is null
     */
    public ImgBuilder<T, A> cellCache(CellCache cellCache) {
        this.cellCache = Objects.requireNonNull(cellCache);
        return this;
    }

    /**
     * Create a list of {@link RandomAccessibleInterval} corresponding to each level of the input image.
     * <p>
     * The {@link RandomAccessibleInterval} returned by this class are immutable. This means that any attempt to write
     * data to it will result in an {@link UnsupportedOperationException}.
     * <p>
     * See {@link #AXIS_X}, {@link #AXIS_Y}, {@link #AXIS_CHANNEL}, {@link #AXIS_Z}, and {@link #AXIS_TIME} to get the physical
     * interpretation of the dimensions of the returned {@link RandomAccessibleInterval}.
     * <p>
     * Pixels of the returned images are lazily fetched.
     *
     * @return a list of {@link RandomAccessibleInterval} corresponding to each level of the input image
     */
    public List<RandomAccessibleInterval<T>> buildForAllLevels() {
        return IntStream.range(0, server.getMetadata().nLevels())
                .mapToObj(this::buildForLevel)
                .toList();
    }

    /**
     * Create an {@link Img} from the input image and the provided level.
     * <p>
     * The {@link Img} returned by this class is immutable. This means that any attempt to write data to it will result in an
     * {@link UnsupportedOperationException}.
     * <p>
     * See {@link #AXIS_X}, {@link #AXIS_Y}, {@link #AXIS_CHANNEL}, {@link #AXIS_Z}, and {@link #AXIS_TIME} to get the physical
     * interpretation of the dimensions of the returned {@link Img}.
     * <p>
     * Pixels of the returned image are lazily fetched.
     *
     * @param level the level to consider
     * @return an {@link Img} corresponding to the provided level of the input image
     * @throws IllegalArgumentException if the provided level does not match with a level of the input image
     */
    public RandomAccessibleInterval<T> buildForLevel(int level) {
        if (level < 0 || level >= server.getMetadata().nLevels()) {
            throw new IllegalArgumentException(String.format(
                    "The provided level %d is not within 0 and %d",
                    level,
                    server.getMetadata().nLevels() - 1
            ));
        }

        List<TileRequest> tiles = new ArrayList<>(server.getTileRequestManager().getTileRequestsForLevel(level));

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
                cellIndex -> cellCache.getCell(tiles.get(Math.toIntExact(cellIndex)), this::createCell)
        );
    }

    /**
     * Create a list of {@link RandomAccessibleInterval} from the input image and the provided downsamples.
     * <p>
     * The {@link RandomAccessibleInterval} returned by this class are immutable. This means that any attempt to write
     * data to them will result in an {@link UnsupportedOperationException}.
     * <p>
     * See {@link #AXIS_X}, {@link #AXIS_Y}, {@link #AXIS_CHANNEL}, {@link #AXIS_Z}, and {@link #AXIS_TIME} to get the physical
     * interpretation of the dimensions of the returned {@link RandomAccessibleInterval}.
     * <p>
     * Values of the returned images are lazily fetched.
     * <p>
     * If the input image has to be scaled and its {@link ImageServerMetadata#getChannelType() channel type} is
     * {@link ImageServerMetadata.ChannelType#CLASSIFICATION}, then the nearest neighbor interpolation is used.
     * Otherwise, the linear interpolation is used.
     *
     * @param downsamples the downsamples to apply to the input image. Must be greater than 0
     * @return a list of {@link RandomAccessibleInterval} corresponding to the input image with the provided downsamples
     * applied. The ith returned {@link RandomAccessibleInterval} corresponds to the ith provided downsample
     * @throws IllegalArgumentException if one of the provided downsamples is not greater than 0
     */
    public List<RandomAccessibleInterval<T>> buildForDownsamples(List<Double> downsamples) {
        return downsamples.stream()
                .map(this::buildForDownsample)
                .toList();
    }

    /**
     * Create a {@link RandomAccessibleInterval} from the input image and the provided downsample.
     * <p>
     * The {@link RandomAccessibleInterval} returned by this class is immutable. This means that any attempt to write
     * data to it will result in an {@link UnsupportedOperationException}.
     * <p>
     * See {@link #AXIS_X}, {@link #AXIS_Y}, {@link #AXIS_CHANNEL}, {@link #AXIS_Z}, and {@link #AXIS_TIME} to get the physical
     * interpretation of the dimensions of the returned {@link RandomAccessibleInterval}.
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
    public RandomAccessibleInterval<T> buildForDownsample(double downsample) {
        if (downsample <= 0) {
            throw new IllegalArgumentException(String.format("The provided downsample %f is not greater than 0", downsample));
        }

        int level = ServerTools.getPreferredResolutionLevel(server, downsample);

        if (server.getMetadata().getChannelType() == ImageServerMetadata.ChannelType.CLASSIFICATION) {
            return AccessibleScaler.scaleWithNearestNeighborInterpolation(
                    buildForLevel(level),
                    server.getDownsampleForResolution(level) / downsample
            );
        } else {
            return AccessibleScaler.scaleWithLinearInterpolation(
                    buildForLevel(level),
                    server.getDownsampleForResolution(level) / downsample
            );
        }
    }

    private static ImgBuilder<? extends RealType<?>, ?> createRealBuilderFromNonRgbServer(ImageServer<BufferedImage> server) {
        return switch (server.getPixelType()) {
            case UINT8 -> new ImgBuilder<>(
                    server,
                    new UnsignedByteType(),
                    image -> new ByteRasterAccess(image.getRaster()),
                    server.nChannels()
            );
            case INT8 -> new ImgBuilder<>(
                    server,
                    new ByteType(),
                    image -> new ByteRasterAccess(image.getRaster()),
                    server.nChannels()
            );
            case UINT16 -> new ImgBuilder<>(
                    server,
                    new UnsignedShortType(),
                    image -> new ShortRasterAccess(image.getRaster()),
                    server.nChannels()
            );
            case INT16 -> new ImgBuilder<>(
                    server,
                    new ShortType(),
                    image -> new ShortRasterAccess(image.getRaster()),
                    server.nChannels()
            );
            case UINT32 -> new ImgBuilder<>(
                    server,
                    new UnsignedIntType(),
                    image -> new IntRasterAccess(image.getRaster()),
                    server.nChannels()
            );
            case INT32 -> new ImgBuilder<>(
                    server,
                    new IntType(),
                    image -> new IntRasterAccess(image.getRaster()),
                    server.nChannels()
            );
            case FLOAT32 -> new ImgBuilder<>(
                    server,
                    new FloatType(),
                    image -> new FloatRasterAccess(image.getRaster()),
                    server.nChannels()
            );
            case FLOAT64 -> new ImgBuilder<>(
                    server,
                    new DoubleType(),
                    image -> new DoubleRasterAccess(image.getRaster()),
                    server.nChannels()
            );
        };
    }

    private static <T> void checkType(ImageServer<?> server, T type) {
        if (server.isRGB()) {
            if (!(type instanceof ARGBType)) {
                throw new IllegalArgumentException(String.format(
                        "The provided type %s is not an ARGBType, which is the one expected for RGB images",
                        type.getClass()
                ));
            }
        } else {
            switch (server.getPixelType()) {
                case UINT8 -> {
                    if (!(type instanceof UnsignedByteType)) {
                        throw new IllegalArgumentException(String.format(
                                "The provided type %s is not a UnsignedByteType, which is the one expected for non-RGB UINT8 images",
                                type.getClass()
                        ));
                    }
                }
                case INT8 -> {
                    if (!(type instanceof ByteType)) {
                        throw new IllegalArgumentException(String.format(
                                "The provided type %s is not a ByteType, which is the one expected for non-RGB INT8 images",
                                type.getClass()
                        ));
                    }
                }
                case UINT16 -> {
                    if (!(type instanceof UnsignedShortType)) {
                        throw new IllegalArgumentException(String.format(
                                "The provided type %s is not a UnsignedShortType, which is the one expected for non-RGB UINT16 images",
                                type.getClass()
                        ));
                    }
                }
                case INT16 -> {
                    if (!(type instanceof ShortType)) {
                        throw new IllegalArgumentException(String.format(
                                "The provided type %s is not a ShortType, which is the one expected for non-RGB INT16 images",
                                type.getClass()
                        ));
                    }
                }
                case UINT32 -> {
                    if (!(type instanceof UnsignedIntType)) {
                        throw new IllegalArgumentException(String.format(
                                "The provided type %s is not a UnsignedIntType, which is the one expected for non-RGB UINT32 images",
                                type.getClass()
                        ));
                    }
                }
                case INT32 -> {
                    if (!(type instanceof IntType)) {
                        throw new IllegalArgumentException(String.format(
                                "The provided type %s is not a IntType, which is the one expected for non-RGB INT32 images",
                                type.getClass()
                        ));
                    }
                }
                case FLOAT32 -> {
                    if (!(type instanceof FloatType)) {
                        throw new IllegalArgumentException(String.format(
                                "The provided type %s is not a FloatType, which is the one expected for non-RGB FLOAT32 images",
                                type.getClass()
                        ));
                    }
                }
                case FLOAT64 -> {
                    if (!(type instanceof DoubleType)) {
                        throw new IllegalArgumentException(String.format(
                                "The provided type %s is not a DoubleType, which is the one expected for non-RGB FLOAT64 images",
                                type.getClass()
                        ));
                    }
                }
            }
        }
    }

    private static <T> void checkRealType(ImageServer<?> server, T type) {
        if (server.isRGB()) {
            if (!(type instanceof UnsignedByteType)) {
                throw new IllegalArgumentException(String.format(
                        "The provided type %s is not an UnsignedByteType, which is the one expected for RGB images",
                        type.getClass()
                ));
            }
        } else {
            checkType(server, type);
        }
    }

    private Cell<A> createCell(TileRequest tile) {
        BufferedImage image;
        try {
            image = server.readRegion(tile.getRegionRequest());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new Cell<>(
                new int[]{ image.getWidth(), image.getHeight(), numberOfChannels, 1, 1 },
                new long[]{ tile.getTileX(), tile.getTileY(), 0, tile.getZ(), tile.getT()},
                cellCreator.apply(image)
        );
    }
}
