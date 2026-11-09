package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.data.Slab;
import org.cloudburstmc.api.block.component.PlaceBlockHandler;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.SlabSlot;
import org.cloudburstmc.api.util.data.SlabType;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;

public class SlabPlaceHandler implements PlaceBlockHandler {

    @Override
    public boolean execute(BlockState blockState, Player player, Vector3i blockPosition, Direction face, Vector3f clickPosition) {
        Level level = player.getLevel();
        if (level.getBlockState(blockPosition).getType() == blockState.getType()) {
            Slab slab = blockState.getData(Slab.class).orElseThrow();
            return level.setBlockState(blockPosition, slab.withType(SlabType.DOUBLE), true, true);
        }

        SlabSlot slot = face == Direction.DOWN ? SlabSlot.TOP
                : face == Direction.UP ? SlabSlot.BOTTOM
                  : clickPosition.getY() > 0.5f ? SlabSlot.TOP : SlabSlot.BOTTOM;
        return level.setBlockState(blockPosition, blockState.withTrait(BlockTraits.SLAB_SLOT, slot), true, true);
    }
}
