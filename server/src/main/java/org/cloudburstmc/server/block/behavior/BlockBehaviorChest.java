package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.blockentity.BlockEntity;
import org.cloudburstmc.server.blockentity.BlockEntityTypes;
import org.cloudburstmc.server.blockentity.Chest;
import org.cloudburstmc.server.inventory.ContainerInventory;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.math.BlockFace;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.BlockEntityRegistry;
import org.cloudburstmc.server.utils.BlockColor;

@Log4j2
public class BlockBehaviorChest extends BlockBehaviorTransparent {

    @Override
    public boolean canWaterlogSource() {
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
    public float getMinX() {
        return this.getX() + 0.0625f;
    }

    @Override
    public float getMinY() {
        return this.getY();
    }

    @Override
    public float getMinZ() {
        return this.getZ() + 0.0625f;
    }

    @Override
    public float getMaxX() {
        return this.getX() + 0.9375f;
    }

    @Override
    public float getMaxY() {
        return this.getY() + 0.9475f;
    }

    @Override
    public float getMaxZ() {
        return this.getZ() + 0.9375f;
    }


    @Override
    public boolean place(Item item, Block block, Block target, BlockFace face, Vector3f clickPos, Player player) {
        Chest chest = null;
        int[] faces = {2, 5, 3, 4};
        this.setMeta(faces[player != null ? player.getDirection().getHorizontalIndex() : 0]);

        for (int side = 2; side <= 5; ++side) {
            if ((this.getMeta() == 4 || this.getMeta() == 5) && (side == 4 || side == 5)) {
                continue;
            } else if ((this.getMeta() == 3 || this.getMeta() == 2) && (side == 2 || side == 3)) {
                continue;
            }
            BlockState c = this.getSide(BlockFace.fromIndex(side));
            if (c instanceof BlockBehaviorChest && c.getMeta() == this.getMeta()) {
                BlockEntity blockEntity = this.getLevel().getBlockEntity(c.getPosition());
                if (blockEntity instanceof Chest && !((Chest) blockEntity).isPaired()) {
                    chest = (Chest) blockEntity;
                    break;
                }
            }
        }
        if ((block.getId() == BlockTypes.WATER || block.getId() == BlockTypes.FLOWING_WATER) && block.getMeta() == 0) {
            this.getLevel().setBlock(block.getPosition(), 1, block, true, false);
        }

        this.getLevel().setBlock(block.getPosition(), this, true, true);

        Chest chest1 = BlockEntityRegistry.get().newEntity(BlockEntityTypes.CHEST, this.getChunk(), this.getPosition());
        chest1.loadAdditionalData(item.getTag());
        if (item.hasCustomName()) {
            chest1.setCustomName(item.getCustomName());
        }

        if (chest != null) {
            chest.pairWith(chest1);
        }

        return true;
    }

    @Override
    public boolean onBreak(Block block, Item item) {
        BlockEntity t = this.getLevel().getBlockEntity(this.getPosition());
        if (t instanceof Chest) {
            ((Chest) t).unpair();
        }
        return super.onBreak(block, item);
    }

    @Override
    public boolean onActivate(Block block, Item item, Player player) {
        if (player != null) {
            BlockState top = up();
            if (!top.isTransparent()) {
                return true;
            }

            BlockEntity t = this.getLevel().getBlockEntity(this.getPosition());
            Chest chest;
            if (t instanceof Chest) {
                chest = (Chest) t;
            } else {
                chest = BlockEntityRegistry.get().newEntity(BlockEntityTypes.CHEST, this.getChunk(), this.getPosition());
            }

            player.addWindow(chest.getInventory());
        }

        return true;
    }

    @Override
    public BlockColor getColor(BlockState state) {
        return BlockColor.WOOD_BLOCK_COLOR;
    }

    public boolean hasComparatorInputOverride() {
        return true;
    }

    public int getComparatorInputOverride() {
        BlockEntity blockEntity = this.level.getBlockEntity(this.getPosition());

        if (blockEntity instanceof Chest) {
            return ContainerInventory.calculateRedstone(((Chest) blockEntity).getInventory());
        }

        return super.getComparatorInputOverride();
    }

    @Override
    public Item toItem(BlockState state) {
        return Item.get(id, 0);
    }

    @Override
    public BlockFace getBlockFace() {
        return BlockFace.fromHorizontalIndex(this.getMeta() & 0x7);
    }
}
