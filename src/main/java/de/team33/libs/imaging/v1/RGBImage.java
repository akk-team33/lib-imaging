package de.team33.libs.imaging.v1;

import de.team33.libs.imaging.v1.math.BinomialDispersion;
import de.team33.libs.imaging.v1.math.Dispersion;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class RGBImage {

    private static final Direction HORIZONTAL = (x, y, delta) -> new Point(x + delta, y);
    private static final Direction VERTICAL = (x, y, delta) -> new Point(x, y + delta);

    private final int width;
    private final int height;
    private final int type;
    private final int[] pixelValues;

    private int
            rMin = Integer.MAX_VALUE, gMin = Integer.MAX_VALUE, bMin = Integer.MAX_VALUE,
            rMax = 0, gMax = 0, bMax = 0;

    private RGBImage(final int width, final int height, final int type, final PixelSupplier supplier) {
        this.width = width;
        this.height = height;
        this.type = type;

        this.pixelValues = new int[width * height * 3];

        final ErrorHandler errorHandler = new ErrorHandler();
        final ExecutorService service = Executors.newWorkStealingPool();
        for (int y = 0; (y < height) && errorHandler.isClean(); ++y) {
//            for (int x = 0; x < width; ++x) {
//                setPixel(x, y, supplier.supply(x, y));
//            }
            service.execute(new LineWise(y, supplier, errorHandler));
            if (0 == (y % 64)) {
                System.out.print('.');
            }
        }
        try {
            if (errorHandler.isDirty()) {
                service.shutdownNow();
            } else {
                service.shutdown();
            }
            if (!service.awaitTermination(2, TimeUnit.MINUTES)) {
                throw new InterruptedException("timeout occurred");
            }
        } catch (final InterruptedException e) {
            errorHandler.accept(e);
        }
        if (errorHandler.isDirty()) {
            //noinspection ProhibitedExceptionThrown,AccessingNonPublicFieldOfAnotherObject
            throw new Error(errorHandler.caught);
        }
    }

    public static RGBImage read(final Path path) throws IOException {
        return from(ImageIO.read(path.toFile()));
    }

    private static RGBImage from(final BufferedImage image) {
        return new RGBImage(
                image.getWidth(),
                image.getHeight(),
                image.getType(),
                (x, y) -> new RGBPixel(image.getRGB(x, y))
        );
    }

    public final RGBImage write(final Format format, final Path path) throws IOException {
        ImageIO.write(build(), format.getRawName(), path.toFile());
        return this;
    }

    public final BufferedImage build() {
        final BufferedImage result = new BufferedImage(width, height, type);
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                result.setRGB(x, y, getPixel(x, y).toRgb());
            }
        }
        return result;
    }

    public final RGBImage blurred(final int radius) {
        try {
            final Dispersion dispersion = BinomialDispersion.forRadius(radius);
            return blurred(HORIZONTAL, dispersion).blurred(VERTICAL, dispersion);
        } finally {
            System.out.println(" blurred ");
        }
    }

    public final RGBImage normalized() {
        try {
            return new RGBImage(width, height, type, new Normalizing(this));
        } finally {
            System.out.println(" normalized ");
        }
    }

    private RGBImage blurred(final Direction direction, final Dispersion dispersion) {
        return new RGBImage(width, height, type, new Blurring(this, direction, dispersion));
    }

    public final RGBPixel getPixel(final Point point) {
        return getPixel(point.x, point.y);
    }

    public final RGBPixel getPixel(final int x, final int y) {
        final int index = indexOf(x, y);
        return new RGBPixel(
                pixelValues[3 * index],
                pixelValues[(3 * index) + 1],
                pixelValues[(3 * index) + 2]
        );
    }

    private void setPixel(final int x, final int y, final RGBPixel pixel) {
        setPixel(indexOf(x, y), pixel.getRed(), pixel.getGreen(), pixel.getBlue());
    }

    private void setPixel(final int index, final int r, final int g, final int b) {
        pixelValues[3 * index] = r;
        pixelValues[(3 * index) + 1] = g;
        pixelValues[(3 * index) + 2] = b;

        rMin = Math.min(rMin, r);
        gMin = Math.min(gMin, g);
        bMin = Math.min(bMin, b);

        rMax = Math.max(rMax, r);
        gMax = Math.max(gMax, g);
        bMax = Math.max(bMax, b);
    }

    private int indexOf(final int x, final int y) {
        return x + (y * width);
    }

    public final RGBImage enhanced(final RGBImage blurred) {
        try {
            return new RGBImage(width, height, type, new Enhancing(this, blurred));
        } finally {
            System.out.println(" enhanced ");
        }
    }

    public final RGBImage smoothed(final RGBImage blurred) {
        try {
            return new RGBImage(width, height, type, new Smoothing(this, blurred));
        } finally {
            System.out.println(" smoothed ");
        }
    }

    @FunctionalInterface
    private interface PixelSupplier {
        RGBPixel supply(int x, int y);
    }

    @FunctionalInterface
    private interface Direction {
        Point point(int x, int y, int delta);
    }

    private static class Normalizing implements PixelSupplier {
        private final RGBImage origin;

        private Normalizing(final RGBImage origin) {
            this.origin = origin;
        }

        @Override
        public final RGBPixel supply(final int x, final int y) {
            final RGBPixel pixel = origin.getPixel(x, y);
            return new RGBPixel(
                    RGBPixel.normal(pixel.getRed(), origin.rMin, origin.rMax),
                    RGBPixel.normal(pixel.getGreen(), origin.gMin, origin.gMax),
                    RGBPixel.normal(pixel.getBlue(), origin.bMin, origin.bMax)
            );
        }
    }

    private static class Blurring implements PixelSupplier {
        private final RGBImage origin;
        private final Direction direction;
        private final Dispersion dispersion;

        private Blurring(final RGBImage origin, final Direction direction, final Dispersion dispersion) {
            this.origin = origin;
            this.direction = direction;
            this.dispersion = dispersion;
        }

        @Override
        public final RGBPixel supply(final int x, final int y) {
            final RGBPixel.Builder builder = RGBPixel.builder();
            for (int distance = dispersion.getMinDistance(), limit = dispersion.getEffectiveRadius();
                 distance <= limit; ++distance) {

                final Point p = direction.point(x, y, distance);
                if ((0 <= p.x) && (p.x < origin.width) && (0 <= p.y) && (p.y < origin.height)) {
                    builder.add(origin.getPixel(p), dispersion.getWeight(distance));
                }
            }
            return builder.build();
        }
    }

    private static class ErrorHandler implements Consumer<Throwable> {
        private volatile Throwable caught = null;

        @Override
        public final synchronized void accept(final Throwable throwable) {
            if (null == caught) {
                caught = throwable;
            } else {
                caught.addSuppressed(throwable);
            }
        }

        public final boolean isDirty() {
            return null != caught;
        }

        public final boolean isClean() {
            return null == caught;
        }
    }

    private class LineWise implements Runnable {
        private final int y;
        private final PixelSupplier supplier;
        private final Consumer<Throwable> errorHandler;

        private LineWise(final int y, final PixelSupplier supplier, final Consumer<Throwable> errorHandler) {
            this.y = y;
            this.supplier = supplier;
            this.errorHandler = errorHandler;
        }

        @Override
        public final void run() {
            try {
                for (int x = 0; x < width; ++x) {
                    setPixel(x, y, supplier.supply(x, y));
                }
            } catch (final Throwable e) {
                errorHandler.accept(e);
            }
        }
    }

    private class Enhancing implements PixelSupplier {
        private final RGBImage sharp;
        private final RGBImage blurred;

        private Enhancing(final RGBImage sharp, final RGBImage blurred) {
            this.sharp = sharp;
            this.blurred = blurred;
        }

        @Override
        public final RGBPixel supply(final int x, final int y) {
            return RGBPixel.enhanced(sharp.getPixel(x, y), blurred.getPixel(x, y));
        }
    }

    private class Smoothing implements PixelSupplier {
        private final RGBImage sharp;
        private final RGBImage blurred;

        private Smoothing(final RGBImage sharp, final RGBImage blurred) {
            this.sharp = sharp;
            this.blurred = blurred;
        }

        @Override
        public final RGBPixel supply(final int x, final int y) {
            return RGBPixel.smoothed(sharp.getPixel(x, y), blurred.getPixel(x, y));
        }
    }
}
