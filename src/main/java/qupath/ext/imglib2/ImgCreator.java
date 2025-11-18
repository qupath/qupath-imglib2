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
import qupath.lib.images.servers.ImageServer;
import qupath.lib.images.servers.ImageServerMetadata;
import qupath.lib.images.servers.ServerTools;
import qupath.lib.images.servers.TileRequest;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * A class to create {@link Img} or {@link RandomAccessibleInterval} from an {@link ImageServer}.
 * <p>
 * Use a {@link #builder(ImageServer)} or {@link #builder(ImageServer)} to create an instance of this class.
 * <p>
 * This class is thread-safe.
 *
 * @param <A> the type contained in the input image
 */
public class ImgCreator<A extends SizableDataAccess> {

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
    private final ImageServer<BufferedImage> server;
    private final CellCache cellCache;
    private final Function<BufferedImage, A> cellCreator;
    private final int numberOfChannels;

    private ImgCreator(Builder builder, Function<BufferedImage, A> cellCreator) {
        this.server = builder.server;
        this.cellCache = builder.cellCache;
        this.cellCreator = cellCreator;
        this.numberOfChannels = server.isRGB() ? 1 : server.nChannels();
    }

    /**
     * Create a builder from an {@link ImageServer}. This doesn't create any accessibles yet.
     *
     * @param server the input image
     * @return a builder to create an instance of this class
     * @throws IllegalArgumentException if the provided image has less than one channel
     */
    public static Builder builder(ImageServer<BufferedImage> server) {
        // Despite the potential warning, T is necessary, otherwise a cannot infer type arguments error occurs
        return new Builder(server);
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
     * @param <T> Generic parameter for the image type.
     */
    public <T extends NumericType<T> & NativeType<T>> Img<T> createForLevel(int level) {
        if (level < 0 || level >= server.getMetadata().nLevels()) {
            throw new IllegalArgumentException(String.format(
                    "The provided level %d is not within 0 and %d",
                    level,
                    server.getMetadata().nLevels() - 1
            ));
        }

        List<TileRequest> tiles = new ArrayList<>(server.getTileRequestManager().getTileRequestsForLevel(level));

        T type = getTypeOfServer(server);
        return new LazyCellImg<>(
                new CellGrid(
                        new long[]{
                                server.getMetadata().getLevel(level).getWidth(),
                                server.getMetadata().getLevel(level).getHeight(),
                                numberOfChannels,
                                server.nZSlices(),
                                server.nTimepoints()
                        },
                        new int[]{
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
     * Create a {@link RandomAccessibleInterval} from the input image and the provided downsample.
     * <p>
     * The {@link RandomAccessibleInterval} returned by this class is immutable. This means that any attempt to write data to it will result in an
     * {@link UnsupportedOperationException}.
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
     * @param <T> Generic parameter for the image type.
     */
    public <T extends NumericType<T> & NativeType<T>> RandomAccessibleInterval<T> createForDownsample(double downsample) {
        if (downsample <= 0) {
            throw new IllegalArgumentException(String.format("The provided downsample %f is not greater than 0", downsample));
        }

        int level = ServerTools.getPreferredResolutionLevel(server, downsample);
        Img<T> img = createForLevel(level);

        if (server.getMetadata().getChannelType() == ImageServerMetadata.ChannelType.CLASSIFICATION) {
            return AccessibleScaler.scaleWithNearestNeighborInterpolation(img, server.getDownsampleForResolution(level) / downsample);
        } else {
            return AccessibleScaler.scaleWithLinearInterpolation(img, server.getDownsampleForResolution(level) / downsample);
        }
    }

    /**
     * A builder to create an instance of {@link ImgCreator}.
     */
    public static class Builder {

        private static final CellCache defaultCellCache = new CellCache((int) (Runtime.getRuntime().maxMemory() * 0.5 / (1024 * 1024)));
        private final ImageServer<BufferedImage> server;
        private CellCache cellCache = defaultCellCache;

        private Builder(ImageServer<BufferedImage> server) {
            if (server.nChannels() <= 0) {
                throw new IllegalArgumentException(String.format("The provided image has less than one channel (%d)", server.nChannels()));
            }
            this.server = server;
        }

        /**
         * Accessibles returned by this class will be divided into cells, which will be cached to gain performance. This function sets the
         * cache to use. By default, a static cache of maximal size half the amount of the {@link Runtime#maxMemory() max memory} is used.
         *
         * @param cellCache the cache to use
         * @return this builder
         * @throws NullPointerException if the provided cache is null
         */
        public Builder cellCache(CellCache cellCache) {
            this.cellCache = Objects.requireNonNull(cellCache);
            return this;
        }

        /**
         * Build an instance of {@link ImgCreator}.
         *
         * @return a new instance of {@link ImgCreator}
         */
        public ImgCreator<?> build() {
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
        
    }

    @SuppressWarnings("unchecked")
    private static <T extends NumericType<T> & NativeType<T>> T getTypeOfServer(ImageServer<?> server) {
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
