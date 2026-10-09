package qupath.ext.imglib2;

import java.util.stream.Collectors;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.blocks.PrimitiveBlocks;
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
import net.imglib2.view.Views;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.color.ColorModelFactory;
import qupath.lib.common.LogTools;
import qupath.lib.images.servers.AbstractTileableImageServer;
import qupath.lib.images.servers.ImageChannel;
import qupath.lib.images.servers.ImageServer;
import qupath.lib.images.servers.ImageServerBuilder;
import qupath.lib.images.servers.ImageServerMetadata;
import qupath.lib.images.servers.PixelCalibration;
import qupath.lib.images.servers.PixelType;
import qupath.lib.images.servers.TileRequest;

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
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * An {@link qupath.lib.images.servers.ImageServer} whose pixel values come from {@link RandomAccessibleInterval}.
 * <p>
 * Use a {@link #builder(List)} to create an instance of this class.
 * <p>
 * This server doesn't support JSON serialization.
 *
 * @param <T> the pixel type of the underlying {@link RandomAccessibleInterval}
 */
public class ImgLib2ImageServer<T extends NativeType<T> & NumericType<T>> extends AbstractTileableImageServer {

    private static final Logger logger = LoggerFactory.getLogger(ImgLib2ImageServer.class);

    private static final AtomicInteger counter = new AtomicInteger();
    private final List<? extends RandomAccessibleInterval<T>> accessibles;
    private final ImageServerMetadata metadata;
    private final int numberOfChannelsInAccessibles;
    private final String persistentId;
    private final List<URI> uris;

    private ImgLib2ImageServer(List<? extends RandomAccessibleInterval<T>> accessibles, PixelType pixelType,
                               ImageServerMetadata metadata, String persistentId, List<URI> uris) {
        this.accessibles = accessibles;
        this.persistentId = persistentId;
        this.uris = uris == null ? List.of() : List.copyOf(uris);

        RandomAccessibleInterval<T> firstAccessible = accessibles.getFirst();
        T value = firstAccessible.getType();
        this.metadata = new ImageServerMetadata.Builder(metadata)
                .width((int) firstAccessible.dimension(ImgBuilder.AXIS_X))
                .height((int) firstAccessible.dimension(ImgBuilder.AXIS_Y))
                .rgb(value instanceof ARGBType)
                .pixelType(pixelType)
                .levels(createResolutionLevels(accessibles))
                .sizeZ((int) firstAccessible.dimension(ImgBuilder.AXIS_Z))
                .sizeT((int) firstAccessible.dimension(ImgBuilder.AXIS_TIME))
                .build();

        this.numberOfChannelsInAccessibles = (int) firstAccessible.dimension(ImgBuilder.AXIS_CHANNEL);
    }

    /**
     * Create a {@link ImgLib2ImageServer} builder.
     * <p>
     * The provided accessibles must correspond to the ones returned by functions of {@link ImgBuilder}: they must have
     * {@link ImgBuilder#NUMBER_OF_AXES} dimensions, the X-axes must correspond to {@link ImgBuilder#AXIS_X}, and so on.
     * <p>
     * All dimensions of the provided accessibles must contain {@link Integer#MAX_VALUE} pixels or less.
     * <p>
     * The type of the provided accessibles must be {@link ARGBType}, {@link UnsignedByteType}, {@link ByteType},
     * {@link UnsignedShortType}, {@link ShortType}, {@link UnsignedIntType}, {@link IntType}, {@link FloatType}, or
     * {@link DoubleType}. If the type is {@link ARGBType}, the provided accessibles must have one channel
     *
     * @param accessibles one accessible for each resolution level the image server should have, from highest to lowest
     *                    resolution. Must not be empty. Each accessible must have the same number of channels, z-stacks,
     *                    and timepoints
     * @return a new builder from the provided accessibles
     * @param <T> the pixel type of the provided accessibles
     * @throws NullPointerException if the provided list is null or contain a null element
     * @throws IllegalArgumentException if the provided list is empty, if the accessible type is not among the list
     * mentioned above, if a dimension of a provided accessible contain more than {@link Integer#MAX_VALUE} pixels,
     * if the provided accessibles do not have {@link ImgBuilder#NUMBER_OF_AXES} axes, if the provided accessibles
     * do not have the same number of channels, z-stacks, or timepoints, or if the accessible type is {@link ARGBType}
     * and the number of channels of the accessibles is not 1
     */
    public static <T extends NativeType<T> & NumericType<T>> Builder<T> builder(List<? extends RandomAccessibleInterval<T>> accessibles) {
        return new Builder<>(accessibles);
    }

    @Override
    protected BufferedImage readTile(TileRequest tileRequest) throws IOException {
        RandomAccessibleInterval<T> tile = getImgLib2Tile(tileRequest);

        if (isRGB()) {
            int minTileX = Math.toIntExact(tile.min(ImgBuilder.AXIS_X));
            int minTileY = Math.toIntExact(tile.min(ImgBuilder.AXIS_Y));
            return createArgbImage(tileRequest, tile.localizingCursor(), minTileX, minTileY);
        } else {
            int xyPlaneSize = Math.toIntExact(tile.dimension(ImgBuilder.AXIS_X) * tile.dimension(ImgBuilder.AXIS_Y));

            DataBuffer dataBuffer = switch (metadata.getPixelType()) {
                    case UINT8 -> createUint8DataBuffer(tile, new byte[numberOfChannelsInAccessibles][xyPlaneSize]);
                    case INT8 -> createInt8DataBuffer(tile, new byte[numberOfChannelsInAccessibles][xyPlaneSize]);
                    case UINT16 -> createUint16DataBuffer(tile, new short[numberOfChannelsInAccessibles][xyPlaneSize]);
                    case INT16 -> createInt16DataBuffer(tile, new short[numberOfChannelsInAccessibles][xyPlaneSize]);
                    case UINT32 -> createUint32DataBuffer(tile, new int[numberOfChannelsInAccessibles][xyPlaneSize]);
                    case INT32 -> createInt32DataBuffer(tile, new int[numberOfChannelsInAccessibles][xyPlaneSize]);
                    case FLOAT32 -> createFloat32DataBuffer(tile, new float[numberOfChannelsInAccessibles][xyPlaneSize]);
                    case FLOAT64 -> createFloat64DataBuffer(tile, new double[numberOfChannelsInAccessibles][xyPlaneSize]);
            };


            return new BufferedImage(
                    ColorModelFactory.createColorModel(metadata.getPixelType(), metadata.getChannels()),
                    WritableRaster.createWritableRaster(
                            new BandedSampleModel(
                                    dataBuffer.getDataType(),
                                    tileRequest.getTileWidth(),
                                    tileRequest.getTileHeight(),
                                    numberOfChannelsInAccessibles
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
    protected ImageServerBuilder.ServerBuilder<BufferedImage> createServerBuilder() {
        return null;
    }

    @Override
    protected String createID() {
        if (persistentId != null)
            return persistentId;
        if (uris.isEmpty())
            return "imglib2-" + counter.incrementAndGet();
        return "imglib2-[" + uris.stream().map(URI::toString).collect(Collectors.joining(", ")) + "]";
    }

    @Override
    public Collection<URI> getURIs() {
        return uris;
    }

    @Override
    public String getServerType() {
        return "ImgLib2";
    }

    @Override
    public ImageServerMetadata getOriginalMetadata() {
        return metadata;
    }

    @Override
    protected BufferedImage createDefaultRGBImage(int width, int height) {
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

    /**
     * A builder to create an instance of {@link ImgLib2ImageServer}.
     *
     * @param <T> the pixel type of the {@link ImgLib2ImageServer} to create
     */
    public static class Builder<T extends NativeType<T> & NumericType<T>> {

        private static final int DEFAULT_TILE_SIZE = 1024;
        private final List<? extends RandomAccessibleInterval<T>> accessibles;
        private final PixelType pixelType;
        private ImageServerMetadata metadata;
        private String persistentId;
        private List<URI> uris;

        private Builder(List<? extends RandomAccessibleInterval<T>> accessibles) {
            checkAccessibles(accessibles);

            RandomAccessibleInterval<T> firstAccessible = accessibles.getFirst();
            T value = firstAccessible.getType();

            this.accessibles = accessibles;
            this.pixelType = switch (value) {
                case ARGBType ignored -> PixelType.UINT8;
                case UnsignedByteType ignored -> PixelType.UINT8;
                case ByteType ignored -> PixelType.INT8;
                case UnsignedShortType ignored -> PixelType.UINT16;
                case ShortType ignored -> PixelType.INT16;
                case UnsignedIntType ignored -> PixelType.UINT32;
                case IntType ignored -> PixelType.INT32;
                case FloatType ignored -> PixelType.FLOAT32;
                case DoubleType ignored -> PixelType.FLOAT64;
                default -> throw new IllegalArgumentException(String.format("Unexpected accessible type %s", value));
            };
            this.metadata = new ImageServerMetadata.Builder()
                    .width(1)   // the width will be ignored, but it must be > 0 to avoid an exception when calling build()
                    .height(1)  // the height will be ignored, but it must be > 0 to avoid an exception when calling build()
                    .channels(value instanceof ARGBType ?
                            ImageChannel.getDefaultRGBChannels() :
                            ImageChannel.getDefaultChannelList((int) firstAccessible.dimension(ImgBuilder.AXIS_CHANNEL))
                    )
                    .preferredTileSize(DEFAULT_TILE_SIZE, DEFAULT_TILE_SIZE)
                    .build();
        }

        /**
         * Set the name of the {@link ImgLib2ImageServer} to build.
         *
         * @param name the name the image should have
         * @return this builder
         */
        public Builder<T> name(String name) {
            this.metadata = new ImageServerMetadata.Builder(metadata).name(name).build();
            return this;
        }

        /**
         * Set the channels of the {@link ImgLib2ImageServer} to build.
         * <p>
         * If not provided here or with {@link #metadata(ImageServerMetadata)}, the channels of the output image will be
         * {@link ImageChannel#getDefaultRGBChannels()} or {@link ImageChannel#getDefaultChannelList(int)} depending on
         * whether the accessible type is {@link ARGBType}.
         *
         * @param channels the channels to set. Must be {@link ImageChannel#getDefaultRGBChannels()} if the type of the
         *                 current accessibles is {@link ARGBType}, or must match the number of channels of the current
         *                 accessibles else
         * @return this builder
         * @throws NullPointerException if the provided list is null or contain a null element
         * @throws IllegalArgumentException if the current accessibles have the {@link ARGBType} and the provided channels
         * are not {@link ImageChannel#getDefaultRGBChannels()}, or if the current accessibles don't have the {@link ARGBType}
         * and the provided number of channels doesn't match the number of channels of the current accessibles
         */
        public Builder<T> channels(Collection<ImageChannel> channels) {
            checkChannels(accessibles, channels);

            this.metadata = new ImageServerMetadata.Builder(metadata).channels(channels).build();
            return this;
        }

        /**
         * Optionally specify a persistent ID to return with {@link ImageServer#getPath()}.
         * This is used for tile caching, so must be unique.
         * <p>
         * If it is not specified, an ID will be generated based on any provided URIs,
         * or if there are no URIs then a unique ID will be generated for the current QuPath session.
         * <p>
         * A typical use is to provide an ID that incorporates the builder type and the URI of
         * an image, so that cached tiles can be reused if the same image is reopened during a QuPath session.
         * Not setting the ID would mean that the reopened image caches its tiles with an entirely separate ID.
         * @param id the unique server ID
         * @return this builder
         */
        public Builder<T> persistentId(String id) {
            this.persistentId = id;
            return this;
        }

        /**
         * Optional list of URIs, which will be returned by {@link ImageServer#getURIs()}.
         * If provided, and {@link #persistentId} is not set, an ID will be generated based on these URIs.
         * @param uris a collection of URIs
         * @return this builder
         */
        public Builder<T> uris(Collection<? extends URI> uris) {
            this.uris = List.copyOf(uris);
            return this;
        }

        /**
         * Optional array of URIs, which will be returned by {@link ImageServer#getURIs()}.
         * If provided, and {@link #persistentId} is not set, an ID will be generated based on these URIs.
         * @param uris a collection of URIs
         * @return this builder
         */
        public Builder<T> uris(URI... uris) {
            return uris(List.of(uris));
        }

        /**
         * Set the tile size of the {@link ImgLib2ImageServer} to build.
         * <p>
         * If not provided here or with {@link #metadata(ImageServerMetadata)}, the tile width and height is set to 1024.
         *
         * @param tileWidth the tile width in pixels to set
         * @param tileHeight the tile height in pixels to set
         * @return this builder
         */
        public Builder<T> preferredTileSize(int tileWidth, int tileHeight) {
            this.metadata = new ImageServerMetadata.Builder(metadata)
                    .preferredTileSize(tileWidth, tileHeight)
                    .build();
            return this;
        }

        /**
         * Set the pixel calibration of the {@link ImgLib2ImageServer} to build.
         *
         * @param pixelCalibration the pixel calibration to set
         * @return this builder
         * @throws NullPointerException if the provided pixel calibration is null
         */
        public Builder<T> pixelCalibration(PixelCalibration pixelCalibration) {
            this.metadata = new ImageServerMetadata.Builder(metadata)
                    .pixelSizeMicrons(pixelCalibration.getPixelWidthMicrons(), pixelCalibration.getPixelHeightMicrons())
                    .zSpacingMicrons(pixelCalibration.getZSpacingMicrons())
                    .timepoints(
                            pixelCalibration.getTimeUnit(),
                            IntStream.range(0, pixelCalibration.nTimepoints()).mapToDouble(pixelCalibration::getTimepoint).toArray()
                    )
                    .build();
            return this;
        }

        /**
         * Set metadata parameters of the {@link ImgLib2ImageServer} to build.
         * <p>
         * If not provided here or with {@link #channels(Collection)}, the channels of the output image will be
         * {@link ImageChannel#getDefaultRGBChannels()} or {@link ImageChannel#getDefaultChannelList(int)} depending on
         * whether the accessible type is {@link ARGBType}.
         * <p>
         * If not provided here or with {@link #preferredTileSize(int, int)}, the tile width and height is set to 1024.
         *
         * @param metadata the metadata the image server should have. The width, height, number of z-stacks, number of
         *                 time points, whether the image is RGB, pixel type, and resolution level are not taken from
         *                 this metadata but determined from the provided accessibles. The channels of the provided
         *                 metadata must be {@link ImageChannel#getDefaultRGBChannels()} if the type of the current
         *                 accessibles is {@link ARGBType}, or must match the number of channels of the current accessibles
         *                 else
         * @return this builder
         * @throws NullPointerException if the provided metadata is null or if the channels of the provided metadata are
         * null or contain a null element
         * @throws IllegalArgumentException if the current accessibles have the {@link ARGBType} and the channels of the
         * provided metadata are not {@link ImageChannel#getDefaultRGBChannels()}, or if the current accessibles don't
         * have the {@link ARGBType} and the number of channels of the provided metadata doesn't match the number of
         * channels of the current accessibles
         */
        public Builder<T> metadata(ImageServerMetadata metadata) {
            checkChannels(accessibles, metadata.getChannels());

            this.metadata = metadata;
            return this;
        }

        /**
         * Create an {@link ImgLib2ImageServer} from this builder.
         *
         * @return a new {@link ImgLib2ImageServer} whose parameters are determined from this builder
         */
        public ImgLib2ImageServer<T> build() {
            return new ImgLib2ImageServer<>(accessibles, pixelType, metadata, persistentId, uris);
        }

        private static <T extends NativeType<T> & NumericType<T>> void checkAccessibles(List<? extends RandomAccessibleInterval<T>> accessibles) {
            if (accessibles == null) {
                throw new NullPointerException("The provided list of accessibles is null");
            }
            if (accessibles.stream().anyMatch(Objects::isNull)) {
                throw new NullPointerException(String.format("One of the provided accessibles %s is null", accessibles));
            }
            if (accessibles.isEmpty()) {
                throw new IllegalArgumentException("The provided list of accessibles is empty");
            }

            for (RandomAccessibleInterval<T> accessible: accessibles) {
                for (int dimension=0; dimension<accessible.numDimensions(); dimension++) {
                    long numberOfValues = accessible.dimension(dimension);

                    if ((int) numberOfValues != numberOfValues) {
                        throw new IllegalArgumentException(String.format(
                                "The dimension %d of the provided accessible %s contain more than %d pixels",
                                dimension,
                                accessible,
                                Integer.MAX_VALUE
                        ));
                    }
                }
            }

            for (RandomAccessibleInterval<T> accessible: accessibles) {
                if (accessible.numDimensions() != ImgBuilder.NUMBER_OF_AXES) {
                    throw new IllegalArgumentException(String.format(
                            "The provided accessible %s does not have %d dimensions",
                            accessible,
                            ImgBuilder.NUMBER_OF_AXES
                    ));
                }
            }

            Map<Integer, String> axes = Map.of(
                    ImgBuilder.AXIS_CHANNEL, "number of channels",
                    ImgBuilder.AXIS_Z, "number of z-stacks",
                    ImgBuilder.AXIS_TIME, "number of timepoints"
            );
            for (var axis: axes.entrySet()) {
                List<Long> numberOfElements = accessibles.stream()
                        .map(accessible -> accessible.dimension(axis.getKey()))
                        .distinct()
                        .toList();
                if (numberOfElements.size() > 1) {
                    throw new IllegalArgumentException(String.format(
                            "The provided accessibles %s do not contain the same %s (found %s)",
                            accessibles,
                            axis.getValue(),
                            numberOfElements
                    ));
                }
            }

            RandomAccessibleInterval<T> firstAccessible = accessibles.getFirst();
            if (firstAccessible.getType() instanceof ARGBType && firstAccessible.dimension(ImgBuilder.AXIS_CHANNEL) != 1) {
                throw new IllegalArgumentException(String.format(
                        "The provided accessibles %s have the ARGB type, but not one channel (found %d)",
                        accessibles,
                        firstAccessible.dimension(ImgBuilder.AXIS_CHANNEL)
                ));
            }
        }

        private static <T extends NativeType<T> & NumericType<T>> void checkChannels(
                List<? extends RandomAccessibleInterval<T>> accessibles,
                Collection<ImageChannel> channels
        ) {
            for (ImageChannel channel: channels) {
                Objects.requireNonNull(channel);
            }

            if (accessibles.getFirst().getType() instanceof ARGBType) {
                if (!channels.equals(ImageChannel.getDefaultRGBChannels())) {
                    throw new IllegalArgumentException(String.format(
                            "The current accessibles %s have the ARGB type, but the provided channels %s are not the default RGB channels %s",
                            accessibles,
                            channels,
                            ImageChannel.getDefaultRGBChannels()
                    ));
                }
            } else {
                if (accessibles.getFirst().dimension(ImgBuilder.AXIS_CHANNEL) != channels.size()) {
                    throw new IllegalArgumentException(String.format(
                            "There are %d provided channels, but the current accessibles %s contain %s channels",
                            channels.size(),
                            accessibles,
                            accessibles.getFirst().dimension(ImgBuilder.AXIS_CHANNEL)
                    ));
                }
            }
        }
    }

    private static List<ImageServerMetadata.ImageResolutionLevel> createResolutionLevels(List<? extends RandomAccessibleInterval<?>> accessibles) {
        ImageServerMetadata.ImageResolutionLevel.Builder builder = new ImageServerMetadata.ImageResolutionLevel.Builder(
                (int) accessibles.getFirst().dimension(ImgBuilder.AXIS_X),
                (int) accessibles.getFirst().dimension(ImgBuilder.AXIS_Y)
        );

        for (RandomAccessibleInterval<?> accessible: accessibles) {
            builder.addLevel(
                    (int) accessible.dimension(ImgBuilder.AXIS_X),
                    (int) accessible.dimension(ImgBuilder.AXIS_Y)
            );
        }

        return builder.build();
    }

    private RandomAccessibleInterval<T> getImgLib2Tile(TileRequest tileRequest) {
        RandomAccessibleInterval<T> wholeLevel = accessibles.get(tileRequest.getLevel());

        long[] minWholeLevel = new long[ImgBuilder.NUMBER_OF_AXES];
        wholeLevel.min(minWholeLevel);

        long[] min = new long[ImgBuilder.NUMBER_OF_AXES];
        min[ImgBuilder.AXIS_X] = minWholeLevel[ImgBuilder.AXIS_X] + tileRequest.getTileX();
        min[ImgBuilder.AXIS_Y] = minWholeLevel[ImgBuilder.AXIS_Y] + tileRequest.getTileY();
        min[ImgBuilder.AXIS_CHANNEL] = minWholeLevel[ImgBuilder.AXIS_CHANNEL];
        min[ImgBuilder.AXIS_Z] = minWholeLevel[ImgBuilder.AXIS_Z] + tileRequest.getZ();
        min[ImgBuilder.AXIS_TIME] = minWholeLevel[ImgBuilder.AXIS_TIME] + tileRequest.getT();

        long[] max = new long[ImgBuilder.NUMBER_OF_AXES];   // max is inclusive, hence the -1
        max[ImgBuilder.AXIS_X] = min[ImgBuilder.AXIS_X] + tileRequest.getTileWidth() - 1;
        max[ImgBuilder.AXIS_Y] = min[ImgBuilder.AXIS_Y] + tileRequest.getTileHeight() - 1;
        max[ImgBuilder.AXIS_CHANNEL] = min[ImgBuilder.AXIS_CHANNEL] + numberOfChannelsInAccessibles - 1;
        max[ImgBuilder.AXIS_Z] = min[ImgBuilder.AXIS_Z];
        max[ImgBuilder.AXIS_TIME] = min[ImgBuilder.AXIS_TIME];

        return Views.interval(wholeLevel, min, max);
    }

    private BufferedImage createArgbImage(TileRequest tileRequest, Cursor<T> cursor, int minTileX, int minTileY) {
        BufferedImage image = new BufferedImage(tileRequest.getTileWidth(), tileRequest.getTileHeight(), BufferedImage.TYPE_INT_ARGB);
        DataBufferInt buffer = (DataBufferInt) image.getRaster().getDataBuffer();

        while (cursor.hasNext()) {
            ARGBType value = (ARGBType) cursor.next();

            int xy = cursor.getIntPosition(ImgBuilder.AXIS_X) - minTileX +
                    (cursor.getIntPosition(ImgBuilder.AXIS_Y) - minTileY) * tileRequest.getTileWidth();

            buffer.setElem(xy, value.get());
        }

        return image;
    }

    private DataBufferByte createUint8DataBuffer(RandomAccessibleInterval<T> tile, byte[][] pixels) {
        if (pixels.length == 1) {
            extract(tile, pixels[0]);
        } else {
            try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
                for (int c = 0; c < pixels.length; c++) {
                    var channel = Views.hyperSlice(tile, ImgBuilder.AXIS_CHANNEL, c);
                    var dest = pixels[c];
                    pool.submit(() -> extract(channel, dest));
                }
            }
            ;
        }
        return new DataBufferByte(pixels, pixels[0].length);
    }

    private DataBufferByte createInt8DataBuffer(RandomAccessibleInterval<T> tile, byte[][] pixels) {
        LogTools.warnOnce(logger, "Requesting INT8 image, underflow is possible");
        if (pixels.length == 1) {
            extract(tile, pixels[0]);
        } else {
            try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
                for (int c = 0; c < pixels.length; c++) {
                    var channel = Views.hyperSlice(tile, ImgBuilder.AXIS_CHANNEL, c);
                    var dest = pixels[c];
                    pool.submit(() -> extract(channel, dest));
                }
            }
            ;
        }
        return new DataBufferByte(pixels, pixels[0].length);
    }

    private DataBufferUShort createUint16DataBuffer(RandomAccessibleInterval<T> tile, short[][] pixels) {
        if (pixels.length == 1) {
            extract(tile, pixels[0]);
        } else {
            try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
                for (int c = 0; c < pixels.length; c++) {
                    var channel = Views.hyperSlice(tile, ImgBuilder.AXIS_CHANNEL, c);
                    var dest = pixels[c];
                    pool.submit(() -> extract(channel, dest));
                }
            }
        }
        return new DataBufferUShort(pixels, pixels[0].length);
    }

    private DataBufferShort createInt16DataBuffer(RandomAccessibleInterval<T> tile, short[][] pixels) {
        if (pixels.length == 1) {
            extract(tile, pixels[0]);
        } else {
            try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
                for (int c = 0; c < pixels.length; c++) {
                    var channel = Views.hyperSlice(tile, ImgBuilder.AXIS_CHANNEL, c);
                    var dest = pixels[c];
                    pool.submit(() -> extract(channel, dest));
                }
            }
        }
        return new DataBufferShort(pixels, pixels[0].length);
    }

    private DataBufferInt createInt32DataBuffer(RandomAccessibleInterval<T> tile, int[][] pixels) {
        if (pixels.length == 1) {
            extract(tile, pixels[0]);
        } else {
            try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
                for (int c = 0; c < pixels.length; c++) {
                    var channel = Views.hyperSlice(tile, ImgBuilder.AXIS_CHANNEL, c);
                    var dest = pixels[c];
                    pool.submit(() -> extract(channel, dest));
                }
            }
        }
        return new DataBufferInt(pixels, pixels[0].length);
    }

    private DataBufferInt createUint32DataBuffer(RandomAccessibleInterval<T> tile, int[][] pixels) {
        LogTools.warnOnce(logger, "Requesting UINT32 image, underflow may occur");
        if (pixels.length == 1) {
            extract(tile, pixels[0]);
        } else {
            try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
                for (int c = 0; c < pixels.length; c++) {
                    var channel = Views.hyperSlice(tile, ImgBuilder.AXIS_CHANNEL, c);
                    var dest = pixels[c];
                    pool.submit(() -> extract(channel, dest));
                }
            }
        }
        return new DataBufferInt(pixels, pixels[0].length);
    }

    private DataBufferFloat createFloat32DataBuffer(RandomAccessibleInterval<T> tile, float[][] pixels) {
        if (pixels.length == 1) {
            extract(tile, pixels[0]);
        } else {
            try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
                for (int c = 0; c < pixels.length; c++) {
                    var channel = Views.hyperSlice(tile, ImgBuilder.AXIS_CHANNEL, c);
                    var dest = pixels[c];
                    pool.submit(() -> extract(channel, dest));
                }
            }
        }
        return new DataBufferFloat(pixels, pixels[0].length);
    }

    private DataBufferDouble createFloat64DataBuffer(RandomAccessibleInterval<T> tile, double[][] pixels) {
        if (pixels.length == 1) {
            extract(tile, pixels[0]);
        } else {
            try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
                for (int c = 0; c < pixels.length; c++) {
                    var channel = Views.hyperSlice(tile, ImgBuilder.AXIS_CHANNEL, c);
                    var dest = pixels[c];
                    pool.submit(() -> extract(channel, dest));
                }
            }
        }
        return new DataBufferDouble(pixels, pixels[0].length);
    }

    private static <T, S extends NativeType<S>> T extract(RandomAccessibleInterval<S> interval, T dest) {
        PrimitiveBlocks.of(interval, PrimitiveBlocks.OnFallback.ACCEPT).copy(interval, dest);
        return dest;
    }

}
