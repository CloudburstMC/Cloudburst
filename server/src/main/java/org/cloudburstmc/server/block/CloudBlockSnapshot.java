package org.cloudburstmc.server.block;

import lombok.ToString;
import org.cloudburstmc.api.block.BlockLayer;
import org.cloudburstmc.api.block.BlockSnapshot;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.data.BlockData;

import java.util.Objects;
import java.util.Optional;

/**
 * Captures a primary and secondary state independently of later layer changes.
 */
@ToString
public class CloudBlockSnapshot implements BlockSnapshot {

    private final BlockState primary;
    private final BlockState secondary;

    /**
     * @param primary   the primary state
     * @param secondary the coexisting state, or air when absent
     */
    public CloudBlockSnapshot(BlockState primary, BlockState secondary) {
        this.primary = Objects.requireNonNull(primary, "primary");
        this.secondary = Objects.requireNonNull(secondary, "secondary");
    }

    /**
     * Captures the values without retaining the supplied array.
     *
     * @param states the primary state followed by the secondary state
     * @throws IllegalArgumentException if the array does not contain exactly two states
     */
    public CloudBlockSnapshot(BlockState[] states) {
        Objects.requireNonNull(states, "states");
        if (states.length != 2) {
            throw new IllegalArgumentException("A block snapshot requires exactly two layers");
        }

        this.primary = Objects.requireNonNull(states[0], "primary");
        this.secondary = Objects.requireNonNull(states[1], "secondary");
    }

    @Override
    public <D extends BlockData> Optional<D> getData(Class<D> dataType) {
        return CloudBlockData.get(this, dataType);
    }

    @Override
    public BlockState getState(BlockLayer layer) {
        return switch (layer) {
            case PRIMARY -> this.primary;
            case SECONDARY -> this.secondary;
        };
    }
}
