package org.cloudburstmc.server.block.behavior;

import lombok.val;
import org.cloudburstmc.server.Server;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.event.block.BlockGrowEvent;
import org.cloudburstmc.server.item.behavior.Item;
import org.cloudburstmc.server.item.behavior.ItemIds;
import org.cloudburstmc.server.world.World;
import org.cloudburstmc.server.math.Direction;

import java.util.concurrent.ThreadLocalRandom;

import static org.cloudburstmc.server.block.BlockIds.*;

public class BlockBehaviorStemMelon extends BlockBehaviorCrops {

    @Override
    public int onUpdate(Block block, int type) {
        if (type == World.BLOCK_UPDATE_NORMAL) {
            if (block.down().getState().getType() != FARMLAND) {
                block.getWorld().useBreakOn(block.getPosition());
                return World.BLOCK_UPDATE_NORMAL;
            }
        } else if (type == World.BLOCK_UPDATE_RANDOM) {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            if (random.nextBoolean()) {
                val state = block.getState();
                if (state.ensureTrait(BlockTraits.GROWTH) < 7) {
                    BlockGrowEvent ev = new BlockGrowEvent(block, state.incrementTrait(BlockTraits.GROWTH));
                    Server.getInstance().getEventManager().fire(ev);
                    if (!ev.isCancelled()) {
                        block.set(ev.getNewState(), true);
                    }
                    return World.BLOCK_UPDATE_RANDOM;
                } else {
                    for (Direction face : Direction.Plane.HORIZONTAL) {
                        val b = block.getSide(face).getState();
                        if (b.getType() == MELON_BLOCK) {
                            return World.BLOCK_UPDATE_RANDOM;
                        }
                    }
                    Block side = block.getSide(Direction.Plane.HORIZONTAL.random(random));
                    BlockState d = side.down().getState();
                    if (side.getState().getType() == AIR && (d.getType() == FARMLAND || d.getType() == GRASS || d.getType() == DIRT)) {
                        BlockGrowEvent ev = new BlockGrowEvent(side, BlockState.get(MELON_BLOCK));
                        Server.getInstance().getEventManager().fire(ev);
                        if (!ev.isCancelled()) {
                            side.set(ev.getNewState(), true);
                        }
                    }
                }
            }
            return World.BLOCK_UPDATE_RANDOM;
        }
        return 0;
    }

    @Override
    public Item toItem(Block block) {
        return Item.get(ItemIds.MELON_SEEDS);
    }

    @Override
    public Item[] getDrops(Block block, Item hand) {
        return new Item[]{
                Item.get(ItemIds.MELON_SEEDS, 0, ThreadLocalRandom.current().nextInt(0, 4))
        };
    }
}
