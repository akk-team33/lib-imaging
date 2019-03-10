package de.team33.libs.imaging.v1.math;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BinomialDispersion implements Dispersion {

    private static final BigInteger MAX_WEIGHT = BigInteger.valueOf(0x00ffffff);
    private static final Map<BigInteger, BigInteger> CACHE = new HashMap<>(5000);

    static {
        for (int i = 100; i < 5000; i += 100) {
            factorial(BigInteger.valueOf(i));
        }
    }

    private final int nominalRadius;
    private final int effectiveRadius;
    private final int[] weights;

    private BinomialDispersion(final int nominalRadius) {
        this.nominalRadius = nominalRadius;
        final List<BigInteger> bigWeights = new ArrayList<>(nominalRadius + 1);
        for (int k = nominalRadius, n = 2 * nominalRadius; k <= n; ++k) {
            bigWeights.add(binomialCoefficient(n, k));
        }
        final int compression = compression(bigWeights.get(0));
        weights = new int[nominalRadius + 1];
        int rr = 0;
        for (int i = 0; i < weights.length; ++i) {
            final BigInteger bigWeight = bigWeights.get(i).shiftRight(compression);
            weights[i] = bigWeight.intValue();
            if (0 < weights[i]) {
                rr = i;
            }
        }
        effectiveRadius = rr;
    }

    private static int compression(final BigInteger reference) {
        BigInteger compressed = reference;
        int result = 0;
        while (0 < compressed.compareTo(MAX_WEIGHT)) {
            compressed = compressed.shiftRight(1);
            result += 1;
        }
        return result;
    }

    private static BigInteger binomialCoefficient(final int n, final int k) {
        return binomialCoefficient(BigInteger.valueOf(n), BigInteger.valueOf(k));
    }

    private static BigInteger binomialCoefficient(final BigInteger n, final BigInteger k) {
        return factorial(n).divide(factorial(n.subtract(k)).multiply(factorial(k)));
    }

    private static BigInteger factorial(final BigInteger n) {
        synchronized (CACHE) {
            if (CACHE.containsKey(n)) {
                return CACHE.get(n);
            } else if (0 <= BigInteger.ONE.compareTo(n)) {
                return BigInteger.ONE;
            } else {
                final BigInteger result = n.multiply(factorial(n.subtract(BigInteger.ONE)));
                CACHE.put(n, result);
                return result;
            }
        }
    }

    public static BinomialDispersion forRadius(final int radius) {
        return new BinomialDispersion(radius);
    }

    @Override
    public final int getNominalRadius() {
        return nominalRadius;
    }

    @Override
    public final int getEffectiveRadius() {
        return effectiveRadius;
    }

    @Override
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

    @Override
    public final int getMinDistance() {
        return -effectiveRadius;
    }
}
