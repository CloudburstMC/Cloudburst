package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import lombok.val;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockCategory;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.blockentity.BlockEntity;
import org.cloudburstmc.server.blockentity.ItemFrame;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.Sound;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.BlockEntityRegistry;

import java.util.Random;

import static org.cloudburstmc.server.block.BlockTypes.AIR;
import static org.cloudburstmc.server.blockentity.BlockEntityTypes.ITEM_FRAME;

public class BlockBehaviorItemFrame extends BlockBehaviorTransparent {

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL) {
            if (block.getSide(block.getState().ensureTrait(BlockTraits.FACING_DIRECTION)).getState().inCategory(BlockCategory.TRANSPARENT)) {
                block.getLevel().useBreakOn(block.getPosition());
                return type;
            }
        }

        return 0;
    }

    @Override
    public boolean canBeActivated(Block block) {
        return true;
    }

    @Override
    public boolean onActivate(Block block, ItemStack item, Player player) {
        val level = block.getLevel();
        BlockEntity blockEntity = level.getBlockEntity(block.getPosition());
        ItemFrame itemFrame = (ItemFrame) blockEntity;
        if (itemFrame.getItem() == null || itemFrame.getItem().getId() == AIR) {
            ItemStack itemOnFrame = item.clone();
            if (player != null && player.isSurvival()) {
                itemOnFrame.setCount(itemOnFrame.getCount() - 1);
                player.getInventory().setItemInHand(itemOnFrame);
            }
            itemOnFrame.setCount(1);
            itemFrame.setItem(itemOnFrame);
            level.addSound(block.getPosition(), Sound.BLOCK_ITEMFRAME_ADD_ITEM);
        } else {
            itemFrame.setItemRotation((itemFrame.getItemRotation() + 1) % 8);
            level.addSound(block.getPosition(), Sound.BLOCK_ITEMFRAME_ROTATE_ITEM);
        }
        return true;
    }

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        if (!target.getState().inCategory(BlockCategory.TRANSPARENT) && face.getIndex() > 1 && !block.getState().inCategory(BlockCategory.SOLID)) {
            placeBlock(block, item.getBlock().withTrait(BlockTraits.FACING_DIRECTION, face));

            ItemFrame frame = BlockEntityRegistry.get().newEntity(ITEM_FRAME, block);
            frame.loadAdditionalData(item.getTag());

            block.getLevel().addSound(block.getPosition(), Sound.BLOCK_ITEMFRAME_PLACE);
            return true;
        }
        return false;
    }

    @Override
    public boolean onBreak(Block block, ItemStack item) {
        super.onBreak(block, item);
        block.getLevel().addSound(block.getPosition(), Sound.BLOCK_ITEMFRAME_REMOVE_ITEM);
        return true;
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        BlockEntity blockEntity = block.getLevel().getBlockEntity(block.getPosition());
        ItemFrame itemFrame = (ItemFrame) blockEntity;
        int chance = new Random().nextInt(100) + 1;
        if (itemFrame != null && chance <= (itemFrame.getItemDropChance() * 100)) {
            return new ItemStack[]{
                    toItem(block), itemFrame.getItem().clone()
            };
        } else {
            return new ItemStack[]{
                    toItem(block)
            };
        }
    }

    @Override
    public ItemStack toItem(Block block) {
        return ItemStack.get(block.getState().defaultState());
    }

    @Override
    public boolean canPassThrough() {
        return true;
    }

    @Override
    public boolean hasComparatorInputOverride() {
        return true;
    }

    @Override
    public int getComparatorInputOverride(Block block) {
        BlockEntity blockEntity = block.getLevel().getBlockEntity(block.getPosition());

        if (blockEntity instanceof ItemFrame) {
            return ((ItemFrame) blockEntity).getAnalogOutput();
        }

        return super.getComparatorInputOverride(block);
    }


    @Override
    public boolean canWaterlogSource() {
        return true;
    }
}
