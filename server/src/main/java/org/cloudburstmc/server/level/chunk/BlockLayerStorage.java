package org.cloudburstmc.server.level.chunk;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockLayer;

@UtilityClass
public class BlockLayerStorage {

    public static int index(BlockLayer layer) {
        return switch (layer) {
            case PRIMARY -> 0;
            case SECONDARY -> 1;
        };
    }
}
