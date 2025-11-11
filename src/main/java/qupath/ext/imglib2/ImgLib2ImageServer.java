package qupath.ext.imglib2;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
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
import qupath.lib.color.ColorModelFactory;
import qupath.lib.images.servers.AbstractTileableImageServer;
import qupath.lib.images.servers.ImageChannel;
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
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * An {@link qupath.lib.images.servers.ImageServer} whose pixel values come from {@link RandomAccessibleInterval}.
 * <p>
 * Use a {@link Builder} to create an instance of this class.
 * <p>
 * This server doesn't support JSON serialization.
 *
 * @param <T> the pixel type of the underlying {@link RandomAccessibleInterval}
 */
public class ImgLib2ImageServer<T extends NativeType<T> & NumericType<T>> extends AbstractTileableImageServer {

    private static final AtomicInteger counter = new AtomicInteger();
    private final List<RandomAccessibleInterval<T>> accessibles;
    private final ImageServerMetadata metadata;
    private final int numberOfChannelsInAccessibles;

    private ImgLib2ImageServer(List<RandomAccessibleInterval<T>> accessibles, PixelType pixelType, ImageServerMetadata metadata) {
        this.accessibles = accessibles;

        RandomAccessibleInterval<T> firstAccessible = accessibles.getFirst();
        T value = firstAccessible.firstElement();
        this.metadata = new ImageServerMetadata.Builder(metadata)
                .width((int) firstAccessible.dimension(ImgCreator.AXIS_X))
                .height((int) firstAccessible.dimension(ImgCreator.AXIS_Y))
                .rgb(value instanceof ARGBType)
                .pixelType(pixelType)
                .levels(createResolutionLevels(accessibles))
                .sizeZ((int) firstAccessible.dimension(ImgCreator.AXIS_Z))
                .sizeT((int) firstAccessible.dimension(ImgCreator.AXIS_TIME))
                .build();

        this.numberOfChannelsInAccessibles = (int) firstAccessible.dimension(ImgCreator.AXIS_CHANNEL);
    }

    @Override
    protected BufferedImage readTile(TileRequest tileRequest) {
        RandomAccessibleInterval<T> tile = getImgLib2Tile(tileRequest);
        long[] minTileLong = new long[ImgCreator.NUMBER_OF_AXES];
        tile.min(minTileLong);
        int[] minTile = Arrays.stream(minTileLong).mapToInt(Math::toIntExact).toArray();

        int xyPlaneSize = Math.toIntExact(tile.dimension(ImgCreator.AXIS_X) * tile.dimension(ImgCreator.AXIS_Y));
        int[] position = new int[ImgCreator.NUMBER_OF_AXES];
        Cursor<T> cursor = tile.localizingCursor();

        if (isRGB()) {
            BufferedImage image = new BufferedImage(tileRequest.getTileWidth(), tileRequest.getTileHeight(), BufferedImage.TYPE_INT_ARGB);

            int[] argb = new int[xyPlaneSize];
            while (cursor.hasNext()) {
                ARGBType value = (ARGBType) cursor.next();

                cursor.localize(position);
                int xy = position[ImgCreator.AXIS_X] - minTile[ImgCreator.AXIS_X] +
                        (position[ImgCreator.AXIS_Y] - minTile[ImgCreator.AXIS_Y]) * tileRequest.getTileWidth();

                argb[xy] = value.get();
            }
            image.setRGB(0, 0, image.getWidth(), image.getHeight(), argb, 0, image.getWidth());

            return image;
        } else {
            DataBuffer dataBuffer = switch (metadata.getPixelType()) {
                case UINT8 -> {
                    byte[][] pixels = new byte[numberOfChannelsInAccessibles][xyPlaneSize];

                    while (cursor.hasNext()) {
                        UnsignedByteType value = (UnsignedByteType) cursor.next();

                        cursor.localize(position);
                        int c = position[ImgCreator.AXIS_CHANNEL] - minTile[ImgCreator.AXIS_CHANNEL];
                        int xy = position[ImgCreator.AXIS_X] - minTile[ImgCreator.AXIS_X] +
                                (position[ImgCreator.AXIS_Y] - minTile[ImgCreator.AXIS_Y]) * tileRequest.getTileWidth();

                        pixels[c][xy] = value.getByte();
                    }

                    yield new DataBufferByte(pixels, xyPlaneSize);
                }
                case INT8 -> {
                    byte[][] pixels = new byte[numberOfChannelsInAccessibles][xyPlaneSize];

                    while (cursor.hasNext()) {
                        ByteType value = (ByteType) cursor.next();

                        cursor.localize(position);
                        int c = position[ImgCreator.AXIS_CHANNEL] - minTile[ImgCreator.AXIS_CHANNEL];
                        int xy = position[ImgCreator.AXIS_X] - minTile[ImgCreator.AXIS_X] +
                                (position[ImgCreator.AXIS_Y] - minTile[ImgCreator.AXIS_Y]) * tileRequest.getTileWidth();

                        pixels[c][xy] = value.getByte();
                    }

                    yield new DataBufferByte(pixels, xyPlaneSize);
                }
                case UINT16 -> {
                    short[][] pixels = new short[numberOfChannelsInAccessibles][xyPlaneSize];

                    while (cursor.hasNext()) {
                        UnsignedShortType value = (UnsignedShortType) cursor.next();

                        cursor.localize(position);
                        int c = position[ImgCreator.AXIS_CHANNEL] - minTile[ImgCreator.AXIS_CHANNEL];
                        int xy = position[ImgCreator.AXIS_X] - minTile[ImgCreator.AXIS_X] +
                                (position[ImgCreator.AXIS_Y] - minTile[ImgCreator.AXIS_Y]) * tileRequest.getTileWidth();

                        pixels[c][xy] = value.getShort();
                    }

                    yield new DataBufferUShort(pixels, xyPlaneSize);
                }
                case INT16 -> {
                    short[][] pixels = new short[numberOfChannelsInAccessibles][xyPlaneSize];

                    while (cursor.hasNext()) {
                        ShortType value = (ShortType) cursor.next();

                        cursor.localize(position);
                        int c = position[ImgCreator.AXIS_CHANNEL] - minTile[ImgCreator.AXIS_CHANNEL];
                        int xy = position[ImgCreator.AXIS_X] - minTile[ImgCreator.AXIS_X] +
                                (position[ImgCreator.AXIS_Y] - minTile[ImgCreator.AXIS_Y]) * tileRequest.getTileWidth();

                        pixels[c][xy] = value.getShort();
                    }

                    yield new DataBufferShort(pixels, xyPlaneSize);
                }
                case UINT32 -> {
                    int[][] pixels = new int[numberOfChannelsInAccessibles][xyPlaneSize];

                    while (cursor.hasNext()) {
                        UnsignedIntType value = (UnsignedIntType) cursor.next();

                        cursor.localize(position);
                        int c = position[ImgCreator.AXIS_CHANNEL] - minTile[ImgCreator.AXIS_CHANNEL];
                        int xy = position[ImgCreator.AXIS_X] - minTile[ImgCreator.AXIS_X] +
                                (position[ImgCreator.AXIS_Y] - minTile[ImgCreator.AXIS_Y]) * tileRequest.getTileWidth();

                        pixels[c][xy] = value.getInt();
                    }

                    yield new DataBufferInt(pixels, xyPlaneSize);
                }
                case INT32 -> {
                    int[][] pixels = new int[numberOfChannelsInAccessibles][xyPlaneSize];

                    while (cursor.hasNext()) {
                        IntType value = (IntType) cursor.next();

                        cursor.localize(position);
                        int c = position[ImgCreator.AXIS_CHANNEL] - minTile[ImgCreator.AXIS_CHANNEL];
                        int xy = position[ImgCreator.AXIS_X] - minTile[ImgCreator.AXIS_X] +
                                (position[ImgCreator.AXIS_Y] - minTile[ImgCreator.AXIS_Y]) * tileRequest.getTileWidth();

                        pixels[c][xy] = value.getInt();
                    }

                    yield new DataBufferInt(pixels, xyPlaneSize);
                }
                case FLOAT32 -> {
                    float[][] pixels = new float[numberOfChannelsInAccessibles][xyPlaneSize];

                    while (cursor.hasNext()) {
                        FloatType value = (FloatType) cursor.next();

                        cursor.localize(position);
                        int c = position[ImgCreator.AXIS_CHANNEL] - minTile[ImgCreator.AXIS_CHANNEL];
                        int xy = position[ImgCreator.AXIS_X] - minTile[ImgCreator.AXIS_X] +
                                (position[ImgCreator.AXIS_Y] - minTile[ImgCreator.AXIS_Y]) * tileRequest.getTileWidth();

                        pixels[c][xy] = value.get();
                    }

                    yield new DataBufferFloat(pixels, xyPlaneSize);
                }
                case FLOAT64 -> {
                    double[][] pixels = new double[numberOfChannelsInAccessibles][xyPlaneSize];

                    while (cursor.hasNext()) {
                        DoubleType value = (DoubleType) cursor.next();

                        cursor.localize(position);
                        int c = position[ImgCreator.AXIS_CHANNEL] - minTile[ImgCreator.AXIS_CHANNEL];
                        int xy = position[ImgCreator.AXIS_X] - minTile[ImgCreator.AXIS_X] +
                                (position[ImgCreator.AXIS_Y] - minTile[ImgCreator.AXIS_Y]) * tileRequest.getTileWidth();

                        pixels[c][xy] = value.get();
                    }

                    yield new DataBufferDouble(pixels, xyPlaneSize);
                }
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
        return String.valueOf(counter.incrementAndGet());
    }

    @Override
    public Collection<URI> getURIs() {
        return List.of();
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
        private final List<RandomAccessibleInterval<T>> accessibles;
        private final PixelType pixelType;
        private ImageServerMetadata metadata;

        /**
         * Create a {@link ImgLib2ImageServer} builder.
         * <p>
         * The provided accessibles must correspond to the ones returned by functions of {@link ImgCreator}: they must have
         * {@link ImgCreator#NUMBER_OF_AXES} dimensions, the X-axes must correspond to {@link ImgCreator#AXIS_X}, and so on.
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
         * @throws NullPointerException if the provided list is null or contain a null element
         * @throws NoSuchElementException if the provided list is empty
         * @throws IllegalArgumentException if the accessible type is not among the list mentioned above, if a dimension
         * of a provided accessible contain more than {@link Integer#MAX_VALUE} pixels, if the provided accessibles do
         * not have {@link ImgCreator#NUMBER_OF_AXES} axes, if the provided accessibles do not have the same number of
         * channels, z-stacks, or timepoints, or if the accessible type is {@link ARGBType} and the number of channels
         * of the accessibles is not 1
         */
        public Builder(List<RandomAccessibleInterval<T>> accessibles) {
            checkAccessibles(accessibles);

            RandomAccessibleInterval<T> firstAccessible = accessibles.getFirst();
            T value = firstAccessible.firstElement();

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
                            ImageChannel.getDefaultChannelList((int) firstAccessible.dimension(ImgCreator.AXIS_CHANNEL))
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
            return new ImgLib2ImageServer<>(accessibles, pixelType, metadata);
        }

        private static <T extends NativeType<T> & NumericType<T>> void checkAccessibles(List<RandomAccessibleInterval<T>> accessibles) {
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
                if (accessible.numDimensions() != ImgCreator.NUMBER_OF_AXES) {
                    throw new IllegalArgumentException(String.format(
                            "The provided accessible %s does not have %d dimensions",
                            accessible,
                            ImgCreator.NUMBER_OF_AXES
                    ));
                }
            }

            Map<Integer, String> axes = Map.of(
                    ImgCreator.AXIS_CHANNEL, "number of channels",
                    ImgCreator.AXIS_Z, "number of z-stacks",
                    ImgCreator.AXIS_TIME, "number of timepoints"
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
            if (firstAccessible.firstElement() instanceof ARGBType && firstAccessible.dimension(ImgCreator.AXIS_CHANNEL) != 1) {
                throw new IllegalArgumentException(String.format(
                        "The provided accessibles %s have the ARGB type, but not one channel (found %d)",
                        accessibles,
                        firstAccessible.dimension(ImgCreator.AXIS_CHANNEL)
                ));
            }
        }

        private static <T extends NativeType<T> & NumericType<T>> void checkChannels(
                List<RandomAccessibleInterval<T>> accessibles,
                Collection<ImageChannel> channels
        ) {
            for (ImageChannel channel: channels) {
                Objects.requireNonNull(channel);
            }

            if (accessibles.getFirst().firstElement() instanceof ARGBType) {
                if (!channels.equals(ImageChannel.getDefaultRGBChannels())) {
                    throw new IllegalArgumentException(String.format(
                            "The current accessibles %s have the ARGB type, but the provided channels %s are not the default RGB channels %s",
                            accessibles,
                            channels,
                            ImageChannel.getDefaultRGBChannels()
                    ));
                }
            } else {
                if (accessibles.getFirst().dimension(ImgCreator.AXIS_CHANNEL) != channels.size()) {
                    throw new IllegalArgumentException(String.format(
                            "There are %d provided channels, but the current accessibles %s contain %s channels",
                            channels.size(),
                            accessibles,
                            accessibles.getFirst().dimension(ImgCreator.AXIS_CHANNEL)
                    ));
                }
            }
        }
    }

    private static List<ImageServerMetadata.ImageResolutionLevel> createResolutionLevels(List<? extends RandomAccessibleInterval<?>> accessibles) {
        ImageServerMetadata.ImageResolutionLevel.Builder builder = new ImageServerMetadata.ImageResolutionLevel.Builder(
                (int) accessibles.getFirst().dimension(ImgCreator.AXIS_X),
                (int) accessibles.getFirst().dimension(ImgCreator.AXIS_Y)
        );

        for (RandomAccessibleInterval<?> accessible: accessibles) {
            builder.addLevel(
                    (int) accessible.dimension(ImgCreator.AXIS_X),
                    (int) accessible.dimension(ImgCreator.AXIS_Y)
            );
        }

        return builder.build();
    }

    private RandomAccessibleInterval<T> getImgLib2Tile(TileRequest tileRequest) {
        RandomAccessibleInterval<T> wholeLevel = accessibles.get(tileRequest.getLevel());

        long[] minWholeLevel = new long[ImgCreator.NUMBER_OF_AXES];
        wholeLevel.min(minWholeLevel);

        long[] min = new long[ImgCreator.NUMBER_OF_AXES];
        min[ImgCreator.AXIS_X] = minWholeLevel[ImgCreator.AXIS_X] + tileRequest.getTileX();
        min[ImgCreator.AXIS_Y] = minWholeLevel[ImgCreator.AXIS_Y] + tileRequest.getTileY();
        min[ImgCreator.AXIS_CHANNEL] = minWholeLevel[ImgCreator.AXIS_CHANNEL];
        min[ImgCreator.AXIS_Z] = minWholeLevel[ImgCreator.AXIS_Z] + tileRequest.getZ();
        min[ImgCreator.AXIS_TIME] = minWholeLevel[ImgCreator.AXIS_TIME] + tileRequest.getT();

        long[] max = new long[ImgCreator.NUMBER_OF_AXES];   // max is inclusive, hence the -1
        max[ImgCreator.AXIS_X] = min[ImgCreator.AXIS_X] + tileRequest.getTileWidth() - 1;
        max[ImgCreator.AXIS_Y] = min[ImgCreator.AXIS_Y] + tileRequest.getTileHeight() - 1;
        max[ImgCreator.AXIS_CHANNEL] = min[ImgCreator.AXIS_CHANNEL] + numberOfChannelsInAccessibles - 1;
        max[ImgCreator.AXIS_Z] = min[ImgCreator.AXIS_Z];
        max[ImgCreator.AXIS_TIME] = min[ImgCreator.AXIS_TIME];

        return Views.interval(wholeLevel, min, max);
    }
}
