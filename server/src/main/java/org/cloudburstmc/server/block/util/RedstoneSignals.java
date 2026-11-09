package org.cloudburstmc.server.block.util;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockComponents;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.util.Direction;

@UtilityClass
public class RedstoneSignals {

    public static boolean hasNeighborSignal(Block receiver) {
        return getNeighborSignal(receiver, false) > 0;
    }

    public static int getNeighborSignal(Block receiver) {
        return getNeighborSignal(receiver, false);
    }

    public static int getExternalSignal(Block receiver) {
        return getNeighborSignal(receiver, true);
    }

    private static int getNeighborSignal(Block receiver, boolean excludeWire) {
        int signal = 0;
        for (Direction direction : Direction.values()) {
            Block neighbor = receiver.getLevel().getLoadedBlock(direction.relative(receiver.getPosition()));
            if (neighbor == null) {
                continue;
            }

            if (!excludeWire || neighbor.getState().getType() != BlockTypes.REDSTONE_WIRE) {
                signal = Math.max(signal, neighbor.requireComponent(BlockComponents.GET_REDSTONE_SIGNAL).execute(neighbor, direction.getOpposite()));
            }

            if (neighbor.requireComponent(BlockComponents.CONDUCTS_REDSTONE).execute(neighbor.getState())) {
                for (Direction sourceDirection : Direction.values()) {
                    Block source = neighbor.getLevel().getLoadedBlock(sourceDirection.relative(neighbor.getPosition()));
                    if (source != null && (!excludeWire || source.getState().getType() != BlockTypes.REDSTONE_WIRE)) {
                        signal = Math.max(signal, source.requireComponent(BlockComponents.GET_DIRECT_REDSTONE_SIGNAL).execute(source, sourceDirection.getOpposite()));
                    }
                }
            }

            if (signal >= 15) {
                return 15;
            }
        }

        return signal;
    }
}
