package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemIds;
import org.cloudburstmc.server.item.food.Food;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorCake extends BlockBehaviorTransparent {

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    public float getHardness() {
        return 0.5f;
    }

    @Override
    public float getResistance() {
        return 2.5f;
    }

//    @Override
//    public float getMinX() {
//        return this.getX() + (1 + getMeta() * 2) / 16f;
//    }
//
//    @Override
//    public float getMinY() {
//        return this.getY();
//    }
//
//    @Override
//    public float getMinZ() {
//        return this.getZ() + 0.0625f;
//    }
//
//    @Override
//    public float getMaxX() {
//        return this.getX() - 0.0625f + 1;
//    }
//
//    @Override
//    public float getMaxY() {
//        return this.getY() + 0.5f;
//    }
//
//    @Override
//    public float getMaxZ() {
//        return this.getZ() - 0.0625f + 1;
//    }

    @Override
    public boolean place(Item item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        if (block.down().getState().getType() != BlockTypes.AIR) {
            placeBlock(block, item);

            return true;
        }
        return false;
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL) {
            if (block.down().getState().getType() == BlockTypes.AIR) {
                removeBlock(block, true);

                return Level.BLOCK_UPDATE_NORMAL;
            }
        }

        return 0;
    }

    @Override
    public Item[] getDrops(Block block, Item hand) {
        return new Item[0];
    }

    @Override
    public Item toItem(Block block) {
        return Item.get(ItemIds.CAKE);
    }

    @Override
    public boolean onActivate(Block block, Item item, Player player) {
        if (player != null && player.getFoodData().getLevel() < player.getFoodData().getMaxLevel()) {
            int counter = block.getState().ensureTrait(BlockTraits.BITE_COUNTER);

            if (counter < 6) {
                counter++;
            }

            if (counter >= 6) {
                removeBlock(block, true);
            } else {
                Food.getByRelative(block.getState(), counter).eatenBy(player);
                block.set(block.getState().withTrait(BlockTraits.BITE_COUNTER, counter), true);
            }

            return true;
        }
        return false;
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.AIR_BLOCK_COLOR;
    }

    public int getComparatorInputOverride(Block block) {
        return (7 - block.getState().ensureTrait(BlockTraits.BITE_COUNTER)) << 1;
    }

    public boolean hasComparatorInputOverride() {
        return true;
    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }
}
