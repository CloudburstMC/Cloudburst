package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.blockentity.Barrel;
import org.cloudburstmc.server.blockentity.BlockEntity;
import org.cloudburstmc.server.blockentity.BlockEntityTypes;
import org.cloudburstmc.server.inventory.ContainerInventory;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.math.BlockFace;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.BlockEntityRegistry;
import org.cloudburstmc.server.utils.BlockColor;

import static org.cloudburstmc.server.block.BlockTypes.BARREL;

public class BlockBehaviorBarrel extends BlockBehaviorSolid {

    @Override
    public boolean place(Item item, Block block, Block target, BlockFace face, Vector3f clickPos, Player player) {
        BlockState newState = BlockState.get(BARREL);
        if (Math.abs(player.getX() - block.getX()) < 2 && Math.abs(player.getZ() - block.getZ()) < 2) {
            float y = player.getY() + player.getEyeHeight();

            if (y - block.getY() > 2) {
                this.setMeta(BlockFace.UP.getIndex());
            } else if (this.getY() - y > 0) {
                this.setMeta(BlockFace.DOWN.getIndex());
            } else {
                this.setMeta(player.getHorizontalFacing().getOpposite().getIndex());
            }
        } else {
            this.setMeta(player.getHorizontalFacing().getOpposite().getIndex());
        }

        block.set(newState, true, false);

        Barrel barrel = BlockEntityRegistry.get().newEntity(BlockEntityTypes.BARREL, block.getChunk(), block.getPosition());
        barrel.loadAdditionalData(item.getTag());

        return true;
    }

    @Override
    public boolean onActivate(Block block, Item item, Player player) {
        if (player == null) {
            return false;
        }

        BlockEntity blockEntity = block.getLevel().getBlockEntity(block.getPosition());
        Barrel barrel;
        if (blockEntity instanceof Barrel) {
            barrel = (Barrel) blockEntity;
        } else {
            barrel = BlockEntityRegistry.get().newEntity(BlockEntityTypes.BARREL, block.getChunk(), block.getPosition());
        }

        player.addWindow(barrel.getInventory());

        return true;
    }

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    public float getHardness() {
        return 2.5f;
    }

    @Override
    public float getResistance() {
        return 12.5f;
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_AXE;
    }

    @Override
    public BlockColor getColor(BlockState state) {
        return BlockColor.WOOD_BLOCK_COLOR;
    }

    @Override
    public Item toItem(BlockState state) {
        return Item.get(this.id, 0);
    }

    @Override
    public BlockFace getBlockFace() {
        int index = getMeta() & 0x7;
        return BlockFace.fromIndex(index);
    }

    public void setBlockFace(BlockFace face) {
        setMeta((getMeta() & 0x8) | (face.getIndex() & 0x7));
        getLevel().setBlockDataAt(this.getX(), this.getY(), this.getZ(), this.getLayer(), getMeta());
    }

    public boolean isOpen() {
        return (getMeta() & 0x8) == 0x8;
    }

    public void setOpen(boolean open) {
        setMeta((getMeta() & 0x7) | (open ? 0x8 : 0x0));
        getLevel().setBlockDataAt(this.getX(), this.getY(), this.getZ(), this.getLayer(), getMeta());
    }

    @Override
    public boolean hasComparatorInputOverride() {
        return true;
    }

    @Override
    public int getComparatorInputOverride() {
        BlockEntity blockEntity = this.level.getBlockEntity(this.getPosition());

        if (blockEntity instanceof Barrel) {
            return ContainerInventory.calculateRedstone(((Barrel) blockEntity).getInventory());
        }

        return super.getComparatorInputOverride();
    }
}