package org.cloudburstmc.server.block.behavior;

import lombok.val;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.event.block.BlockGrowEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.registry.BlockRegistry;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import java.util.concurrent.ThreadLocalRandom;

import static org.cloudburstmc.api.block.BlockTypes.*;

public class BlockBehaviorStemMelon extends BlockBehaviorCrops {

    @Override
    public int onUpdate(Block block, int type) {
        if (type == CloudLevel.BLOCK_UPDATE_NORMAL) {
            if (block.down().getState().getType() != FARMLAND) {
                block.getLevel().useBreakOn(block.getPosition());
                return CloudLevel.BLOCK_UPDATE_NORMAL;
            }
        } else if (type == CloudLevel.BLOCK_UPDATE_RANDOM) {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            if (random.nextBoolean()) {
                val state = block.getState();
                if (state.ensureTrait(BlockTraits.GROWTH) < 7) {
                    BlockGrowEvent ev = new BlockGrowEvent(block, state.incrementTrait(BlockTraits.GROWTH));
                    CloudServer.getInstance().getEventManager().fire(ev);
                    if (!ev.isCancelled()) {
                        block.set(ev.getNewState(), true);
                    }
                    return CloudLevel.BLOCK_UPDATE_RANDOM;
                } else {
                    for (Direction face : Direction.Plane.HORIZONTAL) {
                        val b = block.getSide(face).getState();
                        if (b.getType() == MELON_BLOCK) {
                            return CloudLevel.BLOCK_UPDATE_RANDOM;
                        }
                    }
                    Block side = block.getSide(Direction.Plane.HORIZONTAL.random(random));
                    BlockState d = side.down().getState();
                    if (side.getState().getType() == AIR && (d.getType() == FARMLAND || d.getType() == GRASS || d.getType() == DIRT)) {
                        BlockGrowEvent ev = new BlockGrowEvent(side, BlockRegistry.get().getBlock(MELON_BLOCK));
                        CloudServer.getInstance().getEventManager().fire(ev);
                        if (!ev.isCancelled()) {
                            side.set(ev.getNewState(), true);
                        }
                    }
                }
            }
            return CloudLevel.BLOCK_UPDATE_RANDOM;
        }
        return 0;
    }

    @Override
    public ItemStack toItem(Block block) {
        return CloudItemRegistry.get().getItem(ItemTypes.MELON_SEEDS);
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        return new ItemStack[]{
                CloudItemRegistry.get().getItem(ItemTypes.MELON_SEEDS, ThreadLocalRandom.current().nextInt(0, 4))
        };
    }
}
