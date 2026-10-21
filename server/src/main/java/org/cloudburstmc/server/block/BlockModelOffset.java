package org.cloudburstmc.server.block;

import org.cloudburstmc.api.util.VoxelShape;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.List;

public record BlockModelOffset(Axis x, Axis y, Axis z) {

    private static final double TOLERANCE = 0.00002;

    public static BlockModelOffset fromSamples(List<Sample> samples, String name) {
        if (samples.size() < 2) {
            throw new IllegalArgumentException("Insufficient model-position samples for " + name);
        }

        return new BlockModelOffset(fitAxis(samples, 0, name), fitAxis(samples, 1, name), fitAxis(samples, 2, name));
    }

    public VoxelShape apply(VoxelShape shape, Vector3i position) {
        if (shape.isEmpty()) {
            return shape;
        }

        int indices = indices(position);
        return shape.move(this.x.value(indices >>> 8), this.y.value(indices >>> 4 & 15), this.z.value(indices & 15));
    }

    private static Axis fitAxis(List<Sample> samples, int axis, String name) {
        int minimumIndex = 16;
        int maximumIndex = -1;
        double minimumValue = 0;
        double maximumValue = 0;
        int shift = (2 - axis) * 4;

        for (Sample sample : samples) {
            int index = indices(sample.position()) >>> shift & 15;
            double value = sampleOffset(sample, axis);

            if (index < minimumIndex) {
                minimumIndex = index;
                minimumValue = value;
            }

            if (index > maximumIndex) {
                maximumIndex = index;
                maximumValue = value;
            }
        }

        if (minimumIndex == maximumIndex) {
            throw new IllegalArgumentException("Insufficient model-position index coverage for " + name);
        }

        double step = (maximumValue - minimumValue) / (maximumIndex - minimumIndex);
        Axis fitted = new Axis(minimumValue - minimumIndex * step, step);

        for (Sample sample : samples) {
            int index = indices(sample.position()) >>> shift & 15;
            double observed = sampleOffset(sample, axis);

            if (Math.abs(fitted.value(index) - observed) > TOLERANCE) {
                throw new IllegalArgumentException("Unsupported model-position function for " + name + " on axis " + axis + " at " + sample.position());
            }
        }

        return fitted;
    }

    private static double sampleOffset(Sample sample, int axis) {
        return switch (axis) {
            case 0 -> (double) sample.nativeResult().getX() - sample.position().getX();
            case 1 -> (double) sample.nativeResult().getY() - sample.position().getY();
            case 2 -> (double) sample.nativeResult().getZ() - sample.position().getZ();
            default -> throw new IllegalArgumentException("Unknown axis: " + axis);
        };
    }

    private static int indices(Vector3i position) {
        long mixed = (long) position.getX() * 3129871L ^ (long) position.getZ() * 116129781L;
        mixed *= mixed * 42317861L + 11L;

        long seed = (int) (mixed >>> 16) ^ 0x6A09E667F3BCC909L;
        long low = mix(seed);
        long high = mix(seed + 0x9E3779B97F4A7C15L);

        int indices = 0;
        for (int draw = 0; draw < 3; draw++) {
            long random = Long.rotateLeft(low + high, 17) + low;
            indices = indices * 16 + (int) (random >>> 60);

            high ^= low;
            low = Long.rotateLeft(low, 49) ^ high ^ (high << 21);
            high = Long.rotateLeft(high, 28);
        }

        return indices;
    }

    private static long mix(long value) {
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }

    public record Axis(double minimum, double step) {

        public Axis {
            if (!Double.isFinite(minimum) || !Double.isFinite(step) || !Float.isFinite((float) minimum) || !Float.isFinite((float) (minimum + 15 * step))) {
                throw new IllegalArgumentException("Model offsets must be finite");
            }
        }

        public float value(int index) {
            return (float) (this.minimum + index * this.step);
        }
    }

    public record Sample(Vector3i position, Vector3f nativeResult) {
    }
}
