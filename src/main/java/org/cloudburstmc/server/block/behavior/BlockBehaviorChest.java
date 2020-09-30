package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.server.block.*;
import org.cloudburstmc.server.blockentity.BlockEntity;
import org.cloudburstmc.server.blockentity.BlockEntityTypes;
import org.cloudburstmc.server.blockentity.Chest;
import org.cloudburstmc.server.inventory.ContainerInventory;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ToolType;
import org.cloudburstmc.server.item.behavior.ItemToolBehavior;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.math.Direction.Axis;
import org.cloudburstmc.server.math.Direction.Plane;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.BlockEntityRegistry;
import org.cloudburstmc.server.registry.BlockRegistry;
import org.cloudburstmc.server.registry.CloudItemRegistry;
import org.cloudburstmc.server.utils.BlockColor;

@Log4j2
public class BlockBehaviorChest extends BlockBehaviorTransparent {

    @Override
    public boolean canWaterlogSource() {
        return true;
    }

    @Override
    public boolean canBeActivated(Block block) {
        return true;
    }



    @Override
    public float getResistance() {
        return 12.5f;
    }

    @Override
    public ToolType getToolType(BlockState state) {
        return ItemToolBehavior.TYPE_AXE;
    }

//    @Override
//    public float getMinX() {
//        return this.getX() + 0.0625f;
//    }
//
//    @Override
//    public float getMinY() {
//        return this.getY();
//    }
//
//    @Override
//    public float getMinZ() {
//        return this.getZ() + 0.0625f;
//    }
//
//    @Override
//    public float getMaxX() {
//        return this.getX() + 0.9375f;
//    }
//
//    @Override
//    public float getMaxY() {
//        return this.getY() + 0.9475f;
//    }
//
//    @Override
//    public float getMaxZ() {
//        return this.getZ() + 0.9375f;
//    }


    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        Direction chestFace = player.getHorizontalDirection().getOpposite();
        Axis axis = chestFace.getAxis() == Axis.X ? Axis.Z : Axis.X;

        Chest chest = null;
        for (Direction direction : Plane.HORIZONTAL) {
            if (direction.getAxis() != axis) {
                continue;
            }

            Block b = block.getSide(direction);
            BlockState state = b.getState();

            if (state.getType() != BlockTypes.CHEST || state.ensureTrait(BlockTraits.FACING_DIRECTION) != chestFace) {
                continue;
            }

            BlockEntity be = block.getLevel().getBlockEntity(b.getPosition());
            if (be instanceof Chest) {
                chest = (Chest) be;
            }
        }

        placeBlock(block, BlockRegistry.get().getBlock(BlockTypes.CHEST).withTrait(BlockTraits.FACING_DIRECTION, chestFace));

        Chest chest1 = BlockEntityRegistry.get().newEntity(BlockEntityTypes.CHEST, block.getChunk(), block.getPosition());
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
    public boolean onBreak(Block block, ItemStack item) {
        BlockEntity t = block.getLevel().getBlockEntity(block.getPosition());
        if (t instanceof Chest) {
            ((Chest) t).unpair();
        }
        return super.onBreak(block, item);
    }

    @Override
    public boolean onActivate(Block block, ItemStack item, Player player) {
        if (player != null) {
            Block top = block.up();
            if (!top.getState().inCategory(BlockCategory.TRANSPARENT)) {
                return true;
            }

            BlockEntity t = block.getLevel().getBlockEntity(block.getPosition());
            Chest chest;
            if (t instanceof Chest) {
                chest = (Chest) t;
            } else {
                chest = BlockEntityRegistry.get().newEntity(BlockEntityTypes.CHEST, block.getChunk(), block.getPosition());
            }

            player.addWindow(chest.getInventory());
        }

        return true;
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.WOOD_BLOCK_COLOR;
    }

    public boolean hasComparatorInputOverride() {
        return true;
    }

    public int getComparatorInputOverride(Block block) {
        BlockEntity blockEntity = block.getLevel().getBlockEntity(block.getPosition());

        if (blockEntity instanceof Chest) {
            return ContainerInventory.calculateRedstone(((Chest) blockEntity).getInventory());
        }

        return super.getComparatorInputOverride(block);
    }

    @Override
    public ItemStack toItem(Block block) {
        return CloudItemRegistry.get().getItem(BlockTypes.CHEST);
    }
}
