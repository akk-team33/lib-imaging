package de.team33.libs.imaging.v1;

import de.team33.libs.imaging.v1.doubles.Circle;
import de.team33.libs.imaging.v1.doubles.Point;

import java.util.function.BiFunction;

public class RGBPixel {

    private static final int RED_BITS = 0x00ff0000;
    private static final int GREEN_BITS = 0x0000ff00;
    private static final int BLUE_BITS = 0x000000ff;
    private static final int ALPHA_MAX = 0xff000000;
    private static final int VALUE_LIMIT = 0x10000;

    private final int red;
    private final int green;
    private final int blue;

    public RGBPixel(final int rgb) {
        this((rgb & RED_BITS) >> 8, rgb & GREEN_BITS, (rgb & BLUE_BITS) << 8);
    }

    public RGBPixel(final int red, final int green, final int blue) {
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static RGBPixel enhanced(final RGBPixel sharp, final RGBPixel blurred) {
        return new RGBPixel(
                enhanced(sharp.red, blurred.red, RGBPixel::enhancedCenter, RGBPixel::enhancedUpper),
                enhanced(sharp.green, blurred.green, RGBPixel::enhancedCenter, RGBPixel::enhancedUpper),
                enhanced(sharp.blue, blurred.blue, RGBPixel::enhancedCenter, RGBPixel::enhancedUpper)
        );
    }

    public static RGBPixel smoothed(final RGBPixel sharp, final RGBPixel blurred) {
        return new RGBPixel(
                enhanced(sharp.red, blurred.red, RGBPixel::smoothedCenter, RGBPixel::smoothedUpper),
                enhanced(sharp.green, blurred.green, RGBPixel::smoothedCenter, RGBPixel::smoothedUpper),
                enhanced(sharp.blue, blurred.blue, RGBPixel::smoothedCenter, RGBPixel::smoothedUpper)
        );
    }

    private static boolean enhancedUpper(final int sharp, final int blurred) {
        return sharp > blurred;
    }

    private static boolean smoothedUpper(final int sharp, final int blurred) {
        return sharp < blurred;
    }

    private static Point enhancedCenter(final int sharp, final int blurred) {
        return new Point((sharp < blurred) ? 0 : VALUE_LIMIT, blurred);
    }

    private static Point smoothedCenter(final int sharp, final int blurred) {
        return new Point(blurred, (sharp < blurred) ? 0 : VALUE_LIMIT);
    }

    private static int enhanced(final int sharp, final int blurred,
                                final BiFunction<Integer, Integer, Point> center,
                                final BiFunction<Integer, Integer, Boolean> upper) {
        final double radius = ((sharp < blurred) ? blurred : (VALUE_LIMIT - blurred));
        final Circle circle = new Circle(center.apply(sharp, blurred), radius, upper.apply(sharp, blurred));

        //noinspection NumericCastThatLosesPrecision
        //return (int) ((circle.y(sharp) + sharp) / 2.0);
        return (int) (circle.y(sharp));
    }

    public static int normal(final int value, final int min, final int max) {
        return (int) normalLong(value, min, max + 1L);
    }

    private static long normalLong(final long value, final long min, final long limit) {
        // (value - min) / (limit - min) = result / VALUE_LIMIT
        // ((value - min) * VALUE_LIMIT) / (limit - min) = result
        return ((value - min) * VALUE_LIMIT) / (limit - min);
    }

    public final int getRed() {
        return red;
    }

    public final int getGreen() {
        return green;
    }

    public final int getBlue() {
        return blue;
    }

    public final int toRgb() {
        return ALPHA_MAX | ((red << 8) & RED_BITS) | (green & GREEN_BITS) | ((blue >> 8) & BLUE_BITS);
    }

    @SuppressWarnings({"AccessingNonPublicFieldOfAnotherObject", "ReturnOfThis"})
    public static class Builder {
        private long red = 0;
        private long green = 0;
        private long blue = 0;
        private long count = 0;

        public final Builder add(final RGBPixel pixel, final long weight) {
            red += pixel.red * weight;
            green += pixel.green * weight;
            blue += pixel.blue * weight;
            count += weight;
            return this;
        }

        public final RGBPixel build() {
            //noinspection NumericCastThatLosesPrecision
            return new RGBPixel(
                    (int) ((red + (count / 2)) / count),
                    (int) ((green + (count / 2)) / count),
                    (int) ((blue + (count / 2)) / count)
            );
        }
    }
}
