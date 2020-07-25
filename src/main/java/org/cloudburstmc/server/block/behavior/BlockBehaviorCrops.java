package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.Server;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.event.block.BlockGrowEvent;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemIds;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.particle.BoneMealParticle;
import org.cloudburstmc.server.math.BlockFace;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;

import java.util.concurrent.ThreadLocalRandom;

import static org.cloudburstmc.server.block.BlockTypes.FARMLAND;

public abstract class BlockBehaviorCrops extends FloodableBlockBehavior {

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    public boolean place(Item item, Block block, Block target, BlockFace face, Vector3f clickPos, Player player) {
        if (block.down().getId() == FARMLAND) {
            this.getLevel().setBlock(block.getPosition(), this, true, true);
            return true;
        }
        return false;
    }

    @Override
    public boolean onActivate(Block block, Item item, Player player) {
        //Bone meal
        if (item.getId() == ItemIds.DYE && item.getMeta() == 0x0f) {
            if (this.getMeta() < 7) {
                BlockBehaviorCrops block = (BlockBehaviorCrops) this.clone();
                block.setMeta(block.getMeta() + ThreadLocalRandom.current().nextInt(3) + 2);
                if (block.getMeta() > 7) {
                    block.setMeta(7);
                }
                BlockGrowEvent ev = new BlockGrowEvent(this, block);
                Server.getInstance().getPluginManager().callEvent(ev);

                if (ev.isCancelled()) {
                    return false;
                }

                this.getLevel().setBlock(this.getPosition(), ev.getNewState(), false, true);
                this.level.addParticle(new BoneMealParticle(this.getPosition()));

                if (player != null && player.getGamemode().isSurvival()) {
                    item.decrementCount();
                }
            }

            return true;
        }

        return false;
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL) {
            if (this.down().getId() != FARMLAND) {
                this.getLevel().useBreakOn(this.getPosition());
                return Level.BLOCK_UPDATE_NORMAL;
            }
        } else if (type == Level.BLOCK_UPDATE_RANDOM) {
            if (ThreadLocalRandom.current().nextInt(2) == 1) {
                if (this.getMeta() < 0x07) {
                    BlockBehaviorCrops block = (BlockBehaviorCrops) this.clone();
                    block.setMeta(block.getMeta() + 1);
                    BlockGrowEvent ev = new BlockGrowEvent(this, block);
                    Server.getInstance().getPluginManager().callEvent(ev);

                    if (!ev.isCancelled()) {
                        this.getLevel().setBlock(this.getPosition(), ev.getNewState(), false, true);
                    } else {
                        return Level.BLOCK_UPDATE_RANDOM;
                    }
                }
            } else {
                return Level.BLOCK_UPDATE_RANDOM;
            }
        }

        return 0;
    }

    @Override
    public BlockColor getColor(BlockState state) {
        return BlockColor.FOLIAGE_BLOCK_COLOR;
    }
}
