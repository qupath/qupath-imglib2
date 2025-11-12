# QuPath-imglib2

A Java library to link QuPath with ImgLib2.

## Sample script

Here is a sample script that shows how to use the library from QuPath:

```groovy
import qupath.ext.imglib2.ImgCreator
import qupath.ext.imglib2.ImgLib2ImageServer
import net.imglib2.type.numeric.ARGBType


var server = getCurrentServer()

// Create Img<T> from level
var level = 0
var img = ImgCreator.builder(server).build().createForLevel(level)
println img


// Create RandomAccessibleInterval<T> from downsample
var downsample = 1
var randomAccessible = ImgCreator.builder(server).build().createForDownsample(downsample)
println randomAccessible


// In previous examples, the type of the output image is not checked, which creates unchecked cast warnings
// (visible from Java, not Groovy). A better solution in Java to explicitely declare the output image type:
var type = new ARGBType()   // only valid if server represents a RGB image. Otherwise:
                            // net.imglib2.type.numeric.integer.UnsignedByteType for UINT8 images
                            // net.imglib2.type.numeric.integer.ByteType for INT8 images
                            // net.imglib2.type.numeric.integer.UnsignedShortType for UINT16 images
                            // net.imglib2.type.numeric.integer.ShortType for INT16 images
                            // net.imglib2.type.numeric.integer.UnsignedIntType for UINT32 images
                            // net.imglib2.type.numeric.integer.IntType for INT32 images
                            // net.imglib2.type.numeric.real.FloatType for FLOAT32 images
                            // net.imglib2.type.numeric.real.DoubleType for FLOAT64 images
var safeImg = ImgCreator.builder(server, type).build().createForLevel(level)
println safeImg


// Once you have an image (or random accessible), you can use regular ImgLib2 functions
// For example, to read the pixel located at [x:1, y:2; c:0; z:0; t:0]:
var randomAccess = randomAccessible.randomAccess()

var position = new long[ImgCreator.NUMBER_OF_AXES]
position[ImgCreator.AXIS_X] = 1
position[ImgCreator.AXIS_Y] = 2

var pixel = randomAccess.setPositionAndGet(position)
println pixel


// It is also possible to create an ImageServer from a RandomAccessible or Img.
var newServer = ImgLib2ImageServer.builder(List.of(randomAccessible)).build()
println newServer
```
