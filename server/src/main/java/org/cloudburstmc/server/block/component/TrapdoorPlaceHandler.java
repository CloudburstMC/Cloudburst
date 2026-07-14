package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.registry.CloudBlockRegistry;

public class TrapdoorPlaceHandler extends DefaultBlockPlaceHandler {

    public TrapdoorPlaceHandler(CloudBlockRegistry registry) {
        super(registry);
    }

    @Override
    public boolean execute(BlockState blockState, Player player, Vector3i blockPosition, Direction face, Vector3f clickPosition) {
        return super.execute(applyUpsideDown(blockState, face, clickPosition), player, blockPosition, face, clickPosition);
    }

    private BlockState applyUpsideDown(BlockState blockState, Direction face, Vector3f clickPosition) {
        boolean upsideDown;
        if (face == Direction.UP) {
            upsideDown = false;
        } else if (face == Direction.DOWN) {
            upsideDown = true;
        } else {
            upsideDown = clickPosition.getY() > 0.5f;
        }
        return blockState.withTrait(BlockTraits.IS_UPSIDE_DOWN, upsideDown);
    }
}
