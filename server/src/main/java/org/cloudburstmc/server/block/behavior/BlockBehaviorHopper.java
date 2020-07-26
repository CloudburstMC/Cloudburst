package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.blockentity.BlockEntity;
import org.cloudburstmc.server.blockentity.BlockEntityTypes;
import org.cloudburstmc.server.blockentity.Hopper;
import org.cloudburstmc.server.inventory.ContainerInventory;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemIds;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.level.Level;
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

        this.setMeta(facing.getIndex());

        boolean powered = this.level.isBlockPowered(this.getPosition());

        if (powered == this.isEnabled()) {
            this.setEnabled(!powered);
        }

        this.level.setBlock(this.getPosition(), this);

        BlockEntityRegistry.get().newEntity(BlockEntityTypes.HOPPER, this.getChunk(), this.getPosition());
        return true;
    }

    @Override
    public boolean onActivate(Block block, Item item, Player player) {
        BlockEntity blockEntity = this.level.getBlockEntity(this.getPosition());

        if (blockEntity instanceof Hopper) {
            return player.addWindow(((Hopper) blockEntity).getInventory()) != -1;
        }

        return false;
    }

    @Override
    public boolean canBeActivated() {
        return true;
    }

    public boolean hasComparatorInputOverride() {
        return true;
    }

    @Override
    public int getComparatorInputOverride(Block block) {
        BlockEntity blockEntity = this.level.getBlockEntity(this.getPosition());

        if (blockEntity instanceof Hopper) {
            return ContainerInventory.calculateRedstone(((Hopper) blockEntity).getInventory());
        }

        return super.getComparatorInputOverride(block);
    }

    public Direction getFacing() {
        return Direction.fromIndex(this.getMeta() & 7);
    }

    public boolean isEnabled() {
        return (this.getMeta() & 0x08) != 8;
    }

    public void setEnabled(boolean enabled) {
        if (isEnabled() != enabled) {
            this.setMeta(this.getMeta() ^ 0x08);
        }
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL) {
            boolean powered = this.level.isBlockPowered(this.getPosition());

            if (powered == this.isEnabled()) {
                this.setEnabled(!powered);
                this.level.setBlock(this.getPosition(), this, true, false);
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
    public Direction getBlockFace() {
        return Direction.fromHorizontalIndex(this.getMeta() & 0x07);
    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }
}
