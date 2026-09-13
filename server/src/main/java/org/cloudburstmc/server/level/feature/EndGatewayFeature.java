package org.cloudburstmc.server.level.feature;

import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.api.blockentity.EndGateway;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.generator.BlockStateRegion;
import org.cloudburstmc.server.registry.CloudBlockEntityRegistry;

@UtilityClass
public class EndGatewayFeature {

    public static void place(CloudLevel level, Vector3i origin) {
        place(level, origin, null);
    }

    public static void place(CloudLevel level, Vector3i origin, @Nullable Vector3i exitPosition) {
        placeBlocks(level, origin);
        bindBlockEntity(level, origin, exitPosition, false);
    }

    public static void placeBlocks(BlockStateRegion level, Vector3i origin) {
        for (int x = -1; x <= 1; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -1; z <= 1; z++) {
                    boolean sameX = x == 0;
                    boolean sameY = y == 0;
                    boolean sameZ = z == 0;
                    boolean end = Math.abs(y) == 2;

                    if (sameX && sameY && sameZ) {
                        level.setBlockState(origin.add(x, y, z), BlockStates.END_GATEWAY);
                    } else if ((end && sameX && sameZ) || (!end && (sameX || sameZ) && !sameY)) {
                        level.setBlockState(origin.add(x, y, z), BlockStates.BEDROCK);
                    } else {
                        level.setBlockState(origin.add(x, y, z), BlockStates.AIR);
                    }
                }
            }
        }
    }

    public static void bindBlockEntity(CloudLevel level, Vector3i origin, @Nullable Vector3i exitPosition, boolean exactTeleport) {
        BlockEntity existing = level.getBlockEntity(origin);
        EndGateway gateway;
        if (existing instanceof EndGateway endGateway) {
            gateway = endGateway;
        } else {
            if (existing != null) {
                existing.close();
            }

            gateway = CloudBlockEntityRegistry.get().newEntity(BlockEntityTypes.END_GATEWAY, level.getBlock(origin));
        }

        gateway.setExitLocation(exitPosition == null ? null : Location.from(exitPosition, level));
        gateway.setExactTeleport(exactTeleport);
    }
}
