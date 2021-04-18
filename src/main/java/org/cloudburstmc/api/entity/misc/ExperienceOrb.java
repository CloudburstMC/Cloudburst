package org.cloudburstmc.api.entity.misc;

import com.google.common.collect.ImmutableList;
import org.checkerframework.checker.index.qual.NonNegative;
import org.cloudburstmc.api.entity.Entity;

import java.util.ArrayList;
import java.util.List;

public interface ExperienceOrb extends Entity {

    /**
     * Split sizes used for dropping experience orbs.
     */
    ImmutableList<Integer> ORB_SPLIT_SIZES = ImmutableList.<Integer>builder()
            .add(2477, 1237, 617, 307, 149, 73, 37, 17, 7, 3, 1)
            .build(); //This is indexed biggest to smallest so that we can return as soon as we found the biggest value.

    /**
     * Returns the largest size of normal XP orb that will be spawned for the specified amount of XP. Used to split XP
     * up into multiple orbs when an amount of XP is dropped.
     */
    static int getMaxOrbSize(int amount) {
        for (int split : ORB_SPLIT_SIZES) {
            if (amount >= split) {
                return split;
            }
        }

        return 1;
    }

    /**
     * Splits the specified amount of XP into an array of acceptable XP orb sizes.
     */
    static List<Integer> splitIntoOrbSizes(int amount) {
        List<Integer> result = new ArrayList<>();

        while (amount > 0) {
            int size = getMaxOrbSize(amount);
            result.add(size);
            amount -= size;
        }

        return result;
    }

    @NonNegative
    int getExperience();

    void setExperience(@NonNegative int experience);

    @NonNegative
    int getPickupDelay();

    void setPickupDelay(@NonNegative int pickupDelay);
}
