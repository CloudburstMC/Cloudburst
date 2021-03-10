package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import lombok.val;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockCategory;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.blockentity.ItemFrame;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.item.CloudItemStack;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.Sound;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.BlockEntityRegistry;

import java.util.Random;

import static org.cloudburstmc.api.block.BlockTypes.AIR;
import static org.cloudburstmc.api.blockentity.BlockEntityTypes.ITEM_FRAME;

public class BlockBehaviorItemFrame extends BlockBehaviorTransparent {

    @Override
    public int onUpdate(Block block, int type) {
        if (type == CloudLevel.BLOCK_UPDATE_NORMAL) {
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
    public boolean onActivate(Block block, ItemStack item, CloudPlayer player) {
        val level = block.getLevel();
        BlockEntity blockEntity = level.getBlockEntity(block.getPosition());
        ItemFrame itemFrame = (ItemFrame) blockEntity;
        if (itemFrame.getItem() == null || itemFrame.getItem().getType() == AIR) {
            if (player != null && player.isSurvival()) {
                player.getInventory().decrementHandCount();
            }

            itemFrame.setItem(item.withAmount(1));
            level.addSound(block.getPosition(), Sound.BLOCK_ITEMFRAME_ADD_ITEM);
        } else {
            itemFrame.setItemRotation((itemFrame.getItemRotation() + 1) % 8);
            level.addSound(block.getPosition(), Sound.BLOCK_ITEMFRAME_ROTATE_ITEM);
        }
        return true;
    }

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, CloudPlayer player) {
        if (!target.getState().inCategory(BlockCategory.TRANSPARENT) && face.getIndex() > 1 && !block.getState().inCategory(BlockCategory.SOLID)) {
            placeBlock(block, item.getBehavior().getBlock(item).withTrait(BlockTraits.FACING_DIRECTION, face));

            ItemFrame frame = BlockEntityRegistry.get().newEntity(ITEM_FRAME, block);
            frame.loadAdditionalData(((CloudItemStack) item).getDataTag());

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
                    toItem(block), itemFrame.getItem()
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
    public int getComparatorInputOverride(Block block) {
        BlockEntity blockEntity = block.getLevel().getBlockEntity(block.getPosition());

        if (blockEntity instanceof ItemFrame) {
            return ((ItemFrame) blockEntity).getAnalogOutput();
        }

        return super.getComparatorInputOverride(block);
    }


}
