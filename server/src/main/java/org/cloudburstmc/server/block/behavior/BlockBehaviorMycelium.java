package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3i;
import org.cloudburstmc.server.Server;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.event.block.BlockSpreadEvent;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.utils.BlockColor;

import java.util.concurrent.ThreadLocalRandom;

import static org.cloudburstmc.server.block.BlockTypes.DIRT;
import static org.cloudburstmc.server.block.BlockTypes.MYCELIUM;

public class BlockBehaviorMycelium extends BlockBehaviorSolid {

    @Override
    public int getToolType() {
        return ItemTool.TYPE_SHOVEL;
    }

    @Override
    public float getHardness() {
        return 0.6f;
    }

    @Override
    public float getResistance() {
        return 2.5f;
    }

    @Override
    public Item[] getDrops(BlockState blockState, Item hand) {
        return new Item[]{
                Item.get(DIRT)
        };
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_RANDOM) {
            //TODO: light levels
            Vector3i pos = this.getPosition();
            int x = ThreadLocalRandom.current().nextInt(pos.getX() - 1, pos.getX() + 1);
            int y = ThreadLocalRandom.current().nextInt(pos.getY() - 1, pos.getY() + 1);
            int z = ThreadLocalRandom.current().nextInt(pos.getZ() - 1, pos.getZ() + 1);
            BlockState blockState = this.getLevel().getBlock(x, y, z);
            if (blockState.getId() == DIRT && blockState.getMeta() == 0) {
                if (blockState.up().isTransparent()) {
                    BlockSpreadEvent ev = new BlockSpreadEvent(blockState, this, BlockState.get(MYCELIUM));
                    Server.getInstance().getPluginManager().callEvent(ev);
                    if (!ev.isCancelled()) {
                        this.getLevel().setBlock(blockState.getPosition(), ev.getNewState());
                    }
                }
            }
        }
        return 0;
    }

    @Override
    public BlockColor getColor(BlockState state) {
        return BlockColor.PURPLE_BLOCK_COLOR;
    }

    @Override
    public boolean canSilkTouch() {
        return true;
    }
}
