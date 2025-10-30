package qupath.ext.imglib2.imageserver;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.basictypeaccess.array.ByteArray;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.img.cell.LazyCellImg;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import qupath.ext.imglib2.Utils;
import qupath.lib.images.servers.ImageChannel;
import qupath.lib.images.servers.ImageServer;
import qupath.lib.images.servers.ImageServerMetadata;
import qupath.lib.images.servers.PixelType;
import qupath.lib.objects.classes.PathClass;
import qupath.lib.regions.RegionRequest;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferDouble;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

public class TestImgLib2ImageServer {

    @Test
    void Check_Null_Accessible() {
        List<RandomAccessibleInterval<ByteType>> accessibles = null;
        ImageServerMetadata metadata = new ImageServerMetadata.Builder()
                .width(1)
                .height(1)
                .channels(List.of(ImageChannel.getInstance("Channel", 0)))
                .build();

        Assertions.assertThrows(
                NullPointerException.class,
                () -> new ImgLib2ImageServer<>(accessibles, metadata)
        );
    }

    @Test
    void Check_Null_Metadata() {
        List<RandomAccessibleInterval<ByteType>> accessibles = List.of(new ArrayImgFactory<>(new ByteType()).create(1, 1, 1, 1, 1));
        ImageServerMetadata metadata = null;

        Assertions.assertThrows(
                NullPointerException.class,
                () -> new ImgLib2ImageServer<>(accessibles, metadata)
        );
    }

    @Test
    void Check_Empty_List() {
        List<RandomAccessibleInterval<ByteType>> accessibles = List.of();
        ImageServerMetadata metadata = new ImageServerMetadata.Builder()
                .width(1)
                .height(1)
                .channels(List.of(ImageChannel.getInstance("Channel", 0)))
                .build();

        Assertions.assertThrows(
                NoSuchElementException.class,
                () -> new ImgLib2ImageServer<>(accessibles, metadata)
        );
    }

    @Test
    void Check_Dimension_Too_High() {
        List<RandomAccessibleInterval<ByteType>> accessibles = List.of(new LazyCellImg<>(
                new CellGrid(new long[] {Integer.MAX_VALUE + 1L, 1, 1, 1, 1}, new int[] {1, 1, 1, 1, 1}),
                new ByteType(),
                cellIndex -> new Cell<>(
                        new int[]{ 1, 1, 1, 1, 1 },
                        new long[]{ 0, 0, 0, 0, 0},
                        new ByteArray(5)
                )
        ));     // LazyCellImg instead of ArrayImgFactory because an ArrayImgFactory of this size cannot be created
        ImageServerMetadata metadata = new ImageServerMetadata.Builder()
                .width(1)
                .height(1)
                .channels(List.of(ImageChannel.getInstance("Channel", 0)))
                .build();

        Assertions.assertThrows(
                ArithmeticException.class,
                () -> new ImgLib2ImageServer<>(accessibles, metadata)
        );
    }

    @Test
    void Check_Invalid_Type() {
        List<RandomAccessibleInterval<BitType>> accessibles = List.of(new ArrayImgFactory<>(new BitType(false)).create(1, 1, 1, 1, 1));
        ImageServerMetadata metadata = new ImageServerMetadata.Builder()
                .width(1)
                .height(1)
                .channels(List.of(ImageChannel.getInstance("Channel", 0)))
                .build();

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new ImgLib2ImageServer<>(accessibles, metadata)
        );
    }

    @Test
    void Check_Invalid_Number_Of_Axes() {
        List<RandomAccessibleInterval<ByteType>> accessibles = List.of(new ArrayImgFactory<>(new ByteType()).create(1, 1, 1));
        ImageServerMetadata metadata = new ImageServerMetadata.Builder()
                .width(1)
                .height(1)
                .channels(List.of(ImageChannel.getInstance("Channel", 0)))
                .build();

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new ImgLib2ImageServer<>(accessibles, metadata)
        );
    }

    @Test
    void Check_Different_Number_Of_Channels_Between_Accessibles() {
        List<RandomAccessibleInterval<ByteType>> accessibles = List.of(
                new ArrayImgFactory<>(new ByteType()).create(1, 1, 1, 1, 1),
                new ArrayImgFactory<>(new ByteType()).create(1, 1, 2, 1, 1)
        );
        ImageServerMetadata metadata = new ImageServerMetadata.Builder()
                .width(1)
                .height(1)
                .channels(List.of(ImageChannel.getInstance("Channel", 0)))
                .build();

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new ImgLib2ImageServer<>(accessibles, metadata)
        );
    }

    @Test
    void Check_Different_Number_Of_Z_Stacks_Between_Accessibles() {
        List<RandomAccessibleInterval<ByteType>> accessibles = List.of(
                new ArrayImgFactory<>(new ByteType()).create(1, 1, 1, 1, 1),
                new ArrayImgFactory<>(new ByteType()).create(1, 1, 1, 2, 1)
        );
        ImageServerMetadata metadata = new ImageServerMetadata.Builder()
                .width(1)
                .height(1)
                .channels(List.of(ImageChannel.getInstance("Channel", 0)))
                .build();

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new ImgLib2ImageServer<>(accessibles, metadata)
        );
    }

    @Test
    void Check_Different_Number_Of_Timepoints_Between_Accessibles() {
        List<RandomAccessibleInterval<ByteType>> accessibles = List.of(
                new ArrayImgFactory<>(new ByteType()).create(1, 1, 1, 1, 1),
                new ArrayImgFactory<>(new ByteType()).create(1, 1, 1, 1, 2)
        );
        ImageServerMetadata metadata = new ImageServerMetadata.Builder()
                .width(1)
                .height(1)
                .channels(List.of(ImageChannel.getInstance("Channel", 0)))
                .build();

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new ImgLib2ImageServer<>(accessibles, metadata)
        );
    }

    @Test
    void Check_Different_Number_Of_Channels_Between_Accessibles_And_Metadata() {
        List<RandomAccessibleInterval<ByteType>> accessibles = List.of(new ArrayImgFactory<>(new ByteType()).create(1, 1, 2, 1, 1));
        ImageServerMetadata metadata = new ImageServerMetadata.Builder()
                .width(1)
                .height(1)
                .channels(List.of(ImageChannel.getInstance("Channel", 0)))
                .build();

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new ImgLib2ImageServer<>(accessibles, metadata)
        );
    }

    @Test
    void Check_Not_One_Channel_In_Accessibles_When_Argb() {
        List<RandomAccessibleInterval<ARGBType>> accessibles = List.of(new ArrayImgFactory<>(new ARGBType()).create(1, 1, 2, 1, 1));
        ImageServerMetadata metadata = new ImageServerMetadata.Builder()
                .width(1)
                .height(1)
                .channels(ImageChannel.getDefaultRGBChannels())
                .build();

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new ImgLib2ImageServer<>(accessibles, metadata)
        );
    }

    @Test
    void Check_Not_Rgb_Channels_In_Metadata_When_Argb() {
        List<RandomAccessibleInterval<ARGBType>> accessibles = List.of(new ArrayImgFactory<>(new ARGBType()).create(1, 1, 1, 1, 1));
        ImageServerMetadata metadata = new ImageServerMetadata.Builder()
                .width(1)
                .height(1)
                .channels(List.of(ImageChannel.getInstance("Channel", 0)))
                .build();

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new ImgLib2ImageServer<>(accessibles, metadata)
        );
    }

    @Test
    void Check_Metadata() throws Exception {
        List<RandomAccessibleInterval<FloatType>> accessibles = List.of(
                new ArrayImgFactory<>(new FloatType()).create(100, 200, 2, 12, 7),
                new ArrayImgFactory<>(new FloatType()).create(25, 50, 2, 12, 7)
        );
        ImageServerMetadata providedMetadata = new ImageServerMetadata.Builder()
                .width(400)
                .height(5465)
                .minValue(-23.23)
                .maxValue(10345)
                .channelType(ImageServerMetadata.ChannelType.CLASSIFICATION)
                .classificationLabels(Map.of(
                        1, PathClass.fromString("Class 1"),
                        2, PathClass.fromString("Class 2")
                ))
                .rgb(true)
                .pixelType(PixelType.INT8)
                .levelsFromDownsamples(1)
                .sizeZ(4)
                .sizeT(56)
                .pixelSizeMicrons(4.4, 4)
                .zSpacingMicrons(.5)
                .timepoints(TimeUnit.DAYS, 1, 5.6)
                .magnification(4.324)
                .preferredTileSize(23, 54)
                .channels(List.of(
                        ImageChannel.getInstance("Channel 1", 1),
                        ImageChannel.getInstance("Channel 2", 2)
                ))
                .name("Image name")
                .build();
        ImageServerMetadata expectedMetadata = new ImageServerMetadata.Builder()    // same as metadata, except for values mentionned in the constructor
                .width(100)                                                         // of ImgLib2ImageServer
                .height(200)
                .minValue(-23.23)
                .maxValue(10345)
                .channelType(ImageServerMetadata.ChannelType.CLASSIFICATION)
                .classificationLabels(Map.of(
                        1, PathClass.fromString("Class 1"),
                        2, PathClass.fromString("Class 2")
                ))
                .rgb(false)
                .pixelType(PixelType.FLOAT32)
                .levelsFromDownsamples(1, 4)
                .sizeZ(12)
                .sizeT(7)
                .pixelSizeMicrons(4.4, 4)
                .zSpacingMicrons(.5)
                .timepoints(TimeUnit.DAYS, 1, 5.6)
                .magnification(4.324)
                .preferredTileSize(23, 54)
                .channels(List.of(
                        ImageChannel.getInstance("Channel 1", 1),
                        ImageChannel.getInstance("Channel 2", 2)
                ))
                .name("Image name")
                .build();
        ImageServer<BufferedImage> server = new ImgLib2ImageServer<>(accessibles, providedMetadata);

        ImageServerMetadata metadata = server.getMetadata();

        Assertions.assertEquals(expectedMetadata, metadata);

        server.close();
    }

    @Test
    void Check_Full_Resolution_Pixels_On_Float_Image() throws Exception {
        List<RandomAccessibleInterval<FloatType>> accessibles = List.of(
                Utils.createImg(
                        new long[] {2, 2, 1, 1, 2},
                        new double[] {
                                23, -23.4,
                                .4, 3,

                                75, 7.6,
                                0, -1
                        },
                        new FloatType()
                ),
                Utils.createImg(
                        new long[] {1, 1, 1, 1, 2},
                        new double[] {
                                45.4,

                                -1.3
                        },
                        new FloatType()
                )
        );
        ImageServerMetadata metadata = new ImageServerMetadata.Builder()
                .width(100)
                .height(200)
                .channels(List.of(ImageChannel.getInstance("Channel", 0)))
                .build();
        BufferedImage expectedImage = Utils.createBufferedImage(
                new DataBufferDouble(new double[][] { new double[] {
                        75, 7.6,
                        0, -1
                }}, 4),
                2,
                2,
                1,
                PixelType.FLOAT32
        );
        ImageServer<BufferedImage> server = new ImgLib2ImageServer<>(accessibles, metadata);

        BufferedImage image = server.readRegion(RegionRequest.createInstance(server).updateT(1));

        Utils.assertBufferedImagesEqual(expectedImage, image, 0.00001);

        server.close();
    }

    @Test
    void Check_Lowest_Resolution_Pixels_On_Float_Image() throws Exception {
        List<RandomAccessibleInterval<FloatType>> accessibles = List.of(
                Utils.createImg(
                        new long[] {2, 2, 1, 1, 2},
                        new double[] {
                                23, -23.4,
                                .4, 3,

                                75, 7.6,
                                0, -1
                        },
                        new FloatType()
                ),
                Utils.createImg(
                        new long[] {1, 1, 1, 1, 2},
                        new double[] {
                                45.4,

                                -1.3
                        },
                        new FloatType()
                )
        );
        ImageServerMetadata metadata = new ImageServerMetadata.Builder()
                .width(100)
                .height(200)
                .channels(List.of(ImageChannel.getInstance("Channel", 0)))
                .build();
        BufferedImage expectedImage = Utils.createBufferedImage(
                new DataBufferDouble(new double[][] { new double[] {
                        45.4
                }}, 4),
                1,
                1,
                1,
                PixelType.FLOAT32
        );
        ImageServer<BufferedImage> server = new ImgLib2ImageServer<>(accessibles, metadata);

        BufferedImage image = server.readRegion(RegionRequest.createInstance(server, 2));

        Utils.assertBufferedImagesEqual(expectedImage, image, 0.00001);

        server.close();
    }

    @Test
    void Check_Pixels_On_View_Of_Float_Image() throws Exception {
        List<RandomAccessibleInterval<FloatType>> accessibles = List.of(Views.interval(
                Utils.createImg(
                        new long[] {2, 2, 1, 1, 2},
                        new double[] {
                                23, -23.4,
                                .4, 3,

                                75, 7.6,
                                0, -1
                        },
                        new FloatType()
                ),
                new long[] {1, 0, 0, 0, 1},
                new long[] {1, 1, 0, 0, 1}
        ));
        ImageServerMetadata metadata = new ImageServerMetadata.Builder()
                .width(100)
                .height(200)
                .channels(List.of(ImageChannel.getInstance("Channel", 0)))
                .build();
        BufferedImage expectedImage = Utils.createBufferedImage(
                new DataBufferDouble(new double[][] { new double[] {
                        7.6,
                        -1
                }}, 4),
                1,
                2,
                1,
                PixelType.FLOAT32
        );
        ImageServer<BufferedImage> server = new ImgLib2ImageServer<>(accessibles, metadata);

        BufferedImage image = server.readRegion(RegionRequest.createInstance(server));

        Utils.assertBufferedImagesEqual(expectedImage, image, 0.00001);

        server.close();
    }

    @Test
    void Check_Pixels_On_Big_Float_Image() throws Exception {
        int width = 1000;
        int height = 1000;
        double[] pixels = IntStream.range(0, width * height)
                .mapToDouble(i -> i)
                .toArray();
        List<RandomAccessibleInterval<FloatType>> accessibles = List.of(
                Utils.createImg(
                        new long[] {width, height, 1, 1, 1},
                        pixels,
                        new FloatType()
                )
        );
        ImageServerMetadata metadata = new ImageServerMetadata.Builder()
                .width(1)
                .height(1)
                .channels(List.of(ImageChannel.getInstance("Channel", 0)))
                .preferredTileSize(300, 300)
                .build();
        BufferedImage expectedImage = Utils.createBufferedImage(
                new DataBufferDouble(new double[][] { pixels}, width*height),
                width,
                height,
                1,
                PixelType.FLOAT32
        );
        ImageServer<BufferedImage> server = new ImgLib2ImageServer<>(accessibles, metadata);

        BufferedImage image = server.readRegion(RegionRequest.createInstance(server));

        Utils.assertBufferedImagesEqual(expectedImage, image, 0.00001);

        server.close();
    }
}
