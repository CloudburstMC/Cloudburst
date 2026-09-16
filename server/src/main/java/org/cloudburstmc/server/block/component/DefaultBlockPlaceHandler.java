package org.cloudburstmc.server.block.component;

import lombok.NoArgsConstructor;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockComponents;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.component.PlaceBlockHandler;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;

@NoArgsConstructor
public class DefaultBlockPlaceHandler implements PlaceBlockHandler {

    @Override
    public boolean execute(BlockState blockState, Player player, Vector3i blockPosition, Direction face, Vector3f clickPosition) {
        if (player == null) {
            return false;
        }

        Level level = player.getLevel();
        if (!level.setBlockState(blockPosition, blockState, true, true)) {
            return false;
        }

        Block placedBlock = level.getBlock(blockPosition);
        if (placedBlock.requireComponent(BlockComponents.IS_FREE_TO_FALL).execute(placedBlock)) {
            FallingBlockHandlers.scheduleFall(placedBlock);
        }

        return true;
    }
}
