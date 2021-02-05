package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockCategory;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.misc.FallingBlock;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.registry.EntityRegistry;

public abstract class BlockBehaviorFallable extends BlockBehaviorSolid {

    public int onUpdate(Block block, int type) {
        if (type == CloudLevel.BLOCK_UPDATE_NORMAL) {
            BlockState down = block.down().getState();
            if (down.getType() == BlockTypes.AIR || down.inCategory(BlockCategory.LIQUID)) {
                removeBlock(block, true);

                FallingBlock fallingBlock = EntityRegistry.get().newEntity(EntityTypes.FALLING_BLOCK,
                        Location.from(block.getPosition().toFloat().add(0.5, 0, 0.5), block.getLevel()));
                fallingBlock.setBlock(block.getState());
                fallingBlock.spawnToAll();
            }
        }
        return type;
    }
}
