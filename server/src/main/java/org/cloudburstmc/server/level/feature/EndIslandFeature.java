package org.cloudburstmc.server.level.feature;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.level.generator.BlockStateRegion;

import java.util.random.RandomGenerator;

@UtilityClass
public class EndIslandFeature {

    public static void place(BlockStateRegion level, Vector3i origin, BlockState state, RandomGenerator random, double radius) {
        for (int y = 0; radius > 0.5; y--) {
            int minimum = (int) Math.floor(-radius);
            int maximum = (int) Math.ceil(radius);

            for (int x = minimum; x <= maximum; x++) {
                for (int z = minimum; z <= maximum; z++) {
                    if (x * x + z * z <= (radius + 1) * (radius + 1)) {
                        level.setBlockState(origin.add(x, y, z), state);
                    }
                }
            }

            radius -= random.nextInt(2) + 0.5;
        }
    }
}
