package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.block.component.NeighborBlockHandler;
import org.cloudburstmc.api.block.component.TickBlockHandler;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.NetherPortals;
import org.cloudburstmc.server.level.PortalFrame;

import java.util.Optional;

@UtilityClass
public class PortalBlockHandlers {

    /**
     * When a neighboring block changes, verify that the obsidian frame
     * enclosing this portal block is still intact. If the frame is missing or
     * incomplete the portal block replaces itself with air.
     *
     * <p>Neighbor changes on the perpendicular horizontal axis are ignored:
     * a block placed <em>in front of</em> the portal face cannot break it.</p>
     */
    public static final NeighborBlockHandler ON_NEIGHBOUR_CHANGED = (block, neighbor) -> {
        if (neighbor.getState().getType() == BlockTypes.PORTAL) {
            return;
        }

        Direction.Axis portalAxis = block.getState().ensureTrait(BlockTraits.PORTAL_AXIS);
        Vector3i delta = neighbor.getPosition().sub(block.getPosition());
        Direction.Axis changedAxis;

        if (delta.getX() != 0) {
            changedAxis = Direction.Axis.X;
        } else if (delta.getZ() != 0) {
            changedAxis = Direction.Axis.Z;
        } else {
            changedAxis = Direction.Axis.Y;
        }

        boolean perpendicularHorizontal = changedAxis != Direction.Axis.Y && changedAxis != portalAxis;
        if (perpendicularHorizontal) {
            return;
        }

        CloudLevel level = (CloudLevel) block.getLevel();
        Optional<PortalFrame> frame = NetherPortals.detect(level, block.getPosition());
        if (frame.isEmpty() || !frame.get().isFull(level)) {
            block.set(BlockTypes.AIR.getDefaultState(), true, true);
        }
    };

    /**
     * Delegate random-tick processing to {@link NetherPortals#onPortalRandomTick},
     * which may spawn a Zombified Piglin near the portal when conditions are met.
     */
    public static final TickBlockHandler ON_RANDOM_TICK = (block, random) -> {
        CloudLevel level = (CloudLevel) block.getLevel();
        NetherPortals.onPortalRandomTick(level, block.getPosition(), random);
    };
}
