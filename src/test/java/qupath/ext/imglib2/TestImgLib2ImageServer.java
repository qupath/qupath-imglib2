package qupath.ext.imglib2;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.basictypeaccess.array.ByteArray;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.img.cell.LazyCellImg;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import qupath.lib.images.servers.ImageChannel;
import qupath.lib.images.servers.ImageServer;
import qupath.lib.images.servers.ImageServerMetadata;
import qupath.lib.images.servers.PixelType;
import qupath.lib.objects.classes.PathClass;
import qupath.lib.regions.RegionRequest;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferDouble;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
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

    abstract static class GenericImage<T extends NativeType<T> & NumericType<T>> {

        @Test
        void Check_Full_Resolution_Pixels() throws Exception {
            List<RandomAccessibleInterval<T>> accessibles = getAccessibles();
            ImageServerMetadata metadata = new ImageServerMetadata.Builder()
                    .width(100)
                    .height(200)
                    .channels(getChannels())
                    .build();
            BufferedImage expectedImage = getExpectedFullResolutionImage();
            ImageServer<BufferedImage> server = new ImgLib2ImageServer<>(accessibles, metadata);

            BufferedImage image = server.readRegion(RegionRequest.createInstance(server).updateT(1));

            Utils.assertBufferedImagesEqual(expectedImage, image, 0.00001);

            server.close();
        }

        @Test
        void Check_Lowest_Resolution_Pixels() throws Exception {
            List<RandomAccessibleInterval<T>> accessibles = getAccessibles();
            ImageServerMetadata metadata = new ImageServerMetadata.Builder()
                    .width(100)
                    .height(200)
                    .channels(getChannels())
                    .build();
            BufferedImage expectedImage = getExpectedLowestResolutionImage();
            ImageServer<BufferedImage> server = new ImgLib2ImageServer<>(accessibles, metadata);

            BufferedImage image = server.readRegion(RegionRequest.createInstance(server, 2));

            Utils.assertBufferedImagesEqual(expectedImage, image, 0.00001);

            server.close();
        }

        @Test
        void Check_Pixels_On_View() throws Exception {
            List<RandomAccessibleInterval<T>> accessibles = List.of(Views.interval(
                    getAccessibles().getFirst(),
                    new long[] {1, 0, 0, 0, 1},
                    new long[] {1, 1, 0, 0, 1}
            ));
            ImageServerMetadata metadata = new ImageServerMetadata.Builder()
                    .width(100)
                    .height(200)
                    .channels(getChannels())
                    .build();
            BufferedImage expectedImage = getExpectedViewOfImage();
            ImageServer<BufferedImage> server = new ImgLib2ImageServer<>(accessibles, metadata);

            BufferedImage image = server.readRegion(RegionRequest.createInstance(server));

            Utils.assertBufferedImagesEqual(expectedImage, image, 0.00001);

            server.close();
        }

        @Test
        void Check_Pixels_On_Big_Image() throws Exception {
            List<RandomAccessibleInterval<T>> accessibles = getBigAccessibles();
            ImageServerMetadata metadata = new ImageServerMetadata.Builder()
                    .width(1)
                    .height(1)
                    .channels(getChannels())
                    .preferredTileSize(75, 75)
                    .build();
            BufferedImage expectedImage = getExpectedBigImage();
            ImageServer<BufferedImage> server = new ImgLib2ImageServer<>(accessibles, metadata);

            BufferedImage image = server.readRegion(RegionRequest.createInstance(server));

            Utils.assertBufferedImagesEqual(expectedImage, image, 0.00001);

            server.close();
        }

        abstract protected List<RandomAccessibleInterval<T>> getAccessibles();

        abstract protected List<RandomAccessibleInterval<T>> getBigAccessibles();

        abstract protected List<ImageChannel> getChannels();

        abstract protected BufferedImage getExpectedFullResolutionImage();

        abstract protected BufferedImage getExpectedLowestResolutionImage();

        abstract protected BufferedImage getExpectedViewOfImage();

        abstract protected BufferedImage getExpectedBigImage();
    }

    @Nested
    class ArgbImage extends GenericImage<ARGBType> {

        @Override
        protected List<RandomAccessibleInterval<ARGBType>> getAccessibles() {
            return List.of(
                    Utils.createArgbImg(
                            new long[] {2, 2, 1, 1, 2},
                            new int[] {
                                    ARGBType.rgba(43, 65, 33, 0), ARGBType.rgba(45, 5, 133, 255),
                                    ARGBType.rgba(37, 5, 223, 2), ARGBType.rgba(4, 33, 66, 87),

                                    ARGBType.rgba(43, 65, 33, 0), ARGBType.rgba(45, 5, 133, 255),
                                    ARGBType.rgba(37, 5, 223, 2), ARGBType.rgba(4, 33, 66, 87)
                            }
                    ),
                    Utils.createArgbImg(
                            new long[] {1, 1, 1, 1, 2},
                            new int[] {
                                    ARGBType.rgba(32, 165, 233, 30),

                                    ARGBType.rgba(3, 16, 255, 3)
                            }
                    )
            );
        }

        @Override
        protected List<RandomAccessibleInterval<ARGBType>> getBigAccessibles() {
            int width = 1000;
            int height = 1000;

            return List.of(Utils.createArgbImg(
                    new long[] {width, height, 1, 1, 1},
                    IntStream.range(0, width * height)
                            .map(i -> ARGBType.rgba(120, i % 255, 0, 255))
                            .toArray()
            ));
        }

        @Override
        protected List<ImageChannel> getChannels() {
            return ImageChannel.getDefaultRGBChannels();
        }

        @Override
        protected BufferedImage getExpectedFullResolutionImage() {
            return Utils.createArgbBufferedImage(
                    2,
                    2,
                    new int[] {
                            ARGBType.rgba(43, 65, 33, 0), ARGBType.rgba(45, 5, 133, 255),
                            ARGBType.rgba(37, 5, 223, 2), ARGBType.rgba(4, 33, 66, 87)
                    }
            );
        }

        @Override
        protected BufferedImage getExpectedLowestResolutionImage() {
            return Utils.createArgbBufferedImage(
                    1,
                    1,
                    new int[] {
                            ARGBType.rgba(32, 165, 233, 30)
                    }
            );
        }

        @Override
        protected BufferedImage getExpectedViewOfImage() {
            return Utils.createArgbBufferedImage(
                    1,
                    2,
                    new int[] {
                            ARGBType.rgba(45, 5, 133, 255),
                            ARGBType.rgba(4, 33, 66, 87)
                    }
            );
        }

        @Override
        protected BufferedImage getExpectedBigImage() {
            int width = 1000;
            int height = 1000;

            return Utils.createArgbBufferedImage(
                    width,
                    height,
                    IntStream.range(0, width * height)
                            .map(i -> ARGBType.rgba(120, i % 255, 0, 255))
                            .toArray()
            );
        }
    }

    @Nested
    class Uint8Image extends GenericImage<UnsignedByteType> {

        @Override
        protected List<RandomAccessibleInterval<UnsignedByteType>> getAccessibles() {
            return List.of(
                    Utils.createImg(
                            new long[] {2, 2, 1, 1, 2},
                            new double[] {
                                    23, 23,
                                    4, 3,

                                    75, 7,
                                    0, 1
                            },
                            new UnsignedByteType()
                    ),
                    Utils.createImg(
                            new long[] {1, 1, 1, 1, 2},
                            new double[] {
                                    45,

                                    3
                            },
                            new UnsignedByteType()
                    )
            );
        }

        @Override
        protected List<RandomAccessibleInterval<UnsignedByteType>> getBigAccessibles() {
            int width = 1000;
            int height = 1000;

            return List.of(Utils.createImg(
                    new long[] {width, height, 1, 1, 1},
                    IntStream.range(0, width * height)
                            .mapToDouble(i -> i % 255)
                            .toArray(),
                    new UnsignedByteType()
            ));
        }

        @Override
        protected List<ImageChannel> getChannels() {
            return List.of(ImageChannel.getInstance("Channel", 0));
        }

        @Override
        protected BufferedImage getExpectedFullResolutionImage() {
            return Utils.createBufferedImage(
                    new DataBufferByte(new byte[][] { new byte[] {
                            75, 7,
                            0, 1
                    }}, 4),
                    2,
                    2,
                    1,
                    PixelType.UINT8
            );
        }

        @Override
        protected BufferedImage getExpectedLowestResolutionImage() {
            return Utils.createBufferedImage(
                    new DataBufferByte(new byte[][] { new byte[] {
                            45
                    }}, 4),
                    1,
                    1,
                    1,
                    PixelType.UINT8
            );
        }

        @Override
        protected BufferedImage getExpectedViewOfImage() {
            return Utils.createBufferedImage(
                    new DataBufferByte(new byte[][] { new byte[] {
                            7,
                            1
                    }}, 4),
                    1,
                    2,
                    1,
                    PixelType.UINT8
            );
        }

        @Override
        protected BufferedImage getExpectedBigImage() {
            int width = 1000;
            int height = 1000;
            byte[] pixels = new byte[width * height];
            for (int i=0; i<pixels.length; i++) {
                pixels[i] = (byte) (i % 255);
            }

            return Utils.createBufferedImage(
                    new DataBufferByte(
                            new byte[][] { pixels },
                            width*height
                    ),
                    width,
                    height,
                    1,
                    PixelType.UINT8
            );
        }
    }

    @Nested
    class Int8Image extends GenericImage<ByteType> {

        @Override
        protected List<RandomAccessibleInterval<ByteType>> getAccessibles() {
            return List.of(
                    Utils.createImg(
                            new long[] {2, 2, 1, 1, 2},
                            new double[] {
                                    23, -23,
                                    4, 3,

                                    75, 7,
                                    0, -1
                            },
                            new ByteType()
                    ),
                    Utils.createImg(
                            new long[] {1, 1, 1, 1, 2},
                            new double[] {
                                    45,

                                    -3
                            },
                            new ByteType()
                    )
            );
        }

        @Override
        protected List<RandomAccessibleInterval<ByteType>> getBigAccessibles() {
            int width = 1000;
            int height = 1000;

            return List.of(Utils.createImg(
                    new long[] {width, height, 1, 1, 1},
                    IntStream.range(0, width * height)
                            .mapToDouble(i -> i % 255 - 128)
                            .toArray(),
                    new ByteType()
            ));
        }

        @Override
        protected List<ImageChannel> getChannels() {
            return List.of(ImageChannel.getInstance("Channel", 0));
        }

        @Override
        protected BufferedImage getExpectedFullResolutionImage() {
            return Utils.createBufferedImage(
                    new DataBufferByte(new byte[][] { new byte[] {
                            75, 7,
                            0, -1
                    }}, 4),
                    2,
                    2,
                    1,
                    PixelType.INT8
            );
        }

        @Override
        protected BufferedImage getExpectedLowestResolutionImage() {
            return Utils.createBufferedImage(
                    new DataBufferByte(new byte[][] { new byte[] {
                            45
                    }}, 4),
                    1,
                    1,
                    1,
                    PixelType.INT8
            );
        }

        @Override
        protected BufferedImage getExpectedViewOfImage() {
            return Utils.createBufferedImage(
                    new DataBufferByte(new byte[][] { new byte[] {
                            7,
                            -1
                    }}, 4),
                    1,
                    2,
                    1,
                    PixelType.INT8
            );
        }

        @Override
        protected BufferedImage getExpectedBigImage() {
            int width = 1000;
            int height = 1000;
            byte[] pixels = new byte[width * height];
            for (int i=0; i<pixels.length; i++) {
                pixels[i] = (byte) (i % 255 - 128);
            }

            return Utils.createBufferedImage(
                    new DataBufferByte(
                            new byte[][] { pixels },
                            width*height
                    ),
                    width,
                    height,
                    1,
                    PixelType.INT8
            );
        }
    }

    @Nested
    class Uint16Image extends GenericImage<UnsignedShortType> {

        @Override
        protected List<RandomAccessibleInterval<UnsignedShortType>> getAccessibles() {
            return List.of(
                    Utils.createImg(
                            new long[] {2, 2, 1, 1, 2},
                            new double[] {
                                    23, 23,
                                    4, 3,

                                    75, 7,
                                    0, 1
                            },
                            new UnsignedShortType()
                    ),
                    Utils.createImg(
                            new long[] {1, 1, 1, 1, 2},
                            new double[] {
                                    45,

                                    3
                            },
                            new UnsignedShortType()
                    )
            );
        }

        @Override
        protected List<RandomAccessibleInterval<UnsignedShortType>> getBigAccessibles() {
            int width = 1000;
            int height = 1000;

            return List.of(Utils.createImg(
                    new long[] {width, height, 1, 1, 1},
                    IntStream.range(0, width * height)
                            .mapToDouble(i -> i)
                            .toArray(),
                    new UnsignedShortType()
            ));
        }

        @Override
        protected List<ImageChannel> getChannels() {
            return List.of(ImageChannel.getInstance("Channel", 0));
        }

        @Override
        protected BufferedImage getExpectedFullResolutionImage() {
            return Utils.createBufferedImage(
                    new DataBufferUShort(new short[][] { new short[] {
                            75, 7,
                            0, 1
                    }}, 4),
                    2,
                    2,
                    1,
                    PixelType.UINT16
            );
        }

        @Override
        protected BufferedImage getExpectedLowestResolutionImage() {
            return Utils.createBufferedImage(
                    new DataBufferUShort(new short[][] { new short[] {
                            45
                    }}, 4),
                    1,
                    1,
                    1,
                    PixelType.UINT16
            );
        }

        @Override
        protected BufferedImage getExpectedViewOfImage() {
            return Utils.createBufferedImage(
                    new DataBufferUShort(new short[][] { new short[] {
                            7,
                            1
                    }}, 4),
                    1,
                    2,
                    1,
                    PixelType.UINT16
            );
        }

        @Override
        protected BufferedImage getExpectedBigImage() {
            int width = 1000;
            int height = 1000;
            short[] pixels = new short[width * height];
            for (int i=0; i<pixels.length; i++) {
                pixels[i] = (short) i;
            }

            return Utils.createBufferedImage(
                    new DataBufferUShort(
                            new short[][] { pixels },
                            width*height
                    ),
                    width,
                    height,
                    1,
                    PixelType.UINT16
            );
        }
    }

    @Nested
    class Int16Image extends GenericImage<ShortType> {

        @Override
        protected List<RandomAccessibleInterval<ShortType>> getAccessibles() {
            return List.of(
                    Utils.createImg(
                            new long[] {2, 2, 1, 1, 2},
                            new double[] {
                                    23, -23,
                                    4, 3,

                                    75, 7,
                                    0, -1
                            },
                            new ShortType()
                    ),
                    Utils.createImg(
                            new long[] {1, 1, 1, 1, 2},
                            new double[] {
                                    45,

                                    -3
                            },
                            new ShortType()
                    )
            );
        }

        @Override
        protected List<RandomAccessibleInterval<ShortType>> getBigAccessibles() {
            int width = 1000;
            int height = 1000;

            return List.of(Utils.createImg(
                    new long[] {width, height, 1, 1, 1},
                    IntStream.range(0, width * height)
                            .mapToDouble(i -> i)
                            .toArray(),
                    new ShortType()
            ));
        }

        @Override
        protected List<ImageChannel> getChannels() {
            return List.of(ImageChannel.getInstance("Channel", 0));
        }

        @Override
        protected BufferedImage getExpectedFullResolutionImage() {
            return Utils.createBufferedImage(
                    new DataBufferShort(new short[][] { new short[] {
                            75, 7,
                            0, -1
                    }}, 4),
                    2,
                    2,
                    1,
                    PixelType.INT16
            );
        }

        @Override
        protected BufferedImage getExpectedLowestResolutionImage() {
            return Utils.createBufferedImage(
                    new DataBufferShort(new short[][] { new short[] {
                            45
                    }}, 4),
                    1,
                    1,
                    1,
                    PixelType.INT16
            );
        }

        @Override
        protected BufferedImage getExpectedViewOfImage() {
            return Utils.createBufferedImage(
                    new DataBufferShort(new short[][] { new short[] {
                            7,
                            -1
                    }}, 4),
                    1,
                    2,
                    1,
                    PixelType.INT16
            );
        }

        @Override
        protected BufferedImage getExpectedBigImage() {
            int width = 1000;
            int height = 1000;
            short[] pixels = new short[width * height];
            for (int i=0; i<pixels.length; i++) {
                pixels[i] = (short) i;
            }

            return Utils.createBufferedImage(
                    new DataBufferShort(
                            new short[][] { pixels },
                            width*height
                    ),
                    width,
                    height,
                    1,
                    PixelType.INT16
            );
        }
    }

    @Nested
    class Uint32Image extends GenericImage<UnsignedIntType> {

        @Override
        protected List<RandomAccessibleInterval<UnsignedIntType>> getAccessibles() {
            return List.of(
                    Utils.createImg(
                            new long[] {2, 2, 1, 1, 2},
                            new double[] {
                                    23, 23,
                                    4, 3,

                                    75, 7,
                                    0, 1
                            },
                            new UnsignedIntType()
                    ),
                    Utils.createImg(
                            new long[] {1, 1, 1, 1, 2},
                            new double[] {
                                    45,

                                    3
                            },
                            new UnsignedIntType()
                    )
            );
        }

        @Override
        protected List<RandomAccessibleInterval<UnsignedIntType>> getBigAccessibles() {
            int width = 1000;
            int height = 1000;

            return List.of(Utils.createImg(
                    new long[] {width, height, 1, 1, 1},
                    IntStream.range(0, width * height)
                            .mapToDouble(i -> i)
                            .toArray(),
                    new UnsignedIntType()
            ));
        }

        @Override
        protected List<ImageChannel> getChannels() {
            return List.of(ImageChannel.getInstance("Channel", 0));
        }

        @Override
        protected BufferedImage getExpectedFullResolutionImage() {
            return Utils.createBufferedImage(
                    new DataBufferInt(new int[][] { new int[] {
                            75, 7,
                            0, 1
                    }}, 4),
                    2,
                    2,
                    1,
                    PixelType.UINT32
            );
        }

        @Override
        protected BufferedImage getExpectedLowestResolutionImage() {
            return Utils.createBufferedImage(
                    new DataBufferInt(new int[][] { new int[] {
                            45
                    }}, 4),
                    1,
                    1,
                    1,
                    PixelType.UINT32
            );
        }

        @Override
        protected BufferedImage getExpectedViewOfImage() {
            return Utils.createBufferedImage(
                    new DataBufferInt(new int[][] { new int[] {
                            7,
                            1
                    }}, 4),
                    1,
                    2,
                    1,
                    PixelType.UINT32
            );
        }

        @Override
        protected BufferedImage getExpectedBigImage() {
            int width = 1000;
            int height = 1000;

            return Utils.createBufferedImage(
                    new DataBufferInt(
                            new int[][] { IntStream.range(0, width * height).toArray() },
                            width*height
                    ),
                    width,
                    height,
                    1,
                    PixelType.UINT32
            );
        }
    }

    @Nested
    class Int32Image extends GenericImage<IntType> {

        @Override
        protected List<RandomAccessibleInterval<IntType>> getAccessibles() {
            return List.of(
                    Utils.createImg(
                            new long[] {2, 2, 1, 1, 2},
                            new double[] {
                                    23, -23,
                                    4, 3,

                                    75, 7,
                                    0, -1
                            },
                            new IntType()
                    ),
                    Utils.createImg(
                            new long[] {1, 1, 1, 1, 2},
                            new double[] {
                                    45,

                                    -3
                            },
                            new IntType()
                    )
            );
        }

        @Override
        protected List<RandomAccessibleInterval<IntType>> getBigAccessibles() {
            int width = 1000;
            int height = 1000;

            return List.of(Utils.createImg(
                    new long[] {width, height, 1, 1, 1},
                    IntStream.range(0, width * height)
                            .mapToDouble(i -> i)
                            .toArray(),
                    new IntType()
            ));
        }

        @Override
        protected List<ImageChannel> getChannels() {
            return List.of(ImageChannel.getInstance("Channel", 0));
        }

        @Override
        protected BufferedImage getExpectedFullResolutionImage() {
            return Utils.createBufferedImage(
                    new DataBufferInt(new int[][] { new int[] {
                            75, 7,
                            0, -1
                    }}, 4),
                    2,
                    2,
                    1,
                    PixelType.INT32
            );
        }

        @Override
        protected BufferedImage getExpectedLowestResolutionImage() {
            return Utils.createBufferedImage(
                    new DataBufferInt(new int[][] { new int[] {
                            45
                    }}, 4),
                    1,
                    1,
                    1,
                    PixelType.INT32
            );
        }

        @Override
        protected BufferedImage getExpectedViewOfImage() {
            return Utils.createBufferedImage(
                    new DataBufferInt(new int[][] { new int[] {
                            7,
                            -1
                    }}, 4),
                    1,
                    2,
                    1,
                    PixelType.INT32
            );
        }

        @Override
        protected BufferedImage getExpectedBigImage() {
            int width = 1000;
            int height = 1000;

            return Utils.createBufferedImage(
                    new DataBufferInt(
                            new int[][] { IntStream.range(0, width * height).toArray() },
                            width*height
                    ),
                    width,
                    height,
                    1,
                    PixelType.INT32
            );
        }
    }

    @Nested
    class FloatImage extends GenericImage<FloatType> {

        @Override
        protected List<RandomAccessibleInterval<FloatType>> getAccessibles() {
            return List.of(
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
        }

        @Override
        protected List<RandomAccessibleInterval<FloatType>> getBigAccessibles() {
            int width = 1000;
            int height = 1000;

            return List.of(Utils.createImg(
                    new long[] {width, height, 1, 1, 1},
                    IntStream.range(0, width * height)
                            .mapToDouble(i -> i)
                            .toArray(),
                    new FloatType()
            ));
        }

        @Override
        protected List<ImageChannel> getChannels() {
            return List.of(ImageChannel.getInstance("Channel", 0));
        }

        @Override
        protected BufferedImage getExpectedFullResolutionImage() {
            return Utils.createBufferedImage(
                    new DataBufferFloat(new float[][] { new float[] {
                            75, 7.6f,
                            0, -1
                    }}, 4),
                    2,
                    2,
                    1,
                    PixelType.FLOAT32
            );
        }

        @Override
        protected BufferedImage getExpectedLowestResolutionImage() {
            return Utils.createBufferedImage(
                    new DataBufferFloat(new float[][] { new float[] {
                            45.4f
                    }}, 4),
                    1,
                    1,
                    1,
                    PixelType.FLOAT32
            );
        }

        @Override
        protected BufferedImage getExpectedViewOfImage() {
            return Utils.createBufferedImage(
                    new DataBufferFloat(new float[][] { new float[] {
                            7.6f,
                            -1
                    }}, 4),
                    1,
                    2,
                    1,
                    PixelType.FLOAT32
            );
        }

        @Override
        protected BufferedImage getExpectedBigImage() {
            int width = 1000;
            int height = 1000;
            float[] pixels = new float[width * height];
            for (int i=0; i<pixels.length; i++) {
                pixels[i] = i;
            }

            return Utils.createBufferedImage(
                    new DataBufferFloat(
                            new float[][] { pixels },
                            width*height
                    ),
                    width,
                    height,
                    1,
                    PixelType.FLOAT32
            );
        }
    }

    @Nested
    class DoubleImage extends GenericImage<DoubleType> {

        @Override
        protected List<RandomAccessibleInterval<DoubleType>> getAccessibles() {
            return List.of(
                    Utils.createImg(
                            new long[] {2, 2, 1, 1, 2},
                            new double[] {
                                    23, -23.4,
                                    .4, 3,

                                    75, 7.6,
                                    0, -1
                            },
                            new DoubleType()
                    ),
                    Utils.createImg(
                            new long[] {1, 1, 1, 1, 2},
                            new double[] {
                                    45.4,

                                    -1.3
                            },
                            new DoubleType()
                    )
            );
        }

        @Override
        protected List<RandomAccessibleInterval<DoubleType>> getBigAccessibles() {
            int width = 1000;
            int height = 1000;

            return List.of(Utils.createImg(
                    new long[] {width, height, 1, 1, 1},
                    IntStream.range(0, width * height)
                            .mapToDouble(i -> i)
                            .toArray(),
                    new DoubleType()
            ));
        }

        @Override
        protected List<ImageChannel> getChannels() {
            return List.of(ImageChannel.getInstance("Channel", 0));
        }

        @Override
        protected BufferedImage getExpectedFullResolutionImage() {
            return Utils.createBufferedImage(
                    new DataBufferDouble(new double[][] { new double[] {
                            75, 7.6,
                            0, -1
                    }}, 4),
                    2,
                    2,
                    1,
                    PixelType.FLOAT64
            );
        }

        @Override
        protected BufferedImage getExpectedLowestResolutionImage() {
            return Utils.createBufferedImage(
                    new DataBufferDouble(new double[][] { new double[] {
                            45.4
                    }}, 4),
                    1,
                    1,
                    1,
                    PixelType.FLOAT64
            );
        }

        @Override
        protected BufferedImage getExpectedViewOfImage() {
            return Utils.createBufferedImage(
                    new DataBufferDouble(new double[][] { new double[] {
                            7.6,
                            -1
                    }}, 4),
                    1,
                    2,
                    1,
                    PixelType.FLOAT64
            );
        }

        @Override
        protected BufferedImage getExpectedBigImage() {
            int width = 1000;
            int height = 1000;

            return Utils.createBufferedImage(
                    new DataBufferDouble(
                            new double[][] { IntStream.range(0, width * height)
                                    .mapToDouble(i -> i)
                                    .toArray()
                            },
                            width*height
                    ),
                    width,
                    height,
                    1,
                    PixelType.FLOAT64
            );
        }
    }
}
