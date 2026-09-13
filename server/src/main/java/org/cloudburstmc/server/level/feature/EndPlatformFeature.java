package org.cloudburstmc.server.level.feature;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.generator.BlockStateRegion;

import java.util.function.BiConsumer;

@UtilityClass
public class EndPlatformFeature {

    public static final Vector3i SPAWN = Vector3i.from(100, 49, 0);
    public static final int RADIUS = 2;
    private static final int CLEARANCE = 3;

    public static void generate(BlockStateRegion region) {
        forEachPlatformBlock((position, state) -> {
            if (region.getBlockState(position).getType() != state.getType()) {
                region.setBlockState(position, state);
            }
        });
    }

    public static void rebuild(CloudLevel level) {
        forEachPlatformBlock((position, state) -> replace(level, position, state));
    }

    private static void forEachPlatformBlock(BiConsumer<Vector3i, BlockState> consumer) {
        for (int x = SPAWN.getX() - RADIUS; x <= SPAWN.getX() + RADIUS; x++) {
            for (int z = SPAWN.getZ() - RADIUS; z <= SPAWN.getZ() + RADIUS; z++) {
                consumer.accept(Vector3i.from(x, SPAWN.getY() - 1, z), BlockStates.OBSIDIAN);
                for (int y = SPAWN.getY(); y < SPAWN.getY() + CLEARANCE; y++) {
                    consumer.accept(Vector3i.from(x, y, z), BlockStates.AIR);
                }
            }
        }
    }

    private static void replace(CloudLevel level, Vector3i position, BlockState state) {
        BlockState currentState = level.getBlockState(position);
        if (currentState.getType() == state.getType()) {
            return;
        }

        if (currentState.getType() != BlockTypes.AIR) {
            level.breakBlock(position, ItemStack.EMPTY, null, true);
        }

        level.setBlockState(position, state);
    }
}
