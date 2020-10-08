package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.*;
import org.cloudburstmc.server.event.block.BlockFadeEvent;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorSnowLayer extends BlockBehaviorFallable {

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        BlockState down = block.down().getState();
        if (down.inCategory(BlockCategory.SOLID)) {
            placeBlock(block, BlockState.get(BlockTypes.SNOW_LAYER));
            return true;
        }
        return false;
    }

    @Override
    public int onUpdate(Block block, int type) {
        super.onUpdate(block, type);
        if (type == Level.BLOCK_UPDATE_RANDOM) {
            if (block.getLevel().getBlockLightAt(block.getX(), block.getY(), block.getZ()) >= 10) {
                BlockFadeEvent event = new BlockFadeEvent(block, BlockStates.AIR);
                block.getLevel().getServer().getEventManager().fire(event);
                if (!event.isCancelled()) {
                    block.set(event.getNewState());
                }
                return Level.BLOCK_UPDATE_NORMAL;
            }
        }
        return 0;
    }

    @Override
    public ItemStack toItem(Block block) {
        return ItemStack.get(ItemTypes.SNOWBALL);
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.SNOW_BLOCK_COLOR;
    }

//    @Override //TODO: bounding box
//    protected AxisAlignedBB recalculateBoundingBox() {
//        return null;
//    }
}


