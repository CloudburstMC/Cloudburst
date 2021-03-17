package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.Furnace;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.server.inventory.CloudContainer;
import org.cloudburstmc.server.item.CloudItemStack;
import org.cloudburstmc.server.registry.BlockEntityRegistry;

public class BlockBehaviorFurnace extends BlockBehaviorSolid {

    private BlockEntityType<? extends Furnace> furnaceEntity;

    public BlockBehaviorFurnace(BlockEntityType<? extends Furnace> entity) {
        this.furnaceEntity = entity;
    }

    @Override
    public boolean canBeActivated(Block block) {
        return true;
    }

    @Override
    public int getLightLevel(Block block) {
        return block.getState().ensureTrait(BlockTraits.IS_EXTINGUISHED) ? 0 : 13;
    }

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        placeBlock(block,
                item.getBehavior().getBlock(item)
                        .withTrait(BlockTraits.FACING_DIRECTION, player != null ? player.getHorizontalDirection() : Direction.NORTH)
                        .withTrait(BlockTraits.IS_EXTINGUISHED, true)
        );

        Furnace furnace = BlockEntityRegistry.get().newEntity(furnaceEntity, block);
        furnace.loadAdditionalData(((CloudItemStack) item).getDataTag());
        if (item.hasName()) {
            furnace.setCustomName(item.getName());
        }

        return true;
    }

    @Override
    public boolean onActivate(Block block, ItemStack item, Player player) {
        if (player != null) {
            BlockEntity blockEntity = block.getLevel().getBlockEntity(block.getPosition());
            Furnace furnace;
            if (blockEntity instanceof Furnace) {
                furnace = (Furnace) blockEntity;
            } else {
                furnace = BlockEntityRegistry.get().newEntity(furnaceEntity, block);
            }

            player.addWindow(furnace.getInventory());
        }

        return true;
    }

    @Override
    public ItemStack toItem(Block block) {
        return CloudItemRegistry.get().getItem(block.getState().defaultState());
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        if (checkTool(block.getState(), hand)) {
            return new ItemStack[]{
                    this.toItem(block)
            };
        } else {
            return new ItemStack[0];
        }
    }


    @Override
    public int getComparatorInputOverride(Block block) {
        BlockEntity blockEntity = block.getLevel().getBlockEntity(block.getPosition());

        if (blockEntity instanceof Furnace) {
            return CloudContainer.calculateRedstone(((Furnace) blockEntity).getInventory());
        }

        return super.getComparatorInputOverride(block);
    }


}
