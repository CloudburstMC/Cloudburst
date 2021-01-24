package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import lombok.val;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockCategory;
import org.cloudburstmc.server.block.BlockIds;
import org.cloudburstmc.server.item.behavior.Item;
import org.cloudburstmc.server.item.behavior.ItemIds;
import org.cloudburstmc.server.world.World;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;

import java.util.Random;

public class BlockBehaviorDeadBush extends FloodableBlockBehavior {

    @Override
    public boolean canBeReplaced(Block block) {
        return true;
    }

    @Override
    public boolean place(Item item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        val down = block.down().getState().getType();
        if (down == BlockIds.SAND || down == BlockIds.HARDENED_CLAY || down == BlockIds.STAINED_HARDENED_CLAY ||
                down == BlockIds.DIRT || down == BlockIds.PODZOL) {
            placeBlock(block, item);
            return true;
        }
        return false;
    }


    @Override
    public int onUpdate(Block block, int type) {
        if (type == World.BLOCK_UPDATE_NORMAL) {
            if (block.down().getState().inCategory(BlockCategory.TRANSPARENT)) {
                removeBlock(block, true);
                return World.BLOCK_UPDATE_NORMAL;
            }
        }
        return 0;
    }

    @Override
    public Item[] getDrops(Block block, Item hand) {
        if (hand.isShears()) {
            return new Item[]{
                    toItem(block)
            };
        } else {
            return new Item[]{
                    Item.get(ItemIds.STICK, 0, new Random().nextInt(3))
            };
        }
    }

    public BlockColor getColor(Block block) {
        return BlockColor.FOLIAGE_BLOCK_COLOR;
    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }
}
