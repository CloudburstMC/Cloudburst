package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.data.BlockColor;
import org.cloudburstmc.server.registry.CloudItemRegistry;

public abstract class BlockBehaviorFence extends BlockBehaviorTransparent {

//    @Override //TODO: bounding box
//    public AxisAlignedBB getBoundingBox(Block block) {
//        boolean north = this.canConnect(block.north().getState());
//        boolean south = this.canConnect(block.south().getState());
//        boolean west = this.canConnect(block.west().getState());
//        boolean east = this.canConnect(block.east().getState());
//        float n = north ? 0 : 0.375f;
//        float s = south ? 1 : 0.625f;
//        float w = west ? 0 : 0.375f;
//        float e = east ? 1 : 0.625f;
//        return new SimpleAxisAlignedBB(
//                block.getX() + w,
//                block.getY(),
//                block.getZ() + n,
//                block.getX() + e,
//                block.getY() + 1.5f,
//                block.getZ() + s
//        );
//    }

    public abstract boolean canConnect(BlockState state);

    @Override
    public BlockColor getColor(Block block) {
        return switch (block.getState().ensureTrait(BlockTraits.TREE_SPECIES)) {
            case OAK -> BlockColor.WOOD_BLOCK_COLOR;
            case SPRUCE -> BlockColor.SPRUCE_BLOCK_COLOR;
            case BIRCH -> BlockColor.SAND_BLOCK_COLOR;
            case JUNGLE -> BlockColor.DIRT_BLOCK_COLOR;
            case ACACIA -> BlockColor.ORANGE_BLOCK_COLOR;
            case DARK_OAK -> BlockColor.BROWN_BLOCK_COLOR;
            case MANGROVE -> BlockColor.RED_BLOCK_COLOR; //TODO: ?
            default -> BlockColor.WOOD_BLOCK_COLOR;
        };
    }

    @Override
    public ItemStack toItem(Block block) {
        return CloudItemRegistry.get().getItem(block.getState().getType().getDefaultState());
    }


}
