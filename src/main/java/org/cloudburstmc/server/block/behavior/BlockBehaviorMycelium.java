package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3i;
import lombok.val;
import org.cloudburstmc.server.Server;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockCategory;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.event.block.BlockSpreadEvent;
import org.cloudburstmc.server.item.behavior.Item;
import org.cloudburstmc.server.item.behavior.ItemTool;
import org.cloudburstmc.server.world.World;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.data.DirtType;

import java.util.concurrent.ThreadLocalRandom;

import static org.cloudburstmc.server.block.BlockIds.DIRT;
import static org.cloudburstmc.server.block.BlockIds.MYCELIUM;

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
    public Item[] getDrops(Block block, Item hand) {
        return new Item[]{
                Item.get(DIRT)
        };
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == World.BLOCK_UPDATE_RANDOM) {
            //TODO: light levels
            Vector3i pos = block.getPosition();
            int x = ThreadLocalRandom.current().nextInt(pos.getX() - 1, pos.getX() + 1);
            int y = ThreadLocalRandom.current().nextInt(pos.getY() - 1, pos.getY() + 1);
            int z = ThreadLocalRandom.current().nextInt(pos.getZ() - 1, pos.getZ() + 1);
            Block b = block.getWorld().getBlock(x, y, z);
            val state = b.getState();

            if (state.getType() == DIRT && state.ensureTrait(BlockTraits.DIRT_TYPE) == DirtType.NORMAL) {
                if (b.up().getState().inCategory(BlockCategory.TRANSPARENT)) {
                    BlockSpreadEvent ev = new BlockSpreadEvent(b, block, BlockState.get(MYCELIUM));
                    Server.getInstance().getEventManager().fire(ev);
                    if (!ev.isCancelled()) {
                        b.set(ev.getNewState());
                    }
                }
            }
        }
        return 0;
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.PURPLE_BLOCK_COLOR;
    }

    @Override
    public boolean canSilkTouch() {
        return true;
    }
}
