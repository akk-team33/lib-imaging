package de.team33.libs.imaging.v1.math;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.exp;
import static java.lang.Math.log;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

@SuppressWarnings("NonReproducibleMathCall")
public class GaussDispersion2 {

    @SuppressWarnings("ConstantMathCall")
    private static final double SQRT_2_PI = sqrt(2.0 * Math.PI);
    private static final double MEDIAN_RADIUS_STD_DEVIATION_FACTOR = (2.0 * sqrt(2.0 * log(2.0))) / 2;

    private final double stdDeviation;
    private final double medianRadius;
    private final int effectiveRadius;
    private final int[] weights;
    private final int integral;

    @SuppressWarnings("NumericCastThatLosesPrecision")
    GaussDispersion2(final double medianRadius, final int scalingFactor) {
        this.medianRadius = medianRadius;
        this.stdDeviation = medianRadius / MEDIAN_RADIUS_STD_DEVIATION_FACTOR;
        final List<Integer> weightList = new ArrayList<>((int) medianRadius);
        int count = gauss(scalingFactor, 0, stdDeviation);
        weightList.add(count);
        //noinspection ForLoopThatDoesntUseLoopVariable
        for (int distance = 1, limit = scalingFactor; count < limit; ++distance) {
            final int next = gauss(scalingFactor, distance, stdDeviation);
            if (0 < next) {
                count += 2 * next;
                weightList.add(next);
            } else {
                limit = count;
            }
        }
        effectiveRadius = weightList.size() - 1;
        weights = new int[weightList.size()];
        for (int index = 0; index < weights.length; ++index) {
            weights[index] = weightList.get(index);
        }
        this.integral = count;
    }

    public static GaussDispersion2 forRadius(final double medianRadius) {
        final double sigma = medianRadius / MEDIAN_RADIUS_STD_DEVIATION_FACTOR;
        final double gauss2r = gauss(2 * medianRadius, sigma);
        //noinspection NumericCastThatLosesPrecision
        return new GaussDispersion2(medianRadius, (int) (1.0 / gauss2r));
    }

    private static int gauss(final int scalingFactor, final double x, final double sigma) {
        //noinspection NumericCastThatLosesPrecision
        return (int) ((scalingFactor * gauss(x, sigma)) + 0.5);
    }

    private static double gauss(final double x, final double sigma) {
        if (0.0 == sigma) {
            return (0.0 == x) ? 1.0 : 0.0;
        } else {
            final double a = 1 / (sigma * SQRT_2_PI);
            final double b = pow(x, 2) / (2 * pow(sigma, 2));
            return a * exp(-b);
        }
    }

    public final int getEffectiveRadius() {
        return effectiveRadius;
    }

    public final int getWeight(final int distance) {
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

    public double getMedianRadius() {
        return medianRadius;
    }

    public int getIntegral() {
        return integral;
    }
}
