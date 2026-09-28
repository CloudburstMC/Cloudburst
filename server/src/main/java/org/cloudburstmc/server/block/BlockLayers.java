package org.cloudburstmc.server.block;

import org.cloudburstmc.api.block.BlockState;

import java.util.Objects;

/**
 * The two block states stored at one level position.
 */
public record BlockLayers(BlockState primary, BlockState secondary) {

    public BlockLayers {
        Objects.requireNonNull(primary, "primary");
        Objects.requireNonNull(secondary, "secondary");
    }
}
