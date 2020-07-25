package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemIds;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;

import java.util.Random;

public class BlockBehaviorDeadBush extends FloodableBlockBehavior {

    @Override
    public boolean canBeReplaced() {
        return true;
    }

    @Override
    public boolean place(Item item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        BlockState down = this.down();
        if (down.getId() == BlockTypes.SAND || down.getId() == BlockTypes.HARDENED_CLAY || down.getId() == BlockTypes.STAINED_HARDENED_CLAY ||
                down.getId() == BlockTypes.DIRT || down.getId() == BlockTypes.PODZOL) {
            this.getLevel().setBlock(block.getPosition(), this, true, true);
            return true;
        }
        return false;
    }


    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL) {
            if (this.down().isTransparent()) {
                this.getLevel().useBreakOn(this.getPosition());

                return Level.BLOCK_UPDATE_NORMAL;
            }
        }
        return 0;
    }

    @Override
    public Item[] getDrops(BlockState blockState, Item hand) {
        if (hand.isShears()) {
            return new Item[]{
                    toItem(blockState)
            };
        } else {
            return new Item[]{
                    Item.get(ItemIds.STICK, 0, new Random().nextInt(3))
            };
        }
    }

    public BlockColor getColor(BlockState state) {
        return BlockColor.FOLIAGE_BLOCK_COLOR;
    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }
}
