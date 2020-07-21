package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockFactory;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.utils.Identifier;

/**
 * author: Angelic47
 * Nukkit Project
 */
public class BlockBehaviorLavaStill extends BlockBehaviorLava {

    protected BlockBehaviorLavaStill(Identifier flowingId, Identifier stationaryId) {
        super(flowingId, stationaryId);
    }

    @Override
    public BlockState getBlock(int meta) {
        return BlockState.get(BlockTypes.LAVA);
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type != Level.BLOCK_UPDATE_SCHEDULED) {
            return super.onUpdate(block, type);
        }
        return 0;
    }

    public static BlockFactory factory(Identifier flowingId) {
        return id -> new BlockBehaviorLavaStill(flowingId, id);
    }
}
