package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.blockentity.BlockEntity;
import org.cloudburstmc.server.blockentity.BlockEntityTypes;
import org.cloudburstmc.server.blockentity.Lectern;
import org.cloudburstmc.server.event.block.BlockRedstoneEvent;
import org.cloudburstmc.server.event.block.LecternDropBookEvent;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemIds;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.Sound;
import org.cloudburstmc.server.math.BlockFace;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.BlockEntityRegistry;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorLectern extends BlockBehaviorTransparent {

    @Override
    public boolean canBeActivated() {
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

    @Override
    public float getMaxY() {
        return this.getY() + 0.89999f;
    }

    @Override
    public boolean hasComparatorInputOverride() {
        return true;
    }

    @Override
    public int getComparatorInputOverride() {
        int power = 0;
        int page = 0;
        int maxPage = 0;
        BlockEntity blockEntity = this.getLevel().getBlockEntity(this.getPosition());
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

    public void setBlockFace(BlockFace face) {
        final int dataMask = (1 << 6) - 1;

        int horizontalIndex = face.getHorizontalIndex();
        if (horizontalIndex >= 0) {
            setMeta(getMeta() & (dataMask ^ 0b11) | (horizontalIndex & 0b11));
        }
    }

    @Override
    public boolean place(Item item, Block block, Block target, BlockFace face, Vector3f clickPos, Player player) {
        setBlockFace(player != null ? player.getDirection().getOpposite() : BlockFace.SOUTH);

        Lectern lectern = BlockEntityRegistry.get().newEntity(BlockEntityTypes.LECTERN, this.getChunk(), this.getPosition());

        this.getLevel().setBlock(blockState.getPosition(), this, true, true);
        return true;
    }

    @Override
    public boolean onActivate(Block block, Item item, Player player) {
        if (player != null) {
            BlockEntity t = this.getLevel().getBlockEntity(this.getPosition());
            Lectern lectern;
            if (t instanceof Lectern) {
                lectern = (Lectern) t;
            } else {
                lectern = BlockEntityRegistry.get().newEntity(BlockEntityTypes.LECTERN, this.getChunk(), this.getPosition());
            }

            Item currentBook = lectern.getBook();
            if (currentBook.getId() == BlockTypes.AIR) {
                if (item.getId() == ItemIds.WRITTEN_BOOK || item.getId() == ItemIds.WRITABLE_BOOK) {
                    Item newBook = item.clone();
                    if (player.isSurvival()) {
                        newBook.setCount(newBook.getCount() - 1);
                        player.getInventory().setItemInHand(newBook);
                    }
                    newBook.setCount(1);
                    lectern.setBook(newBook);
                    lectern.spawnToAll();
                    this.getLevel().addSound(this.getPosition(), Sound.ITEM_BOOK_PUT);
                }
            }
        }

        return true;
    }

    @Override
    public boolean isPowerSource() {
        return true;
    }

    public boolean isActivated() {
        return (this.getMeta() & 0x04) == 0x04;
    }

    public void setActivated(boolean activated) {
        if (activated) {
            setMeta(getMeta() | 0x04);
        } else {
            setMeta(getMeta() ^ 0x04);
        }
    }

    public void executeRedstonePulse() {
        if (isActivated()) {
            level.cancelSheduledUpdate(this.getPosition(), this);
        } else {
            this.level.getServer().getPluginManager().callEvent(new BlockRedstoneEvent(this, 0, 15));
        }

        level.scheduleUpdate(this, this.getPosition(), 4);
        setActivated(true);
        level.setBlock(this.getPosition(), this, true, false);
        level.addSound(this.getPosition(), Sound.ITEM_BOOK_PAGE_TURN);

        level.updateAroundRedstone(this.getPosition(), null);
    }

    @Override
    public int getWeakPower(BlockFace face) {
        return isActivated() ? 15 : 0;
    }

    @Override
    public int getStrongPower(BlockFace side) {
        return 0;
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_SCHEDULED) {
            if (isActivated()) {
                this.level.getServer().getPluginManager().callEvent(new BlockRedstoneEvent(this, 15, 0));

                setActivated(false);
                level.setBlock(this.getPosition(), this, true, false);
                level.updateAroundRedstone(this.getPosition(), null);
            }

            return Level.BLOCK_UPDATE_SCHEDULED;
        }

        return 0;
    }

    @Override
    public BlockColor getColor(BlockState state) {
        return BlockColor.WOOD_BLOCK_COLOR;
    }

    public void dropBook(Player player) {
        BlockEntity blockEntity = this.getLevel().getBlockEntity(this.getPosition());
        if (blockEntity instanceof Lectern) {
            Lectern lectern = (Lectern) blockEntity;
            Item book = lectern.getBook();
            if (book.getId() != BlockTypes.AIR) {
                LecternDropBookEvent dropBookEvent = new LecternDropBookEvent(player, lectern, book);
                this.getLevel().getServer().getPluginManager().callEvent(dropBookEvent);
                if (!dropBookEvent.isCancelled()) {
                    lectern.setBook(Item.get(BlockTypes.AIR));
                    lectern.spawnToAll();
                    this.level.dropItem(lectern.getPosition().add(0.5f, 1, 0.5f), dropBookEvent.getBook());
                }
            }
        }
    }
}