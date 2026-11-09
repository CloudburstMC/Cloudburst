package org.cloudburstmc.server.block;

import lombok.ToString;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.data.BlockData;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.registry.BlockRegistry;
import org.cloudburstmc.api.util.data.BlockMirror;
import org.cloudburstmc.api.util.data.BlockRotation;
import org.cloudburstmc.math.vector.Vector3i;

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

    @Override
    public boolean isSupported(Level level, Vector3i position) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(position, "position");

        if (position.getY() < level.getMinHeight() || position.getY() >= level.getMaxHeight() || !BlockLayerRules.canCoexist(this.primary, this.secondary)) {
            return false;
        }

        BlockRegistry registry = level.getServer().getBlockRegistry();
        CloudBlock proposed = new CloudBlock(level, position, new BlockState[]{this.primary, this.secondary});
        if (!registry.requireComponent(this.primary.getType(), BlockComponents.CAN_SURVIVE).execute(proposed)) {
            return false;
        }

        if (this.secondary == BlockStates.AIR) {
            return true;
        }

        CloudBlock coexisting = new CloudBlock(level, position, new BlockState[]{this.secondary, BlockStates.AIR});
        return registry.requireComponent(this.secondary.getType(), BlockComponents.CAN_SURVIVE).execute(coexisting);
    }

    @Override
    public BlockSnapshot rotate(BlockRotation rotation) {
        BlockState primary = this.primary.rotate(rotation);
        BlockState secondary = this.secondary.rotate(rotation);
        return primary == this.primary && secondary == this.secondary ? this : new CloudBlockSnapshot(primary, secondary);
    }

    @Override
    public BlockSnapshot mirror(BlockMirror mirror) {
        BlockState primary = this.primary.mirror(mirror);
        BlockState secondary = this.secondary.mirror(mirror);
        return primary == this.primary && secondary == this.secondary ? this : new CloudBlockSnapshot(primary, secondary);
    }
}
