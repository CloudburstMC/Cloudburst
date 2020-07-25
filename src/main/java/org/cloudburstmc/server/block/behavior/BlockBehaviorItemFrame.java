package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.blockentity.BlockEntity;
import org.cloudburstmc.server.blockentity.ItemFrame;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemIds;
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
            if (this.getSide(getFacing()).isTransparent()) {
                this.level.useBreakOn(this.getPosition());
                return type;
            }
        }

        return 0;
    }

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    public boolean onActivate(Block block, Item item, Player player) {
        BlockEntity blockEntity = this.getLevel().getBlockEntity(this.getPosition());
        ItemFrame itemFrame = (ItemFrame) blockEntity;
        if (itemFrame.getItem() == null || itemFrame.getItem().getId() == AIR) {
            Item itemOnFrame = item.clone();
            if (player != null && player.isSurvival()) {
                itemOnFrame.setCount(itemOnFrame.getCount() - 1);
                player.getInventory().setItemInHand(itemOnFrame);
            }
            itemOnFrame.setCount(1);
            itemFrame.setItem(itemOnFrame);
            this.getLevel().addSound(this.getPosition(), Sound.BLOCK_ITEMFRAME_ADD_ITEM);
        } else {
            itemFrame.setItemRotation((itemFrame.getItemRotation() + 1) % 8);
            this.getLevel().addSound(this.getPosition(), Sound.BLOCK_ITEMFRAME_ROTATE_ITEM);
        }
        return true;
    }

    @Override
    public boolean place(Item item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        if (!target.isTransparent() && face.getIndex() > 1 && !blockState.isSolid()) {
            switch (face) {
                case NORTH:
                    this.setMeta(3);
                    break;
                case SOUTH:
                    this.setMeta(2);
                    break;
                case WEST:
                    this.setMeta(1);
                    break;
                case EAST:
                    this.setMeta(0);
                    break;
                default:
                    return false;
            }
            this.getLevel().setBlock(blockState.getPosition(), this, true, true);

            ItemFrame frame = BlockEntityRegistry.get().newEntity(ITEM_FRAME, this.getChunk(), this.getPosition());
            frame.loadAdditionalData(item.getTag());

            this.getLevel().addSound(this.getPosition(), Sound.BLOCK_ITEMFRAME_PLACE);
            return true;
        }
        return false;
    }

    @Override
    public boolean onBreak(Block block, Item item) {
        super.onBreak(block, item);
        this.getLevel().addSound(this.getPosition(), Sound.BLOCK_ITEMFRAME_REMOVE_ITEM);
        return true;
    }

    @Override
    public Item[] getDrops(BlockState blockState, Item hand) {
        BlockEntity blockEntity = this.getLevel().getBlockEntity(this.getPosition());
        ItemFrame itemFrame = (ItemFrame) blockEntity;
        int chance = new Random().nextInt(100) + 1;
        if (itemFrame != null && chance <= (itemFrame.getItemDropChance() * 100)) {
            return new Item[]{
                    toItem(blockState), itemFrame.getItem().clone()
            };
        } else {
            return new Item[]{
                    toItem(blockState)
            };
        }
    }

    @Override
    public Item toItem(BlockState state) {
        return Item.get(ItemIds.FRAME);
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
    public int getComparatorInputOverride() {
        BlockEntity blockEntity = this.level.getBlockEntity(this.getPosition());

        if (blockEntity instanceof ItemFrame) {
            return ((ItemFrame) blockEntity).getAnalogOutput();
        }

        return super.getComparatorInputOverride();
    }

    public Direction getFacing() {
        switch (this.getMeta() & 3) {
            case 0:
                return Direction.WEST;
            case 1:
                return Direction.EAST;
            case 2:
                return Direction.NORTH;
            case 3:
                return Direction.SOUTH;
        }

        return null;
    }

    @Override
    public float getHardness() {
        return 0.25f;
    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }
}
