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
 */
public class ImgBuilder<T> {

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
    private final int numberOfChannels;
    private final T type;
    private CellCache cellCache = DEFAULT_CELL_CACHE;

    private ImgBuilder(ImageServer<BufferedImage> server, T type, int numberOfChannels) {
        Objects.requireNonNull(server, "Server must not be null");
        Objects.requireNonNull(type, "Type must not be null");
        if (numberOfChannels <= 0) {
            throw new IllegalArgumentException(String.format("The provided image has less than one channel (%d)", server.nChannels()));
        }
        if (server.isRGB() && numberOfChannels == 1)
            checkType(server, type);
        else
            checkRealType(server.getPixelType(), type);

        this.server = server;
        this.numberOfChannels = numberOfChannels;
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
    public static <T extends RealType<T>> ImgBuilder<? extends NumericType<?>> createBuilder(ImageServer<BufferedImage> server) {
        if (server.isRGB()) {
            return createRgbBuilder(server);
        } else {
            T type = getRealType(server.getPixelType());
            return createBuilder(server, type);
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
    public static <T extends NumericType<T>> ImgBuilder<T> createBuilder(ImageServer<BufferedImage> server, T type) {
        if (server.isRGB() && type instanceof ARGBType) {
            return new ImgBuilder<>(server, type, 1);
        } else {
            return new ImgBuilder<>(server, type, server.nChannels());
        }
    }

    /**
     * Create a builder from an {@link ImageServer} representing an RGB image using {@link ARGBType}.
     *
     * @param server the input image
     * @return a builder to create an instance of this class
     * @throws IllegalArgumentException if the provided image is not RGB
     */
    public static ImgBuilder<ARGBType> createRgbBuilder(ImageServer<BufferedImage> server) throws IllegalArgumentException {
        return new ImgBuilder<>(server, new ARGBType(), 1);
    }

    /**
     * Create a builder from an {@link ImageServer}, where the type is known to be an instance of {@link RealType}.
     * An RGB image is returned so that the channels are accessed separately, not packed into a single type.
     * <p>
     * The type of the output image is not checked, which might lead to problems later when accessing pixel values of the
     * returned accessibles of this class. It is recommended to use {@link #createRealBuilder(ImageServer, RealType)} instead.
     * See also this function to know which pixel type is used.
     *
     * @param server the input image
     * @return a builder to create an instance of this class
     * @throws IllegalArgumentException if the provided image has less than one channel
     */
    public static <T extends RealType<T>> ImgBuilder<? extends RealType<?>> createRealBuilder(ImageServer<BufferedImage> server) {
        T type = getRealType(server.getPixelType());
        return createRealBuilder(server, type);
    }

    /**
     * Create a builder from an {@link ImageServer}, where the type is specified and an instance of {@link RealType}.
     * <p>
     * Note that the type must be compatible for this to work; see {@link #createBuilder(ImageServer, NumericType)}
     * for more information, and/or check the type against the type returned by {@link #getRealType(PixelType)}.
     * <p>
     * Note that for an RGB image, this will give 3 channels with {@link UnsignedByteType}, rather than using {@link ARGBType}.
     *
     * @param server the input image
     * @return a builder to create an instance of this class
     * @throws IllegalArgumentException if the provided type is not compatible with the input image (see above), or if
     *      * the provided image has less than one channel
     * @see #createRgbBuilder(ImageServer)
     */
    public static <T extends RealType<T>> ImgBuilder<T> createRealBuilder(ImageServer<BufferedImage> server, T type) {
        return new ImgBuilder<>(server, type, server.nChannels());
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
    public ImgBuilder<T> cellCache(CellCache cellCache) {
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

        if (type instanceof NativeType<?>) {
            return createLazyImage((NativeType)type, level);
        } else {
            throw new IllegalArgumentException(type + " is not an instanceof NativeType");
        }
    }

    private <S extends NativeType<S>> LazyCellImg<S, ?> createLazyImage(S type, int level) {
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
        RandomAccessibleInterval<T> imgLevel = buildForLevel(level);

        if (server.getMetadata().getChannelType() != ImageServerMetadata.ChannelType.CLASSIFICATION &&
            imgLevel.getType() instanceof NumericType<?>) {
            return AccessibleScaler.scaleWithLinearInterpolation(
                    (RandomAccessibleInterval)imgLevel,
                    server.getDownsampleForResolution(level) / downsample
            );
        } else {
            return AccessibleScaler.scaleWithNearestNeighborInterpolation(
                    imgLevel,
                    server.getDownsampleForResolution(level) / downsample
            );
        }
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
            checkRealType(server.getPixelType(), type);
        }
    }

    private static <T> void checkRealType(PixelType pixelType, T type) {
        var expectedType = getRealType(pixelType);
        if (!expectedType.getClass().isInstance(type)) {
            throw new IllegalArgumentException(String.format(
                    "The provided type %s is not %s, which is the one expected for %s images",
                    type.getClass(),
                    expectedType.getClass().getSimpleName(),
                    pixelType
            ));
        }
    }

    private Cell<? extends SizableDataAccess> createCell(TileRequest tile) {
        BufferedImage image;
        try {
            image = server.readRegion(tile.getRegionRequest());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new Cell<>(
                new int[]{ image.getWidth(), image.getHeight(), numberOfChannels, 1, 1 },
                new long[]{ tile.getTileX(), tile.getTileY(), 0, tile.getZ(), tile.getT()},
                createAccess(image)
        );
    }

    private SizableDataAccess createAccess(BufferedImage img) {
        if (server.isRGB()) {
            if (numberOfChannels == 1) {
                return new ArgbBufferedImageAccess(img);
            } else {
                return new ByteBufferedImageAccess(img);
            }
        } else {
            var raster = img.getRaster();
            return switch (server.getPixelType()) {
                case UINT8, INT8 -> new ByteRasterAccess(raster);
                case UINT16, INT16 -> new ShortRasterAccess(raster);
                case UINT32, INT32 -> new IntRasterAccess(raster);
                case FLOAT32 -> new FloatRasterAccess(raster);
                case FLOAT64 ->  new DoubleRasterAccess(raster);
            };
        }
    }

    /**
     * Create an instance of the default type for a server.
     * If {@code server.isRGB()} returns true, this will call {@link #getRgbType()}, otherwise it will call
     * {@link #getRealType(ImageServer)}.
     * @param server the image server
     * @return the default numeric type to create images for this server
     */
    public static NumericType<?> getDefaultType(ImageServer<?> server) {
        if (server.isRGB())
            return getRgbType();
        else
            return getRealType(server.getPixelType());
    }

    /**
     * Create an instance of the default type for an RGB image.
     * @return a new instance of {@link ARGBType}.
     */
    public static ARGBType getRgbType() {
        return new ARGBType();
    }

    /**
     * Create an instance of the default {@link RealType} for an image server.
     * If the image is RGB, this will return {@link UnsignedByteType} - indicating that channels should be
     * treated separately.
     * @param server the image server
     * @return the default real type to create images for this server
     */
    public static RealType<?> getRealType(ImageServer<?> server) {
        return getRealType(server.getPixelType());
    }

    /**
     * Create the default {@link RealType} for a pixel type.
     * <p>
     * <b>Warning!</b> The return value is unchecked. It is possible to write code that compiles but fails with
     * a {@link ClassCastException} at runtime because the caller assigns the wrong class, e.g.
     * <code>
     *     FloatType type = createRealType(PixelType.UINT8); // Compiler cannot check return type!
     * </code>
     * @param pixelType the pixel type of the image server
     * @return an instanceof {@link RealType} suitable to represent the given pixel type
     * @param <T> generic parameter for the type
     */
    @SuppressWarnings("unchecked")
    public static <T extends RealType<T>> T getRealType(PixelType pixelType) {
        Objects.requireNonNull(pixelType, "Pixel type must not be null");
        return switch (pixelType) {
            case UINT8 -> (T)new UnsignedByteType();
            case INT8 -> (T)new ByteType();
            case UINT16 -> (T)new UnsignedShortType();
            case INT16 -> (T)new ShortType();
            case UINT32 -> (T)new UnsignedIntType();
            case INT32 -> (T)new IntType();
            case FLOAT32 -> (T)new FloatType();
            case FLOAT64 -> (T)new DoubleType();
        };
    }
    

}
