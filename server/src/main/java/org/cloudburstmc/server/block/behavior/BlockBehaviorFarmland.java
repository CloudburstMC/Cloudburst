package org.cloudburstmc.server.block.behavior;

import lombok.val;
import org.cloudburstmc.server.block.*;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ToolType;
import org.cloudburstmc.server.item.behavior.ItemToolBehavior;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Identifier;

public class BlockBehaviorFarmland extends BlockBehaviorTransparent {

    @Override
    public float getResistance() {
        return 3;
    }

    @Override
    public float getHardness() {
        return 0.6f;
    }

    @Override
    public ToolType getToolType() {
        return ItemToolBehavior.TYPE_SHOVEL;
    }

//    @Override
//    public float getMaxY() {
//        return this.getY() + 0.9375f;
//    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_RANDOM) {
            val up = block.up();
            if (up.getState().inCategory(BlockCategory.CROPS)) {
                return 0;
            }

            if (up.getState().inCategory(BlockCategory.SOLID)) {
                block.set(BlockState.get(BlockTypes.DIRT));
                return Level.BLOCK_UPDATE_RANDOM;
            }

            boolean found = false;

            if (block.getLevel().isRaining()) {
                found = true;
            } else {
                for (int x = block.getX() - 4; x <= block.getX() + 4; x++) {
                    for (int z = block.getZ() - 4; z <= block.getZ() + 4; z++) {
                        for (int y = block.getY(); y <= block.getY() + 1; y++) {
                            if (z == block.getZ() && x == block.getX() && y == block.getY()) {
                                continue;
                            }

                            Identifier b = block.getLevel().getBlockAt(x, y, z).getType();

                            if (b == BlockTypes.FLOWING_WATER || b == BlockTypes.WATER) {
                                found = true;
                                break;
                            }
                        }
                    }
                }
            }

            val state = block.getState();
            val down = block.getLevel().getBlockAt(block.getPosition().down()).getType();
            if (found || down == BlockTypes.WATER || down == BlockTypes.FLOWING_WATER) {
                if (state.ensureTrait(BlockTraits.MOISTURIZED_AMOUNT) < 7) {
                    block.set(state.withTrait(BlockTraits.MOISTURIZED_AMOUNT, 7));
                }
                return Level.BLOCK_UPDATE_RANDOM;
            }

            if (state.ensureTrait(BlockTraits.MOISTURIZED_AMOUNT) > 0) {
                block.set(state.decrementTrait(BlockTraits.MOISTURIZED_AMOUNT));
            } else {
                block.set(BlockState.get(BlockTypes.DIRT));
            }

            return Level.BLOCK_UPDATE_RANDOM;
        }

        return 0;
    }

    @Override
    public ItemStack toItem(Block block) {
        return ItemStack.get(BlockTypes.DIRT);
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.DIRT_BLOCK_COLOR;
    }
}
