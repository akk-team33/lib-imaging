package de.team33.libs.imaging.v1.doubles;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static java.util.Objects.requireNonNull;

/**
 * Represents a half circle within a two dimensional coordinate system.
 */
public class Circle {

    private static final double UPPER_BOW = 1;
    private static final double LOWER_BOW = -1;

    private final Point center;
    private final double radius;
    private final double bow;

    public Circle(final Point center, final double radius, final boolean upper) {
        this.center = requireNonNull(center);
        this.radius = radius;
        this.bow = upper ? UPPER_BOW : LOWER_BOW;
    }

    public final Point getCenter() {
        return center;
    }

    public final double getRadius() {
        return radius;
    }

    public final boolean isUpper() {
        return 0 < bow;
    }

    public final double y(final double x) {
        return (bow * sqrt(pow(radius, 2) - pow(x - center.getX(), 2))) + center.getY();
    }

    @Override
    public final String toString() {
        return String.format("Circle{%s, %s * %s}", center, radius, bow);
    }
}
