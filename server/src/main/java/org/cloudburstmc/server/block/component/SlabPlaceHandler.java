package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.component.PlaceBlockHandler;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.SlabSlot;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;

public class SlabPlaceHandler implements PlaceBlockHandler {

    private final BlockType doubleSlabType;

    public SlabPlaceHandler(BlockType doubleSlabType) {
        this.doubleSlabType = doubleSlabType;
    }

    @Override
    public boolean execute(BlockState blockState, Player player, Vector3i pos, Direction face, Vector3f clickPos) {
        Level level = player.getLevel();
        if (level.getBlockState(pos).getType() == blockState.getType()) {
            return level.setBlockState(pos, doubleSlabType.getDefaultState(), true, true);
        }

        SlabSlot slot = face == Direction.DOWN ? SlabSlot.TOP
                : face == Direction.UP ? SlabSlot.BOTTOM
                  : clickPos.getY() > 0.5f ? SlabSlot.TOP : SlabSlot.BOTTOM;
        return level.setBlockState(pos, blockState.withTrait(BlockTraits.SLAB_SLOT, slot), true, true);
    }
}
