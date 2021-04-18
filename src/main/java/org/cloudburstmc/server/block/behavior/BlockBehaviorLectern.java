package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import lombok.val;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.api.blockentity.Lectern;
import org.cloudburstmc.api.event.block.BlockRedstoneEvent;
import org.cloudburstmc.api.event.block.LecternDropBookEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.BlockColor;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.Sound;
import org.cloudburstmc.server.registry.BlockEntityRegistry;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.registry.CloudItemRegistry;

public class BlockBehaviorLectern extends BlockBehaviorTransparent {

    @Override
    public boolean canBeActivated(Block block) {
        return true;
    }


//    @Override //TODO: bounding box
//    public float getMaxY() {
//        return this.getY() + 0.89999f;
//    }



    @Override
    public int getComparatorInputOverride(Block block) {
        int power = 0;
        int page = 0;
        int maxPage = 0;
        BlockEntity blockEntity = block.getLevel().getBlockEntity(block.getPosition());
        if (blockEntity instanceof Lectern) {
            Lectern lectern = (Lectern) blockEntity;
            if (lectern.hasBook()) {
                maxPage = lectern.getTotalPages();
                page = lectern.getPage() + 1;
                power = (int) (((float) page / maxPage) * 16);
            }
        }
        return power;
    }

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        if (placeBlock(block, CloudBlockRegistry.get().getBlock(BlockTypes.LECTERN).withTrait(
                BlockTraits.DIRECTION,
                player != null ? player.getHorizontalDirection() : Direction.NORTH)
        )) {
            BlockEntityRegistry.get().newEntity(BlockEntityTypes.LECTERN, block);

            return true;
        }

        return false;
    }

    @Override
    public boolean onActivate(Block block, ItemStack item, Player player) {
        if (player != null) {
            BlockEntity t = block.getLevel().getBlockEntity(block.getPosition());
            Lectern lectern;
            if (t instanceof Lectern) {
                lectern = (Lectern) t;
            } else {
                lectern = BlockEntityRegistry.get().newEntity(BlockEntityTypes.LECTERN, block);
            }

            ItemStack currentBook = lectern.getBook();
            if (currentBook != null && currentBook.isNull()) {
                if (item.getType() == ItemTypes.WRITTEN_BOOK || item.getType() == ItemTypes.WRITABLE_BOOK) {
                    if (player.isSurvival()) {
                        player.getInventory().decrementHandCount();
                    }

                    lectern.setBook(item.withAmount(1));
                    lectern.spawnToAll();
                    ((CloudLevel) block.getLevel()).addSound(block.getPosition(), Sound.ITEM_BOOK_PUT);
                }
            }
        }

        return true;
    }


    public boolean isActivated(BlockState state) {
        return state.ensureTrait(BlockTraits.IS_POWERED);
    }

    public void executeRedstonePulse(Block block) {
        val level = block.getLevel();
        if (isActivated(block.getState())) {
            level.cancelScheduledUpdate(block.getPosition());
        } else {
            level.getServer().getEventManager().fire(new BlockRedstoneEvent(block, 0, 15));
        }

        level.scheduleUpdate(block.getPosition(), 4);

        block.set(block.getState().withTrait(BlockTraits.IS_POWERED, false), true);
        ((CloudLevel)level).addSound(block.getPosition(), Sound.ITEM_BOOK_PAGE_TURN);

        ((CloudLevel)level).updateAroundRedstone(block.getPosition(), null);
    }

    @Override
    public int getWeakPower(Block block, Direction face) {
        return isActivated(block.getState()) ? 15 : 0;
    }

    @Override
    public int getStrongPower(Block block, Direction side) {
        return 0;
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == CloudLevel.BLOCK_UPDATE_SCHEDULED) {
            val state = block.getState();
            if (isActivated(state)) {
                block.getLevel().getServer().getEventManager().fire(new BlockRedstoneEvent(block, 15, 0));

                block.set(state.withTrait(BlockTraits.IS_POWERED, false));
                ((CloudLevel)block.getLevel()).updateAroundRedstone(block.getPosition(), null);
            }

            return CloudLevel.BLOCK_UPDATE_SCHEDULED;
        }

        return 0;
    }

    @Override
    public BlockColor getColor(Block state) {
        return BlockColor.WOOD_BLOCK_COLOR;
    }

    public void dropBook(Block block, Player player) {
        BlockEntity blockEntity = block.getLevel().getBlockEntity(block.getPosition());
        if (blockEntity instanceof Lectern) {
            Lectern lectern = (Lectern) blockEntity;
            ItemStack book = lectern.getBook();
            if (book != null && !book.isNull()) {
                LecternDropBookEvent dropBookEvent = new LecternDropBookEvent(player, lectern, book);
                block.getLevel().getServer().getEventManager().fire(dropBookEvent);
                if (!dropBookEvent.isCancelled()) {
                    lectern.setBook(CloudItemRegistry.AIR);
                    lectern.spawnToAll();
                    block.getLevel().dropItem(lectern.getPosition().add(0.5f, 1, 0.5f), dropBookEvent.getBook());
                }
            }
        }
    }
}