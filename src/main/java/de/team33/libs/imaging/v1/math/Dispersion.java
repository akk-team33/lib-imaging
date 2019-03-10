package de.team33.libs.imaging.v1.math;

public interface Dispersion {

    int getMinDistance();

    int getNominalRadius();

    int getEffectiveRadius();

    int getWeight(int distance);
}
