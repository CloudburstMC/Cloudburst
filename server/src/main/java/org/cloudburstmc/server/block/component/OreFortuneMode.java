package org.cloudburstmc.server.block.component;

import java.util.random.RandomGenerator;

public enum OreFortuneMode {
    ORE {
        @Override
        int apply(RandomGenerator random, int count, int level) {
            if (level <= 0) {
                return count;
            }
            int multiplier = Math.max(0, random.nextInt(level + 2) - 1) + 1;
            return count * multiplier;
        }
    },
    UNIFORM {
        @Override
        int apply(RandomGenerator random, int count, int level) {
            return level <= 0 ? count : count + random.nextInt(level + 1);
        }
    };

    abstract int apply(RandomGenerator random, int count, int level);
}
