package org.cloudburstmc.server.block.util;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockComponents;
import org.cloudburstmc.api.util.Direction;

@UtilityClass
public class RedstoneSignals {

    public static boolean hasNeighborSignal(Block receiver) {
        for (Direction direction : Direction.values()) {
            Block neighbor = receiver.getLevel().getLoadedBlock(direction.relative(receiver.getPosition()));
            if (neighbor == null) {
                continue;
            }

            if (neighbor.requireComponent(BlockComponents.GET_REDSTONE_SIGNAL).execute(neighbor, direction.getOpposite()) > 0) {
                return true;
            }

            if (neighbor.requireComponent(BlockComponents.CONDUCTS_REDSTONE).execute(neighbor.getState())) {
                for (Direction sourceDirection : Direction.values()) {
                    Block source = neighbor.getLevel().getLoadedBlock(sourceDirection.relative(neighbor.getPosition()));
                    if (source != null && source.requireComponent(BlockComponents.GET_DIRECT_REDSTONE_SIGNAL).execute(source, sourceDirection.getOpposite()) > 0) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
