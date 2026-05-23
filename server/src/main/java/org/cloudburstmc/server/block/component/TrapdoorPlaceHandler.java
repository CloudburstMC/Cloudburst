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
    public boolean execute(BlockState blockState, Player player, Vector3i pos, Direction face, Vector3f clickPos) {
        return super.execute(applyUpsideDown(blockState, face, clickPos), player, pos, face, clickPos);
    }

    private static BlockState applyUpsideDown(BlockState blockState, Direction face, Vector3f clickPos) {
        boolean upsideDown;
        if (face == Direction.UP) {
            upsideDown = false;
        } else if (face == Direction.DOWN) {
            upsideDown = true;
        } else {
            upsideDown = clickPos.getY() > 0.5f;
        }
        return blockState.withTrait(BlockTraits.IS_UPSIDE_DOWN, upsideDown);
    }
}
