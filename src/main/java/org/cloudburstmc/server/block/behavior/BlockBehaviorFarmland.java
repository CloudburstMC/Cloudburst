package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockCategory;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.data.BlockColor;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.registry.CloudItemRegistry;

public class BlockBehaviorFarmland extends BlockBehaviorTransparent {


//    @Override
//    public float getMaxY() {
//        return this.getY() + 0.9375f;
//    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == CloudLevel.BLOCK_UPDATE_RANDOM) {
            var up = block.up();
            if (up.getState().inCategory(BlockCategory.CROPS)) {
                return 0;
            }

            if (up.getState().inCategory(BlockCategory.SOLID)) {
                block.set(CloudBlockRegistry.get().getBlock(BlockTypes.DIRT));
                return CloudLevel.BLOCK_UPDATE_RANDOM;
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

                            var b = block.getLevel().getBlockState(x, y, z).getType();

                            if (b == BlockTypes.FLOWING_WATER || b == BlockTypes.WATER) {
                                found = true;
                                break;
                            }
                        }
                    }
                }
            }

            var state = block.getState();
            var down = block.getLevel().getBlockState(block.getPosition().down()).getType();
            if (found || down == BlockTypes.WATER || down == BlockTypes.FLOWING_WATER) {
                if (state.ensureTrait(BlockTraits.MOISTURIZED_AMOUNT) < 7) {
                    block.set(state.withTrait(BlockTraits.MOISTURIZED_AMOUNT, 7));
                }
                return CloudLevel.BLOCK_UPDATE_RANDOM;
            }

            if (state.ensureTrait(BlockTraits.MOISTURIZED_AMOUNT) > 0) {
                block.set(state.decrementTrait(BlockTraits.MOISTURIZED_AMOUNT));
            } else {
                block.set(CloudBlockRegistry.get().getBlock(BlockTypes.DIRT));
            }

            return CloudLevel.BLOCK_UPDATE_RANDOM;
        }

        return 0;
    }

    @Override
    public ItemStack toItem(Block block) {
        return CloudItemRegistry.get().getItem(BlockTypes.DIRT);
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.DIRT_BLOCK_COLOR;
    }
}
