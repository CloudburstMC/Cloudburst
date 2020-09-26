package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.blockentity.BlockEntity;
import org.cloudburstmc.server.blockentity.BlockEntityType;
import org.cloudburstmc.server.blockentity.Furnace;
import org.cloudburstmc.server.inventory.ContainerInventory;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ToolType;
import org.cloudburstmc.server.item.behavior.ItemToolBehavior;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.BlockEntityRegistry;

public class BlockBehaviorFurnaceBurning extends BlockBehaviorSolid {

    private BlockEntityType<? extends Furnace> furnaceEntity;

    public BlockBehaviorFurnaceBurning(BlockEntityType<? extends Furnace> entity) {
        this.furnaceEntity = entity;
    }

    @Override
    public boolean canBeActivated(Block block) {
        return true;
    }

    @Override
    public float getHardness() {
        return 3.5f;
    }

    @Override
    public float getResistance() {
        return 17.5f;
    }

    @Override
    public ToolType getToolType() {
        return ItemToolBehavior.TYPE_PICKAXE;
    }

    @Override
    public int getLightLevel(Block block) {
        return 13;
    }

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        placeBlock(block, item.getBlock().withTrait(BlockTraits.FACING_DIRECTION, player != null ? player.getHorizontalDirection() : Direction.NORTH));

        Furnace furnace = BlockEntityRegistry.get().newEntity(furnaceEntity, block);
        furnace.loadAdditionalData(item.getTag());
        if (item.hasCustomName()) {
            furnace.setCustomName(item.getCustomName());
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
        return ItemStack.get(block.getState().defaultState());
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        if (hand.isPickaxe() && hand.getTier() >= ItemToolBehavior.TIER_WOODEN) {
            return new ItemStack[]{
                    this.toItem(block)
            };
        } else {
            return new ItemStack[0];
        }
    }

    public boolean hasComparatorInputOverride() {
        return true;
    }

    @Override
    public int getComparatorInputOverride(Block block) {
        BlockEntity blockEntity = block.getLevel().getBlockEntity(block.getPosition());

        if (blockEntity instanceof Furnace) {
            return ContainerInventory.calculateRedstone(((Furnace) blockEntity).getInventory());
        }

        return super.getComparatorInputOverride(block);
    }

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }

}
