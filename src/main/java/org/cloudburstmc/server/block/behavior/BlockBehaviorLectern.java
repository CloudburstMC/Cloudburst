package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import lombok.val;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockIds;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.blockentity.BlockEntity;
import org.cloudburstmc.server.blockentity.BlockEntityTypes;
import org.cloudburstmc.server.blockentity.Lectern;
import org.cloudburstmc.server.event.block.BlockRedstoneEvent;
import org.cloudburstmc.server.event.block.LecternDropBookEvent;
import org.cloudburstmc.server.item.behavior.Item;
import org.cloudburstmc.server.item.behavior.ItemIds;
import org.cloudburstmc.server.item.behavior.ItemTool;
import org.cloudburstmc.server.world.World;
import org.cloudburstmc.server.world.Sound;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.BlockEntityRegistry;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorLectern extends BlockBehaviorTransparent {

    @Override
    public boolean canBeActivated(Block block) {
        return true;
    }

    @Override
    public float getHardness() {
        return 2;
    }

    @Override
    public float getResistance() {
        return 12.5f;
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_AXE;
    }

//    @Override //TODO: bounding box
//    public float getMaxY() {
//        return this.getY() + 0.89999f;
//    }

    @Override
    public boolean hasComparatorInputOverride() {
        return true;
    }

    @Override
    public int getComparatorInputOverride(Block block) {
        int power = 0;
        int page = 0;
        int maxPage = 0;
        BlockEntity blockEntity = block.getWorld().getBlockEntity(block.getPosition());
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
    public boolean place(Item item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        if (placeBlock(block, BlockState.get(BlockIds.LECTERN).withTrait(
                BlockTraits.DIRECTION,
                player != null ? player.getHorizontalDirection() : Direction.NORTH)
        )) {
            BlockEntityRegistry.get().newEntity(BlockEntityTypes.LECTERN, block);

            return true;
        }

        return false;
    }

    @Override
    public boolean onActivate(Block block, Item item, Player player) {
        if (player != null) {
            BlockEntity t = block.getWorld().getBlockEntity(block.getPosition());
            Lectern lectern;
            if (t instanceof Lectern) {
                lectern = (Lectern) t;
            } else {
                lectern = BlockEntityRegistry.get().newEntity(BlockEntityTypes.LECTERN, block);
            }

            Item currentBook = lectern.getBook();
            if (currentBook != null && currentBook.getId() == BlockIds.AIR) {
                if (item.getId() == ItemIds.WRITTEN_BOOK || item.getId() == ItemIds.WRITABLE_BOOK) {
                    Item newBook = item.clone();
                    if (player.isSurvival()) {
                        newBook.setCount(newBook.getCount() - 1);
                        player.getInventory().setItemInHand(newBook);
                    }
                    newBook.setCount(1);
                    lectern.setBook(newBook);
                    lectern.spawnToAll();
                    block.getWorld().addSound(block.getPosition(), Sound.ITEM_BOOK_PUT);
                }
            }
        }

        return true;
    }

    @Override
    public boolean isPowerSource(Block block) {
        return true;
    }

    public boolean isActivated(BlockState state) {
        return state.ensureTrait(BlockTraits.IS_POWERED);
    }

    public void executeRedstonePulse(Block block) {
        val level = block.getWorld();
        if (isActivated(block.getState())) {
            level.cancelSheduledUpdate(block.getPosition(), block);
        } else {
            level.getServer().getEventManager().fire(new BlockRedstoneEvent(block, 0, 15));
        }

        level.scheduleUpdate(block.getPosition(), 4);

        block.set(block.getState().withTrait(BlockTraits.IS_POWERED, false), true);
        level.addSound(block.getPosition(), Sound.ITEM_BOOK_PAGE_TURN);

        level.updateAroundRedstone(block.getPosition(), null);
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
        if (type == World.BLOCK_UPDATE_SCHEDULED) {
            val state = block.getState();
            if (isActivated(state)) {
                block.getWorld().getServer().getEventManager().fire(new BlockRedstoneEvent(block, 15, 0));

                block.set(state.withTrait(BlockTraits.IS_POWERED, false));
                block.getWorld().updateAroundRedstone(block.getPosition(), null);
            }

            return World.BLOCK_UPDATE_SCHEDULED;
        }

        return 0;
    }

    @Override
    public BlockColor getColor(Block state) {
        return BlockColor.WOOD_BLOCK_COLOR;
    }

    public void dropBook(Block block, Player player) {
        BlockEntity blockEntity = block.getWorld().getBlockEntity(block.getPosition());
        if (blockEntity instanceof Lectern) {
            Lectern lectern = (Lectern) blockEntity;
            Item book = lectern.getBook();
            if (book != null && book.getId() != BlockIds.AIR) {
                LecternDropBookEvent dropBookEvent = new LecternDropBookEvent(player, lectern, book);
                block.getWorld().getServer().getEventManager().fire(dropBookEvent);
                if (!dropBookEvent.isCancelled()) {
                    lectern.setBook(Item.get(BlockIds.AIR));
                    lectern.spawnToAll();
                    block.getWorld().dropItem(lectern.getPosition().add(0.5f, 1, 0.5f), dropBookEvent.getBook());
                }
            }
        }
    }
}