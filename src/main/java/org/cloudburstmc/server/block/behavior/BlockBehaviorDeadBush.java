package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import lombok.val;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockCategory;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;

import java.util.Random;

public class BlockBehaviorDeadBush extends FloodableBlockBehavior {

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        val down = block.down().getState().getType();
        if (down == BlockTypes.SAND || down == BlockTypes.HARDENED_CLAY || down == BlockTypes.STAINED_HARDENED_CLAY ||
                down == BlockTypes.DIRT || down == BlockTypes.PODZOL) {
            placeBlock(block, item);
            return true;
        }
        return false;
    }


    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL) {
            if (block.down().getState().inCategory(BlockCategory.TRANSPARENT)) {
                removeBlock(block, true);
                return Level.BLOCK_UPDATE_NORMAL;
            }
        }
        return 0;
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        if (hand.getBehavior().isShears()) {
            return new ItemStack[]{
                    toItem(block)
            };
        } else {
            return new ItemStack[]{
                    ItemStack.get(ItemTypes.STICK, new Random().nextInt(3))
            };
        }
    }

    public BlockColor getColor(Block block) {
        return BlockColor.FOLIAGE_BLOCK_COLOR;
    }


}
