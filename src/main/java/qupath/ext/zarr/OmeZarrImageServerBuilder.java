package qupath.ext.zarr;

import java.util.Arrays;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;
import ome.zarr.imglib2.PyramidBackend;
import ome.zarr.imglib2.PyramidContents;
import ome.zarr.imglib2.ZarrUtils;
import ome.zarr.imglib2.metadata.AxisCalibration;
import ome.zarr.imglib2.metadata.Omero;
import ome.zarr.zarrjava.ZarrJavaPyramidBackend;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.imglib2.ImgBuilder;
import qupath.ext.imglib2.ImgLib2ImageServer;
import qupath.lib.images.servers.ImageChannel;
import qupath.lib.images.servers.ImageServer;
import qupath.lib.images.servers.ImageServerBuilder;
import qupath.lib.images.servers.PixelCalibration;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class OmeZarrImageServerBuilder implements ImageServerBuilder<BufferedImage> {

    private static final Logger logger = LoggerFactory.getLogger(OmeZarrImageServerBuilder.class);

    @Override
    public UriImageSupport<BufferedImage> checkImageSupport(URI uri, String... args) throws IOException {
        float supportLevel = 0f;
        if (ZarrUtils.isZarr(uri))
            supportLevel = 5f;
        return UriImageSupport.createInstance(
                OmeZarrImageServerBuilder.class,
                supportLevel,
                DefaultImageServerBuilder.createInstance(
                        OmeZarrImageServerBuilder.class, uri, args
                ));
    }

    @Override
    public ImageServer<BufferedImage> buildServer(URI uri, String... args) throws Exception {
        return create(uri, args);
    }

    private < T extends NativeType< T > & RealType< T >> ImageServer<BufferedImage> create(URI uri, String... args) throws Exception {
        PyramidBackend backend = new ZarrJavaPyramidBackend();
        PyramidContents<T> contents = backend.read(uri);
        List<RandomAccessibleInterval<T>> resolutions = new ArrayList<>();
        AxisCalibration[] axes = contents.axesPerLevel[0];
        PixelCalibration cal = parsePixelCalibration(axes);
        for (int r = 0; r < contents.numResolutionLevels(); r++) {
            RandomAccessibleInterval<T> img = standardizeAxes(contents.asImg(r), contents);
            resolutions.add(img);
        }
        String argsString = args.length == 0 ? "" : " (" + Arrays.toString(args) + ")";
        var builder = ImgLib2ImageServer.builder(resolutions)
                .persistentId(getClass().getSimpleName() + "-" + uri + argsString);

        var channels = parseChannels(contents.omero);
        long expectedChannels = resolutions.getFirst().dimension(ImgBuilder.AXIS_CHANNEL);
        if (channels.size() == expectedChannels) {
            builder = builder.channels(channels);
        } else if (!channels.isEmpty()) {
            logger.warn("Expected {} channels, found {}", expectedChannels, channels.size());
        }
        return builder
                .pixelCalibration(cal)
                .build();
    }

    private static <T> RandomAccessibleInterval<T> standardizeAxes(RandomAccessibleInterval<T> img, PyramidContents<?> contents) {
        // Ensure the ordering is what we expect
        checkXYAxisOrThrow(contents);
        // We expect XYZCT, but some can be missing.
        // We need XYCZT, without any missing.
        int nAxes = contents.numDimensions();
        return switch (nAxes) {
            case 2 -> ensureDims(img, 5);
            case 3 -> fixAxes3(img, contents);
            case 4 -> fixAxes4(img, contents);
            case 5 -> Views.moveAxis(img, 2, 3); // Switch XYCZT to XYZCT
            default -> null;
        };
    }

    private static <T> RandomAccessibleInterval<T> fixAxes4(RandomAccessibleInterval<T> img, PyramidContents<?> contents) {
        assert img.numDimensions() == 4;
        // One axis is missing - add it, then rearrange
        img = ensureDims(img, 5);
        if (!contents.hasAxis(AxisCalibration.C)) {
            // Input: XYZTC, Output: XYCZT
            return Views.moveAxis(img, 4, 2);
        } else if (!contents.hasAxis(AxisCalibration.Z)) {
            // Input: XYCTZ, Output: XYCZT
            return Views.moveAxis(img, 4, 3);
        } else if (!contents.hasAxis(AxisCalibration.T)) {
            // Input: XYZCT, Output: XYCZT
            return Views.moveAxis(img, 2, 3);
        } else {
            throw new IllegalArgumentException("Unexpected axes " + Arrays.toString(contents.axesPerLevel[0]));
        }
    }

    private static <T> RandomAccessibleInterval<T> fixAxes3(RandomAccessibleInterval<T> img, PyramidContents<?> contents) {
        assert img.numDimensions() == 3;
        // One (non-XY) axis is present - add the missing 2, then rearrange
        img = ensureDims(img, 5);
        if (contents.hasAxis(AxisCalibration.C)) {
            // Input: XYCZT, Output: XYCZT
            return img;
        } else if (contents.hasAxis(AxisCalibration.Z)) {
            // Input: XYZCT, Output: XYCZT
            return Views.moveAxis(img, 2, 3);
        } else if (contents.hasAxis(AxisCalibration.T)) {
            // Input: XYTZC, Output: XYCZT
            img = Views.moveAxis(img, 2, 4);
            return Views.moveAxis(img, 2, 3);
        } else {
            throw new IllegalArgumentException("Unexpected axes " + Arrays.toString(contents.axesPerLevel[0]));
        }
    }

    private static <T> RandomAccessibleInterval<T> ensureDims(RandomAccessibleInterval<T> img, int nDims) {
        while (img.numDimensions() < nDims) {
            img = Views.addDimension(img, 0, 0);
        }
        return img;
    }

    /**
     * Check that the axis ordering matches our expectations of XYZCT (based upon the documentation).
     * Some axis may be missing, but based on the docs we don't expect the order can change.
     * Confirming this now means we don't need to write defensive code to handle weirdness later.
     * @param contents
     */
    private static void checkXYAxisOrThrow(PyramidContents<?> contents) {
        int lastInd = -1;
        List<String> expectedOrder = List.of(AxisCalibration.X, AxisCalibration.Y, AxisCalibration.Z, AxisCalibration.C, AxisCalibration.T);
        for (var name : expectedOrder) {
            if (contents.hasAxis(name)) {
                int ind = contents.axisIndex(name);
                if (ind < lastInd) {
                    throw new IllegalArgumentException("Unexpected axis order, expected XYZCT but got " + Arrays.toString(contents.axesPerLevel[0]));
                } else {
                    lastInd = ind;
                }
            } else if (name.equals(AxisCalibration.X) || name.equals(AxisCalibration.Y)) {
                throw new IllegalArgumentException("Missing X or Y axis");
            }
        }
    }

    private static PixelCalibration parsePixelCalibration(AxisCalibration[] axes) {
        if (axes.length < 2)
            throw new IllegalArgumentException("At least 2 axes are required for an image, got " + Arrays.toString(axes));
        if (!Objects.equals(AxisCalibration.X, axes[0].name))
            throw new IllegalArgumentException("Expected first axis to be " + AxisCalibration.X + ", found " + axes[0].name);
        if (!Objects.equals(AxisCalibration.Y, axes[1].name))
            throw new IllegalArgumentException("Expected second axis to be " + AxisCalibration.Y + ", found " + axes[1].name);
        // Original expected order is XYZCT *before* we convert for QuPath - but z may not be present
        var builder = new PixelCalibration.Builder();
        if (isMicrons(axes[0].unit) && isMicrons(axes[1].unit)) {
            builder = builder.pixelSizeMicrons(axes[0].scale, axes[1].scale);
        }
        if (axes.length >= 3 && Objects.equals(AxisCalibration.Z, axes[2].name) && isMicrons(axes[2].unit)) {
            builder = builder.zSpacingMicrons(axes[2].scale);
        }
        return builder.build();
    }

    private static final Set<String> MICRONS = Set.of(
            "µm", "um",
            "micron", "microns",
            "micrometre", "micrometres",
            "micrometer", "micrometers");

    private static boolean isMicrons(String unit) {
        return MICRONS.contains(unit);
    }

    private static List<ImageChannel> parseChannels(Omero omero) {
        List<ImageChannel> channels = new ArrayList<>();
        for (int c = 0; c < omero.channels.size(); c++) {
            var channel = omero.channels.get(c);
            String name = channel.label;
            Integer color = parseColor(channel.color).orElse(ImageChannel.getDefaultChannelColor(c));
            channels.add(ImageChannel.getInstance(name, color));
        }
        return channels;
    }

    private static Optional<Integer> parseColor(String color) {
        if (color != null && !color.isBlank()) {
            try {
                if (!color.startsWith("#")) {
                    color = "#" + color;
                }
                return Optional.of(
                        Color.decode(color).getRGB()
                );
            } catch (Exception e) {
                logger.warn("Invalid image color: {}", color);
            }
        }
        return Optional.empty();
    }

    @Override
    public String getName() {
        return "OME-Zarr server builder";
    }

    @Override
    public String getDescription() {
        return "Image server builder to support OME-Zarr";
    }

    @Override
    public Class<BufferedImage> getImageType() {
        return BufferedImage.class;
    }

}
