package qupath.ext.imglib2.imageserver;

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
import qupath.ext.imglib2.ImgCreator;
import qupath.lib.color.ColorModelFactory;
import qupath.lib.images.servers.AbstractTileableImageServer;
import qupath.lib.images.servers.ImageChannel;
import qupath.lib.images.servers.ImageServerBuilder;
import qupath.lib.images.servers.ImageServerMetadata;
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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An {@link qupath.lib.images.servers.ImageServer} whose pixel values come from {@link RandomAccessibleInterval}.
 *
 * @param <T> the pixel type of the underlying {@link RandomAccessibleInterval}
 */
public class ImgLib2ImageServer<T extends NativeType<T> & NumericType<T>> extends AbstractTileableImageServer {

    private static final AtomicInteger counter = new AtomicInteger();
    private final List<RandomAccessibleInterval<T>> accessibles;
    private final ImageServerMetadata metadata;
    private final String id;
    private final int numberOfChannelsInAccessibles;

    /**
     * Create an {@link ImgLib2ImageServer} from the provided accessibles and metadata.
     * <p>
     * The provided accessibles must correspond to the ones returned by functions of {@link ImgCreator}: they must have
     * {@link ImgCreator#NUMBER_OF_AXES} dimensions, the X-axes must correspond to {@link ImgCreator#AXIS_X}, and so on.
     * <p>
     * All dimensions of the provided accessibles must contain {@link Integer#MAX_VALUE} pixels or less.
     * <p>
     * The type of the provided accessibles must be {@link ARGBType}, {@link UnsignedByteType}, {@link ByteType},
     * {@link UnsignedShortType}, {@link ShortType}, {@link UnsignedIntType}, {@link IntType}, {@link FloatType}, or
     * {@link DoubleType}.
     *
     * @param accessibles one accessible for each resolution level the image server should have, from highest to lowest resolution.
     *                    Must not be empty. Each accessible must have the same number of channels, z-stacks, and timepoints
     * @param metadata the metadata the image server should have. The width, height, number of z-stacks, number of time points, whether
     *                 the image is RGB, pixel type, and resolution level are not taken from this metadata but determined from the provided
     *                 accessibles. The channels of the provided metadata must correspond with the channel axes of the provided accessibles
     * @throws NullPointerException if one of the provided parameters is null
     * @throws NoSuchElementException if the provided list is empty
     * @throws ArithmeticException if a dimension of an accessible contain more than {@link Integer#MAX_VALUE} pixels
     * @throws IllegalArgumentException if the accessible type is not among the list mentioned above, if the provided accessibles do not have
     * {@link ImgCreator#NUMBER_OF_AXES} axes, if the provided accessibles do not have the same number of channels, z-stacks, or timepoints,
     * if the provided accessibles have a different number of channels than what the provided metadata have (or if, when the accessibles type
     * is {@link ARGBType}, the provided accessibles don't have one channel or the provided metadata doesn't contain the default RGB channels)
     */
    public ImgLib2ImageServer(List<RandomAccessibleInterval<T>> accessibles, ImageServerMetadata metadata) {
        checkInputs(accessibles, metadata);

        this.accessibles = accessibles;

        T value = accessibles.getFirst().firstElement();
        this.metadata = new ImageServerMetadata.Builder(metadata)
                .width(Math.toIntExact(accessibles.getFirst().dimension(ImgCreator.AXIS_X)))
                .height(Math.toIntExact(accessibles.getFirst().dimension(ImgCreator.AXIS_Y)))
                .rgb(value instanceof ARGBType)
                .pixelType(switch (value) {
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
                })
                .levels(createResolutionLevels(accessibles))
                .sizeZ(Math.toIntExact(accessibles.getFirst().dimension(ImgCreator.AXIS_Z)))
                .sizeT(Math.toIntExact(accessibles.getFirst().dimension(ImgCreator.AXIS_TIME)))
                .build();

        this.id = String.valueOf(counter.incrementAndGet());

        this.numberOfChannelsInAccessibles = Math.toIntExact(accessibles.getFirst().dimension(ImgCreator.AXIS_CHANNEL));
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
        return new ImgLib2ServerBuilder<>(accessibles, getMetadata());
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

    private static <T extends NativeType<T> & NumericType<T>> void checkInputs(List<RandomAccessibleInterval<T>> accessibles, ImageServerMetadata metadata) {
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

        if (accessibles.getFirst().firstElement() instanceof ARGBType) {
            if (accessibles.getFirst().dimension(ImgCreator.AXIS_CHANNEL) != 1) {
                throw new IllegalArgumentException(String.format(
                        "The provided accessibles %s have the ARGB type, but not one channel (found %d)",
                        accessibles,
                        accessibles.getFirst().dimension(ImgCreator.AXIS_CHANNEL)
                ));
            }
            if (!metadata.getChannels().equals(ImageChannel.getDefaultRGBChannels())) {
                throw new IllegalArgumentException(String.format(
                        "The provided accessibles %s have the ARGB type, but the provided metadata %s doesn't contain the default RGB channels (found %s)",
                        accessibles,
                        metadata,
                        metadata.getChannels()
                ));
            }
        } else {
            if (accessibles.getFirst().dimension(ImgCreator.AXIS_CHANNEL) != metadata.getSizeC()) {
                throw new IllegalArgumentException(String.format(
                        "The provided metadata contains %d channels, while the provided accessibles %s contain %s channels",
                        metadata.getSizeC(),
                        accessibles,
                        accessibles.getFirst().dimension(ImgCreator.AXIS_CHANNEL)
                ));
            }
        }
    }

    private static List<ImageServerMetadata.ImageResolutionLevel> createResolutionLevels(List<? extends RandomAccessibleInterval<?>> accessibles) {
        ImageServerMetadata.ImageResolutionLevel.Builder builder = new ImageServerMetadata.ImageResolutionLevel.Builder(
                Math.toIntExact(accessibles.getFirst().dimension(ImgCreator.AXIS_X)),
                Math.toIntExact(accessibles.getFirst().dimension(ImgCreator.AXIS_Y))
        );

        for (RandomAccessibleInterval<?> accessible: accessibles) {
            builder.addLevel(
                    Math.toIntExact(accessible.dimension(ImgCreator.AXIS_X)),
                    Math.toIntExact(accessible.dimension(ImgCreator.AXIS_Y))
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
