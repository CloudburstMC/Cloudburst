package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.Server;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.event.block.LeavesDecayEvent;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.math.BlockFace;
import org.cloudburstmc.server.math.SimpleAxisAlignedBB;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;

import java.util.concurrent.ThreadLocalRandom;

import static org.cloudburstmc.server.block.BlockTypes.*;
import static org.cloudburstmc.server.item.ItemIds.APPLE;
import static org.cloudburstmc.server.item.ItemIds.STICK;

public class BlockBehaviorLeaves extends BlockBehaviorTransparent {
    public static final int OAK = 0;
    public static final int SPRUCE = 1;
    public static final int BIRCH = 2;
    public static final int JUNGLE = 3;

    @Override
    public float getHardness() {
        return 0.2f;
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_SHEARS;
    }

    @Override
    public int getBurnChance() {
        return 30;
    }

    @Override
    public int getBurnAbility() {
        return 60;
    }

    @Override
    public boolean place(Item item, Block block, Block target, BlockFace face, Vector3f clickPos, Player player) {
        this.setPersistent(true);
        this.getLevel().setBlock(this.getPosition(), this, true);
        return true;
    }

    @Override
    public Item toItem(BlockState state) {
        return Item.get(id, this.getMeta() & 0x3, 1);
    }

    @Override
    public Item[] getDrops(BlockState blockState, Item hand) {
        if (hand.isShears()) {
            return new Item[]{
                    toItem(blockState)
            };
        } else {
            if (this.canDropApple() && ThreadLocalRandom.current().nextInt(200) == 0) {
                return new Item[]{
                        Item.get(APPLE)
                };
            }
            if (ThreadLocalRandom.current().nextInt(20) == 0) {
                if (ThreadLocalRandom.current().nextBoolean()) {
                    return new Item[]{
                            Item.get(STICK, 0, ThreadLocalRandom.current().nextInt(1, 2))
                    };
                } else if ((this.getMeta() & 0x03) != JUNGLE || ThreadLocalRandom.current().nextInt(20) == 0) {
                    return new Item[]{
                            this.getSapling()
                    };
                }
            }
        }
        return new Item[0];
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_RANDOM && !isPersistent() && !isCheckDecay()) {
            setCheckDecay(true);
            getLevel().setBlock(this.getPosition(), this, false, false);
        } else if (type == Level.BLOCK_UPDATE_RANDOM && isCheckDecay() && !isPersistent()) {
            setMeta(getMeta() & 0x03);

            LeavesDecayEvent ev = new LeavesDecayEvent(this);

            Server.getInstance().getPluginManager().callEvent(ev);
            if (ev.isCancelled() || findLog(this, 7)) {
                getLevel().setBlock(this.getPosition(), this, false, false);
            } else {
                getLevel().useBreakOn(this.getPosition());
                return Level.BLOCK_UPDATE_NORMAL;
            }
        }
        return 0;
    }

    private Boolean findLog(BlockState pos, Integer distance) {
        for (BlockState collisionBlockState : this.getLevel().getCollisionBlocks(new SimpleAxisAlignedBB(
                pos.getX() - distance, pos.getY() - distance, pos.getZ() - distance,
                pos.getX() + distance, pos.getY() + distance, pos.getZ() + distance))) {
            if (collisionBlockState.getId() == LOG || collisionBlockState.getId() == LOG2) {
                return true;
            }
        }
        return false;
    }

    public boolean isCheckDecay() {
        return (this.getMeta() & 0x08) != 0;
    }

    public void setCheckDecay(boolean checkDecay) {
        if (checkDecay) {
            this.setMeta(this.getMeta() | 0x08);
        } else {
            this.setMeta(this.getMeta() & ~0x08);
        }
    }

    public boolean isPersistent() {
        return (this.getMeta() & 0x04) != 0;
    }

    public void setPersistent(boolean persistent) {
        if (persistent) {
            this.setMeta(this.getMeta() | 0x04);
        } else {
            this.setMeta(this.getMeta() & ~0x04);
        }
    }

    @Override
    public BlockColor getColor() {
        return BlockColor.FOLIAGE_BLOCK_COLOR;
    }

    @Override
    public boolean canSilkTouch() {
        return true;
    }

    protected boolean canDropApple() {
        return (this.getMeta() & 0x03) == OAK;
    }

    protected Item getSapling() {
        return Item.get(SAPLING, this.getMeta() & 0x03);
    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }
}
