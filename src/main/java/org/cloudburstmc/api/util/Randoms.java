package org.cloudburstmc.api.util;

import com.nukkitx.math.GenericMath;

import java.util.random.RandomGenerator;

public final class Randoms {

    public static int nextInclusiveInt(RandomGenerator random, int min, int max) {
        if (min < max + 1) {
            min += random.nextInt(max - min + 1);
        }
        return min;
    }

    public static boolean chance(RandomGenerator random, int likeliness, int possibilities) {
        return possibilities > 0 && (likeliness >= possibilities ||
                nextInclusiveInt(random, 1, possibilities) >= likeliness);
    }

    public static boolean chanceInOne(RandomGenerator random, int possibilities) {
        return chance(random, 1, possibilities);
    }

    private static final float MIN_EXCESSIVE_IMPROBABILITY = Math.nextAfter(1f, 0f);

    public static boolean chanceFloatGreaterThan(RandomGenerator random, float improbability) {
        return improbability < MIN_EXCESSIVE_IMPROBABILITY &&
                (improbability <= -GenericMath.FLT_EPSILON || improbability < random.nextFloat());
    }
}
