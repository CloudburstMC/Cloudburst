package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.component.NeighborBlockHandler;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.block.util.PlacementSupport;
import org.cloudburstmc.server.level.CloudLevel;

@UtilityClass
public class TorchBlockHandlers {

    public static final NeighborBlockHandler ON_NEIGHBOUR_CHANGED = (block, neighbor) -> {
        BlockState state = block.getState();
        Direction torchDirection = state.ensureTrait(BlockTraits.TORCH_DIRECTION);
        Vector3i pos = block.getPosition();

        Direction supportFace = torchDirection == Direction.DOWN ? Direction.UP : torchDirection.getOpposite();
        Vector3i supportPos = PlacementSupport.supportPosition(pos, supportFace);

        if (!neighbor.getPosition().equals(supportPos)) {
            return;
        }

        CloudLevel level = (CloudLevel) block.getLevel();
        if (PlacementSupport.hasCenterFaceSupport(level, pos, supportFace)) {
            return;
        }

        level.breakBlock(pos, null, null, true);
    };
}
