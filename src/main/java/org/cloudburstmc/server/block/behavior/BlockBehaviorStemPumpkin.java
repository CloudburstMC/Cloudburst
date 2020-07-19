package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.Server;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.event.block.BlockGrowEvent;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemIds;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.math.BlockFace;
import org.cloudburstmc.server.utils.Identifier;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by Pub4Game on 15.01.2016.
 */
public class BlockBehaviorStemPumpkin extends BlockBehaviorCrops {

    public BlockBehaviorStemPumpkin(Identifier id) {
        super(id);
    }

    @Override
    public int onUpdate(int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL) {
            if (this.down().getId() != BlockTypes.FARMLAND) {
                this.getLevel().useBreakOn(this.getPosition());
                return Level.BLOCK_UPDATE_NORMAL;
            }
        } else if (type == Level.BLOCK_UPDATE_RANDOM) {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            if (random.nextBoolean()) {
                if (this.getMeta() < 0x07) {
                    BlockState blockState = this.clone();
                    blockState.setMeta(blockState.getMeta() + 1);
                    BlockGrowEvent ev = new BlockGrowEvent(this, blockState);
                    Server.getInstance().getPluginManager().callEvent(ev);
                    if (!ev.isCancelled()) {
                        this.getLevel().setBlock(this.getPosition(), ev.getNewState(), true);
                    }
                    return Level.BLOCK_UPDATE_RANDOM;
                } else {
                    for (BlockFace face : BlockFace.Plane.HORIZONTAL) {
                        BlockState b = this.getSide(face);
                        if (b.getId() == BlockTypes.PUMPKIN) {
                            return Level.BLOCK_UPDATE_RANDOM;
                        }
                    }
                    BlockState side = this.getSide(BlockFace.Plane.HORIZONTAL.random(random));
                    BlockState d = side.down();
                    if (side.getId() == BlockTypes.AIR && (d.getId() == BlockTypes.FARMLAND || d.getId() == BlockTypes.GRASS || d.getId() == BlockTypes.DIRT)) {
                        BlockGrowEvent ev = new BlockGrowEvent(side, BlockState.get(BlockTypes.PUMPKIN));
                        Server.getInstance().getPluginManager().callEvent(ev);
                        if (!ev.isCancelled()) {
                            this.getLevel().setBlock(side.getPosition(), ev.getNewState(), true);
                        }
                    }
                }
            }
            return Level.BLOCK_UPDATE_RANDOM;
        }
        return 0;
    }

    @Override
    public Item toItem() {
        return Item.get(ItemIds.PUMPKIN_SEEDS);
    }

    @Override
    public Item[] getDrops(Item hand) {
        return new Item[]{
                Item.get(ItemIds.PUMPKIN_SEEDS, 0, ThreadLocalRandom.current().nextInt(0, 4))
        };
    }
}
