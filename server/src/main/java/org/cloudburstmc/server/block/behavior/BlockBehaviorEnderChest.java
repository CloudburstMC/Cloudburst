package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockCategory;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.blockentity.BlockEntity;
import org.cloudburstmc.server.blockentity.BlockEntityTypes;
import org.cloudburstmc.server.blockentity.EnderChest;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ToolType;
import org.cloudburstmc.server.item.behavior.ItemToolBehavior;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.BlockEntityRegistry;
import org.cloudburstmc.server.registry.CloudItemRegistry;
import org.cloudburstmc.server.utils.BlockColor;

import static org.cloudburstmc.server.block.BlockTypes.OBSIDIAN;

public class BlockBehaviorEnderChest extends BlockBehaviorTransparent {

    @Override
    public boolean canBeActivated(Block block) {
        return true;
    }

    @Override
    public int getLightLevel(Block block) {
        return 7;
    }

    @Override
    public float getHardness() {
        return 22.5f;
    }

    @Override
    public float getResistance() {
        return 3000;
    }

    @Override
    public ToolType getToolType() {
        return ItemToolBehavior.TYPE_PICKAXE;
    }

//    @Override
//    public float getMinX() {
//        return this.getX() + 0.0625f;
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
        int[] faces = {2, 5, 3, 4};

        placeBlock(block, item.getBlock().withTrait(BlockTraits.FACING_DIRECTION, player != null ? player.getHorizontalDirection() : Direction.NORTH));

        EnderChest enderChest = BlockEntityRegistry.get().newEntity(BlockEntityTypes.ENDER_CHEST, block);
        enderChest.loadAdditionalData(item.getTag());
        if (item.hasCustomName()) {
            enderChest.setCustomName(item.getCustomName());
        }
        return true;
    }

    @Override
    public boolean onActivate(Block block, ItemStack item, Player player) {
        if (player != null) {
            BlockState top = block.up().getState();
            if (!top.inCategory(BlockCategory.TRANSPARENT)) {
                return true;
            }

            BlockEntity blockEntity = block.getLevel().getBlockEntity(block.getPosition());
            if (!(blockEntity instanceof EnderChest)) {
                BlockEntityRegistry.get().newEntity(BlockEntityTypes.ENDER_CHEST, block.getChunk(), block.getPosition());
            }

            player.setViewingEnderChest((EnderChest) blockEntity);
            player.addWindow(player.getEnderChestInventory());
        }

        return true;
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        if (hand.isPickaxe() && hand.getTier() >= ItemToolBehavior.TIER_WOODEN) {
            return new ItemStack[]{
                    ItemStack.get(OBSIDIAN, 0, 8)
            };
        } else {
            return new ItemStack[0];
        }
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.OBSIDIAN_BLOCK_COLOR;
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }

    @Override
    public boolean canSilkTouch() {
        return true;
    }

    @Override
    public ItemStack toItem(Block block) {
        return CloudItemRegistry.get().getItem(block.getState().defaultState());
    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }
}
