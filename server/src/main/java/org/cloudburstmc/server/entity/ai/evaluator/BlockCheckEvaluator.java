package org.cloudburstmc.server.entity.ai.evaluator;

import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.entity.EntityIntelligent;
import org.cloudburstmc.api.entity.ai.behavior.BehaviorEvaluator;
import org.cloudburstmc.math.vector.Vector3i;

/**
 * Checks if the block at the entity's location plus an offset
 * matches the expected block type.
 *
 * @author daoge_cmd
 */
public class BlockCheckEvaluator implements BehaviorEvaluator {

    protected final BlockType blockType;
    protected final Vector3i offset;

    public BlockCheckEvaluator(BlockType blockType, Vector3i offset) {
        this.blockType = blockType;
        this.offset = offset;
    }

    @Override
    public boolean evaluate(EntityIntelligent entity) {
        var loc = entity.getLocation();
        int x = (int) Math.floor(loc.getX()) + offset.getX();
        int y = (int) Math.floor(loc.getY()) + offset.getY();
        int z = (int) Math.floor(loc.getZ()) + offset.getZ();
        var blockState = entity.getLevel().getBlockState(x, y, z);
        return blockState != null && blockState.getType() == blockType;
    }
}
