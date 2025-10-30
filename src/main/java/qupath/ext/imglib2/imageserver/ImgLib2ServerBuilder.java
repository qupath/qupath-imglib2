package qupath.ext.imglib2.imageserver;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;
import qupath.lib.images.servers.ImageServer;
import qupath.lib.images.servers.ImageServerBuilder;
import qupath.lib.images.servers.ImageServerMetadata;

import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A {@link qupath.lib.images.servers.ImageServerBuilder.ServerBuilder} that creates {@link ImgLib2ImageServer}.
 *
 * @param <T> the pixel type of the underlying {@link RandomAccessibleInterval} of the image server
 */
public class ImgLib2ServerBuilder<T extends NativeType<T> & NumericType<T>> implements ImageServerBuilder.ServerBuilder<BufferedImage> {

    private final List<RandomAccessibleInterval<T>> accessibles;
    private final ImageServerMetadata metadata;

    /**
     * Create an {@link ImgLib2ServerBuilder} from the provided accessibles and metadata.
     * <p>
     * See {@link ImgLib2ImageServer#ImgLib2ImageServer(List, ImageServerMetadata)} for more information. Calling this
     * constructor with invalid arguments won't throw exceptions but the following call to {@link #build()} will.
     */
    public ImgLib2ServerBuilder(List<RandomAccessibleInterval<T>> accessibles, ImageServerMetadata metadata) {
        this.accessibles = accessibles;
        this.metadata = metadata;
    }

    @Override
    public ImageServer<BufferedImage> build() {
        return new ImgLib2ImageServer<>(accessibles, metadata);
    }

    @Override
    public Collection<URI> getURIs() {
        return List.of();
    }

    @Override
    public ImageServerBuilder.ServerBuilder<BufferedImage> updateURIs(Map<URI, URI> updateMap) {
        return new ImgLib2ServerBuilder<>(accessibles, metadata);
    }

    @Override
    public Optional<ImageServerMetadata> getMetadata() {
        return Optional.of(metadata);
    }
}
