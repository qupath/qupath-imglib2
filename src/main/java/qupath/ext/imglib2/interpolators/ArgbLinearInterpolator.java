package qupath.ext.imglib2.interpolators;

import net.imglib2.RandomAccessible;
import net.imglib2.interpolation.randomaccess.NLinearInterpolator2D;
import net.imglib2.type.numeric.ARGBType;

/**
 * A {@link NLinearInterpolator2D} that performs intermediary operations with doubles, to avoid precision errors.
 */
class ArgbLinearInterpolator extends NLinearInterpolator2D<ARGBType> {

    /**
     * Create an instance of this class.
     *
     * @param randomAccessible the accessible to interpolate
     */
    public ArgbLinearInterpolator(RandomAccessible<ARGBType> randomAccessible) {
        super(randomAccessible);
    }

    @Override
    public ARGBType get()
    {
        fillWeights();

        int pixelValue;
        double red = 0;
        double green = 0;
        double blue = 0;
        double alpha = 0;

        pixelValue = target.get().get();
        red += ARGBType.red(pixelValue) * weights[0];
        green += ARGBType.green(pixelValue) * weights[0];
        blue += ARGBType.blue(pixelValue) * weights[0];
        alpha += ARGBType.alpha(pixelValue) * weights[0];

        target.fwd(0);
        pixelValue = target.get().get();
        red += ARGBType.red(pixelValue) * weights[1];
        green += ARGBType.green(pixelValue) * weights[1];
        blue += ARGBType.blue(pixelValue) * weights[1];
        alpha += ARGBType.alpha(pixelValue) * weights[1];

        target.fwd(1);
        pixelValue = target.get().get();
        red += ARGBType.red(pixelValue) * weights[3];
        green += ARGBType.green(pixelValue) * weights[3];
        blue += ARGBType.blue(pixelValue) * weights[3];
        alpha += ARGBType.alpha(pixelValue) * weights[3];

        target.bck(0);
        pixelValue = target.get().get();
        red += ARGBType.red(pixelValue) * weights[2];
        green += ARGBType.green(pixelValue) * weights[2];
        blue += ARGBType.blue(pixelValue) * weights[2];
        alpha += ARGBType.alpha(pixelValue) * weights[2];

        target.bck(1);

        accumulator.set(ARGBType.rgba(red, green, blue, alpha));

        return accumulator;
    }
}
