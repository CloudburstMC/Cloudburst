package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.trait.BlockTrait;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.registry.CloudBlockRegistry;

import java.util.Map;

public class StairsPlaceHandler extends DefaultBlockPlaceHandler {

    public StairsPlaceHandler(CloudBlockRegistry registry) {
        super(registry);
    }

    @Override
    public boolean execute(BlockState blockState, Player player, Vector3i pos, Direction face, Vector3f clickPos) {
        blockState = applyUpsideDown(blockState, face, clickPos);
        return super.execute(blockState, player, pos, face, clickPos);
    }

    @Override
    protected BlockState applyDirectionTraits(BlockState blockState, Player player, Vector3i pos, Direction face) {
        Map<BlockTrait<?>, Comparable<?>> traits = blockState.getTraits();
        if (traits.containsKey(BlockTraits.DIRECTION)) {
            blockState = blockState.withTrait(BlockTraits.DIRECTION, player.getHorizontalDirection());
        }
        return blockState;
    }

    private BlockState applyUpsideDown(BlockState blockState, Direction face, Vector3f clickPos) {
        boolean upsideDown = (clickPos.getY() > 0.5f && face != Direction.UP) || face == Direction.DOWN;
        return blockState.withTrait(BlockTraits.IS_UPSIDE_DOWN, upsideDown);
    }
}
