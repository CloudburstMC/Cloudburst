package org.cloudburstmc.server.level.feature;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.level.CloudLevel;

@UtilityClass
public class EndPodiumFeature {

    public static void place(CloudLevel level, Vector3i origin, boolean active) {
        level.batchBlockUpdates(() -> placeBlocks(level, origin, active));
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
}
