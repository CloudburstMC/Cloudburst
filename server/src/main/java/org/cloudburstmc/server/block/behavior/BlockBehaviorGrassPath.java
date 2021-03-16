package org.cloudburstmc.server.block.behavior;

import lombok.val;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.data.BlockColor;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.player.CloudPlayer;

public class BlockBehaviorGrassPath extends BlockBehaviorGrass {


//    @Override //TODO: bounding box
//    public float getMaxY() {
//        return this.getY() + 0.9375f;
//    }


    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.DIRT_BLOCK_COLOR;
    }

    @Override
    public int onUpdate(Block block, int type) {
        return 0;
    }

    @Override
    public boolean onActivate(Block block, ItemStack item, CloudPlayer player) {
        val behavior = item.getBehavior();
        if (behavior.isHoe()) {
            behavior.useOn(item, block);
            block.set(BlockState.get(BlockTypes.FARMLAND), true);
            return true;
        }

        return false;
    }
}
