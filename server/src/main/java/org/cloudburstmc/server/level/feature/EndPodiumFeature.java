package org.cloudburstmc.server.level.feature;

import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.generator.BlockStateRegion;

@UtilityClass
public class EndPodiumFeature {

    public static void place(CloudLevel level, Vector3i origin, boolean active) {
        level.batchBlockUpdates(() -> placeBlocks(level, origin, active));
    }

    public static @Nullable Integer findExistingBase(BlockStateRegion level, int minY, int maxY) {
        for (int y = minY + 1; y < maxY - 3; y++) {
            if (hasBaseAt(level, y)) {
                return y;
            }
        }

        return null;
    }

    public static @Nullable Integer findActivePortal(BlockStateRegion level, int minY, int maxY) {
        for (int y = minY + 1; y < maxY; y++) {
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    if (level.getBlockState(x, y, z) == BlockStates.END_PORTAL) {
                        return y;
                    }
                }
            }
        }

        return null;
    }

    private static void placeBlocks(CloudLevel level, Vector3i origin, boolean active) {
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                double distance = Math.sqrt(x * x + z * z);
                if (distance >= 3.5) {
                    continue;
                }

                boolean insideRim = distance < 2.5;
                level.setBlockState(origin.add(x, -1, z), insideRim ? BlockStates.BEDROCK : BlockStates.END_STONE);
                level.setBlockState(origin.add(x, 0, z), insideRim ? (active ? BlockStates.END_PORTAL : BlockStates.AIR) : BlockStates.BEDROCK);
                for (int y = 1; y <= 32; y++) {
                    level.setBlockState(origin.add(x, y, z), BlockStates.AIR);
                }
            }
        }

        for (int y = 0; y < 4; y++) {
            level.setBlockState(origin.add(0, y, 0), BlockStates.BEDROCK);
        }

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockState torch = BlockStates.TORCH.withTrait(BlockTraits.TORCH_DIRECTION, direction.getOpposite());
            level.setBlockState(origin.add(direction.getStepX(), 2, direction.getStepZ()), torch);
        }
    }

    private static boolean hasBaseAt(BlockStateRegion level, int y) {
        for (int offset = 0; offset < 4; offset++) {
            if (level.getBlockState(0, y + offset, 0) != BlockStates.BEDROCK) {
                return false;
            }
        }

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(direction.getStepX() * 3, y, direction.getStepZ() * 3) != BlockStates.BEDROCK) {
                return false;
            }
        }

        return true;
    }
}
