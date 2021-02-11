package org.cloudburstmc.server.block.behavior;

import lombok.val;
import lombok.var;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.api.blockentity.Hopper;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.inventory.CloudContainer;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.item.TierTypes;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.BlockEntityRegistry;

public class BlockBehaviorHopper extends BlockBehaviorTransparent {


    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, CloudPlayer player) {
        Direction facing = face.getOpposite();

        if (facing == Direction.UP) {
            facing = Direction.DOWN;
        }

        var hopper = item.getBehavior().getBlock(item).withTrait(BlockTraits.FACING_DIRECTION, facing)
                .withTrait(BlockTraits.IS_TOGGLED, block.getLevel().isBlockPowered(block.getPosition()));

        placeBlock(block, hopper);

        BlockEntityRegistry.get().newEntity(BlockEntityTypes.HOPPER, block);
        return true;
    }

    @Override
    public boolean onActivate(Block block, ItemStack item, CloudPlayer player) {
        BlockEntity blockEntity = block.getLevel().getBlockEntity(block.getPosition());

        if (blockEntity instanceof Hopper) {
            return player.addWindow(((Hopper) blockEntity).getInventory()) != -1;
        }

        return false;
    }

    @Override
    public boolean canBeActivated(Block block) {
        return true;
    }


    @Override
    public int getComparatorInputOverride(Block block) {
        BlockEntity blockEntity = block.getLevel().getBlockEntity(block.getPosition());

        if (blockEntity instanceof Hopper) {
            return CloudContainer.calculateRedstone(((Hopper) blockEntity).getInventory());
        }

        return super.getComparatorInputOverride(block);
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == CloudLevel.BLOCK_UPDATE_NORMAL) {
            val state = block.getState();
            boolean powered = block.getLevel().isBlockPowered(block.getPosition());

            if (powered != state.ensureTrait(BlockTraits.IS_TOGGLED)) {
                block.set(state.withTrait(BlockTraits.IS_TOGGLED, powered));
            }

            return type;
        }

        return 0;
    }


    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        if (hand.getBehavior().getTier(hand).compareTo(TierTypes.WOOD) >= 0) {
            return new ItemStack[]{toItem(block)};
        }

        return new ItemStack[0];
    }

    @Override
    public ItemStack toItem(Block block) {
        return ItemStack.get(ItemTypes.HOPPER);
    }


}
