package qupath.ext.imglib2.imageserver;

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
import qupath.ext.imglib2.ImgCreator;
import qupath.lib.images.servers.AbstractTileableImageServer;
import qupath.lib.images.servers.ImageServerBuilder;
import qupath.lib.images.servers.ImageServerMetadata;
import qupath.lib.images.servers.PixelType;
import qupath.lib.images.servers.TileRequest;

import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

public class ImgLib2ImageServer<T extends NativeType<T> & NumericType<T>> extends AbstractTileableImageServer {

    private static final AtomicInteger counter = new AtomicInteger();
    private final List<RandomAccessibleInterval<T>> accessibles;
    private final ImageServerMetadata metadata;
    private final String id;

    /**
     * accessibles must be 5d, have same axis as in ImgCreator
     * provided metadata attributes copied except for width, height, ...
     *
     * @param accessibles
     * @param metadata
     * @throws NoSuchElementException if the provided list is empty
     * @throws ArithmeticException if a dimension of an accessible contain more than {@link Integer#MAX_VALUE} pixels
     * @throws IllegalArgumentException if the accessible type is not {@link ARGBType}, {@link UnsignedByteType}, {@link ByteType},
     * {@link UnsignedShortType}, {@link ShortType}, {@link UnsignedIntType}, {@link IntType}, {@link FloatType}, or {@link DoubleType},
     * or if the provided accessibles do not have the same number of channels, z-stacks, or timepoints
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
                    default -> throw new IllegalStateException(String.format("Unexpected value accessible type %s", value));
                })
                .levels(createResolutionLevels(accessibles))
                .sizeZ(Math.toIntExact(accessibles.getFirst().dimension(ImgCreator.AXIS_Z)))
                .sizeT(Math.toIntExact(accessibles.getFirst().dimension(ImgCreator.AXIS_TIME)))
                .build();

        this.id = String.valueOf(counter.incrementAndGet());
    }

    @Override
    protected BufferedImage readTile(TileRequest tileRequest) {
        return null;
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
        if (accessibles.getFirst().dimension(ImgCreator.AXIS_CHANNEL) != metadata.getSizeC()) {
            throw new IllegalArgumentException(String.format(
                    "The provided metadata contains %d channels, while the provided accessibles %s contain %s channels",
                    metadata.getSizeC(),
                    accessibles,
                    accessibles.getFirst().dimension(ImgCreator.AXIS_CHANNEL)
            ));
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
}
