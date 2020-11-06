package org.cloudburstmc.server.block.behavior;

import lombok.val;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.event.block.BlockGrowEvent;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.math.Direction;

import java.util.concurrent.ThreadLocalRandom;

import static org.cloudburstmc.server.block.BlockTypes.*;

public class BlockBehaviorStemPumpkin extends BlockBehaviorCrops {

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL) {
            if (block.down().getState().getType() != FARMLAND) {
                block.getLevel().useBreakOn(block.getPosition());
                return Level.BLOCK_UPDATE_NORMAL;
            }
        } else if (type == Level.BLOCK_UPDATE_RANDOM) {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            if (random.nextBoolean()) {
                val state = block.getState();
                if (state.ensureTrait(BlockTraits.GROWTH) < 7) {
                    BlockGrowEvent ev = new BlockGrowEvent(block, state.incrementTrait(BlockTraits.GROWTH));
                    CloudServer.getInstance().getEventManager().fire(ev);
                    if (!ev.isCancelled()) {
                        block.set(ev.getNewState(), true);
                    }
                    return Level.BLOCK_UPDATE_RANDOM;
                } else {
                    for (Direction face : Direction.Plane.HORIZONTAL) {
                        val b = block.getSide(face).getState();
                        if (b.getType() == PUMPKIN) {
                            return Level.BLOCK_UPDATE_RANDOM;
                        }
                    }
                    Block side = block.getSide(Direction.Plane.HORIZONTAL.random(random));
                    BlockState d = side.down().getState();
                    if (side.getState().getType() == AIR && (d.getType() == FARMLAND || d.getType() == GRASS || d.getType() == DIRT)) {
                        BlockGrowEvent ev = new BlockGrowEvent(side, BlockState.get(PUMPKIN));
                        CloudServer.getInstance().getEventManager().fire(ev);
                        if (!ev.isCancelled()) {
                            side.set(ev.getNewState(), true);
                        }
                    }
                }
            }
            return Level.BLOCK_UPDATE_RANDOM;
        }
        return 0;
    }

    @Override
    public ItemStack toItem(Block block) {
        return ItemStack.get(ItemTypes.PUMPKIN_SEEDS);
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        return new ItemStack[]{
                ItemStack.get(ItemTypes.PUMPKIN_SEEDS, ThreadLocalRandom.current().nextInt(0, 4))
        };
    }
}
