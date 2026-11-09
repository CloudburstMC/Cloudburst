package org.cloudburstmc.server.block.component;

import lombok.NoArgsConstructor;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.component.PlaceBlockHandler;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;

@NoArgsConstructor
public class DefaultBlockPlaceHandler implements PlaceBlockHandler {

    @Override
    public boolean execute(BlockState blockState, Player player, Vector3i blockPosition, Direction face, Vector3f clickPosition) {
        return player != null && player.getLevel().setBlockState(blockPosition, blockState, true, true);
    }
}
