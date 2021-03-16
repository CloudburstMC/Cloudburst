package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import lombok.val;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.event.block.BlockGrowEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.BlockColor;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.particle.BoneMealParticle;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.utils.data.DyeColor;

import java.util.concurrent.ThreadLocalRandom;

import static org.cloudburstmc.api.block.BlockTypes.FARMLAND;

public abstract class BlockBehaviorCrops extends FloodableBlockBehavior {

    @Override
    public boolean canBeActivated(Block block) {
        return true;
    }

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, CloudPlayer player) {
        if (block.down().getState().getType() == FARMLAND) {
            placeBlock(block, item);
            return true;
        }
        return false;
    }

    @Override
    public boolean onActivate(Block block, ItemStack item, CloudPlayer player) {
        //Bone meal
        if (item.getType() == ItemTypes.DYE && item.getMetadata(DyeColor.class) == DyeColor.WHITE) {
            if (block.getState().ensureTrait(BlockTraits.GROWTH) < 7) {
                BlockGrowEvent ev = new BlockGrowEvent(block, block.getState().incrementTrait(BlockTraits.GROWTH));
                CloudServer.getInstance().getEventManager().fire(ev);

                if (ev.isCancelled()) {
                    return false;
                }

                block.set(ev.getNewState(), false);
                block.getLevel().addParticle(new BoneMealParticle(block.getPosition()));

                if (player != null && player.getGamemode().isSurvival()) {
                    player.getInventory().decrementHandCount();
                }
            }

            return true;
        }

        return false;
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == CloudLevel.BLOCK_UPDATE_NORMAL) {
            if (block.down().getState().getType() != FARMLAND) {
                removeBlock(block, true);
                return CloudLevel.BLOCK_UPDATE_NORMAL;
            }
        } else if (type == CloudLevel.BLOCK_UPDATE_RANDOM) {
            if (ThreadLocalRandom.current().nextInt(2) == 1) {
                val state = block.getState();
                if (state.ensureTrait(BlockTraits.GROWTH) < 0x07) {
                    BlockGrowEvent ev = new BlockGrowEvent(block, state.incrementTrait(BlockTraits.GROWTH));
                    CloudServer.getInstance().getEventManager().fire(ev);

                    if (!ev.isCancelled()) {
                        block.set(ev.getNewState(), false, true);
                    } else {
                        return CloudLevel.BLOCK_UPDATE_RANDOM;
                    }
                }
            } else {
                return CloudLevel.BLOCK_UPDATE_RANDOM;
            }
        }

        return 0;
    }

    @Override
    public BlockColor getColor(Block state) {
        return BlockColor.FOLIAGE_BLOCK_COLOR;
    }
}
