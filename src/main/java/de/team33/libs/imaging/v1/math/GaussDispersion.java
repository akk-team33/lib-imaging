package de.team33.libs.imaging.v1.math;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.exp;
import static java.lang.Math.log;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

public class GaussDispersion {

    @SuppressWarnings("ConstantMathCall")
    private static final double SQRT_2_PI = sqrt(2 * Math.PI);
    private static final double MEDIAN_RADIUS_STD_DEVIATION_FACTOR = 2.0 * sqrt(2.0 * log(2.0)) / 2;

    private final double stdDeviation;
    private final double medianRadius;
    private final int effectiveRadius;
    private final double[] weights;

    @SuppressWarnings("NumericCastThatLosesPrecision")
    GaussDispersion(final double medianRadius, final double maxWeight, final double quote) {
        this.medianRadius = medianRadius;
        this.stdDeviation = medianRadius / MEDIAN_RADIUS_STD_DEVIATION_FACTOR;
        final List<Double> weightList = new ArrayList<>((int) stdDeviation);
        final double max = gauss(0.0, stdDeviation);
        final double factor = maxWeight / max;
        double next = max;
        double sum = next;
        weightList.add(factor * next);
        for (int distance = 1; sum < quote; ++distance) {
            next = gauss(distance, stdDeviation);
            sum += 2 * next;
            weightList.add(factor * next);
        }
        effectiveRadius = weightList.size() - 1;
        weights = new double[weightList.size()];
        for (int index = 0; index < weights.length; ++index) {
            weights[index] = weightList.get(index);
        }
    }

    public static GaussDispersion forRadius(final double radius) {
        return new GaussDispersion(radius, 10, 0.99);
    }

    private double gauss(final double x, final double sigma) {
        final double a = 1 / (sigma * SQRT_2_PI);
        final double b = pow(x, 2) / (2 * pow(sigma, 2));
        return a * exp(-b);
    }

    public final int getNominalRadius() {
        return (int) stdDeviation;
    }

    public final int getEffectiveRadius() {
        return effectiveRadius;
    }

    public final double getWeight(final int distance) {
        if (0 > distance) {
            //noinspection TailRecursion
            return getWeight(-distance);

        } else if (weights.length > distance) {
            return weights[distance];

        } else {
            return 0;
        }
    }

    public final int getMinDistance() {
        return -effectiveRadius;
    }
}
