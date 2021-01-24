package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import lombok.val;
import lombok.var;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.blockentity.BlockEntity;
import org.cloudburstmc.server.blockentity.BlockEntityTypes;
import org.cloudburstmc.server.blockentity.Hopper;
import org.cloudburstmc.server.inventory.ContainerInventory;
import org.cloudburstmc.server.item.behavior.Item;
import org.cloudburstmc.server.item.behavior.ItemIds;
import org.cloudburstmc.server.item.behavior.ItemTool;
import org.cloudburstmc.server.world.World;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.BlockEntityRegistry;

public class BlockBehaviorHopper extends BlockBehaviorTransparent {

    @Override
    public float getHardness() {
        return 3;
    }

    @Override
    public float getResistance() {
        return 24;
    }

    @Override
    public boolean place(Item item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        Direction facing = face.getOpposite();

        if (facing == Direction.UP) {
            facing = Direction.DOWN;
        }

        var hopper = item.getBlock().withTrait(BlockTraits.FACING_DIRECTION, facing)
                .withTrait(BlockTraits.IS_TOGGLED, block.getWorld().isBlockPowered(block.getPosition()));

        placeBlock(block, hopper);

        BlockEntityRegistry.get().newEntity(BlockEntityTypes.HOPPER, block);
        return true;
    }

    @Override
    public boolean onActivate(Block block, Item item, Player player) {
        BlockEntity blockEntity = block.getWorld().getBlockEntity(block.getPosition());

        if (blockEntity instanceof Hopper) {
            return player.addWindow(((Hopper) blockEntity).getInventory()) != -1;
        }

        return false;
    }

    @Override
    public boolean canBeActivated(Block block) {
        return true;
    }

    public boolean hasComparatorInputOverride() {
        return true;
    }

    @Override
    public int getComparatorInputOverride(Block block) {
        BlockEntity blockEntity = block.getWorld().getBlockEntity(block.getPosition());

        if (blockEntity instanceof Hopper) {
            return ContainerInventory.calculateRedstone(((Hopper) blockEntity).getInventory());
        }

        return super.getComparatorInputOverride(block);
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == World.BLOCK_UPDATE_NORMAL) {
            val state = block.getState();
            boolean powered = block.getWorld().isBlockPowered(block.getPosition());

            if (powered != state.ensureTrait(BlockTraits.IS_TOGGLED)) {
                block.set(state.withTrait(BlockTraits.IS_TOGGLED, powered));
            }

            return type;
        }

        return 0;
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_PICKAXE;
    }

    @Override
    public Item[] getDrops(Block block, Item hand) {
        if (hand.getTier() >= ItemTool.TIER_WOODEN) {
            return new Item[]{toItem(block)};
        }

        return new Item[0];
    }

    @Override
    public Item toItem(Block block) {
        return Item.get(ItemIds.HOPPER);
    }

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }
}
