package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockCategory;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.entity.EntityTypes;
import org.cloudburstmc.server.entity.misc.FallingBlock;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.Location;
import org.cloudburstmc.server.registry.EntityRegistry;

public abstract class BlockBehaviorFallable extends BlockBehaviorSolid {

    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL) {
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
