package org.cloudburstmc.server.block.behavior;

import lombok.val;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.api.blockentity.FlowerPot;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockStates;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.item.CloudItemStack;
import org.cloudburstmc.server.item.ItemType;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.BlockEntityRegistry;

public class BlockBehaviorFlowerPot extends FloodableBlockBehavior {

    protected static boolean canPlaceIntoFlowerPot(ItemType id) {
        return id == BlockTypes.SAPLING || id == BlockTypes.WEB || id == BlockTypes.TALL_GRASS || id == BlockTypes.DEADBUSH ||
                id == BlockTypes.FLOWER || id == BlockTypes.RED_MUSHROOM || id == BlockTypes.BROWN_MUSHROOM || id == BlockTypes.CACTUS || id == BlockTypes.REEDS;
        // TODO: 2016/2/4 case NETHER_WART:
    }


    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, CloudPlayer player) {
        if (face != Direction.UP) return false;

        FlowerPot flowerPot = BlockEntityRegistry.get().newEntity(BlockEntityTypes.FLOWER_POT, block);
        flowerPot.loadAdditionalData(((CloudItemStack) item).getDataTag());

        placeBlock(block, item.getBehavior().getBlock(item));
        return true;
    }

    @Override
    public boolean canBeActivated(Block block) {
        return true;
    }

    @Override
    public boolean onActivate(Block block, ItemStack item, CloudPlayer player) {
        val level = block.getLevel();
        BlockEntity blockEntity = level.getBlockEntity(block.getPosition());
        if (!(blockEntity instanceof FlowerPot)) return false;
        FlowerPot flowerPot = (FlowerPot) blockEntity;

        val itemBlock = item.getBehavior().getBlock(item);
        if (!canPlaceIntoFlowerPot(item.getType())) {
            if (!canPlaceIntoFlowerPot(itemBlock.getType())) {
                return true;
            }
        } else if (itemBlock.getType() == BlockTypes.AIR) {
            return true;
        }

        flowerPot.setPlant(itemBlock);

        block.set(block.getState().withTrait(BlockTraits.HAS_UPDATE, true), true);
        blockEntity.spawnToAll();

        if (player.isSurvival()) {
            player.getInventory().decrementHandCount();
        }
        return true;
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        boolean dropInside = false;
        BlockState blockState = BlockStates.AIR;
        BlockEntity blockEntity = block.getLevel().getBlockEntity(block.getPosition());
        if (blockEntity instanceof FlowerPot) {
            dropInside = true;
            blockState = ((FlowerPot) blockEntity).getPlant();
        }

        if (dropInside) {
            return new ItemStack[]{
                    ItemStack.get(ItemTypes.FLOWER_POT),
                    ItemStack.get(blockState)
            };
        } else {
            return new ItemStack[]{
                    ItemStack.get(ItemTypes.FLOWER_POT)
            };
        }
    }

//    @Override //TODO: bounding box
//    protected AxisAlignedBB recalculateBoundingBox() {
//        return this;
//    }
//
//    @Override
//    public float getMinX() {
//        return this.getX() + 0.3125f;
//    }
//
//    @Override
//    public float getMinZ() {
//        return this.getZ() + 0.3125f;
//    }
//
//    @Override
//    public float getMaxX() {
//        return this.getX() + 0.6875f;
//    }
//
//    @Override
//    public float getMaxY() {
//        return this.getY() + 0.375f;
//    }
//
//    @Override
//    public float getMaxZ() {
//        return this.getZ() + 0.6875f;
//    }

    @Override
    public ItemStack toItem(Block block) {
        return ItemStack.get(ItemTypes.FLOWER_POT);
    }
}
