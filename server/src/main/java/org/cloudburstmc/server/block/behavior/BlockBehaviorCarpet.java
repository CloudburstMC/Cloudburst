package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.data.DyeColor;

import static org.cloudburstmc.server.block.BlockTypes.AIR;

public class BlockBehaviorCarpet extends FloodableBlockBehavior {


    @Override
    public float getResistance() {
        return 0.5f;
    }

    @Override
    public boolean isSolid() {
        return true;
    }

    @Override
    public boolean canPassThrough() {
        return false;
    }

//    @Override
//    public float getMaxY() {
//        return this.getY() + 0.0625f;
//    }

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        Block down = block.down();
        if (down.getState().getType() != AIR) {
            placeBlock(block, item);
            return true;
        }
        return false;
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL) {
            if (block.down().getState().getType() == AIR) {
                block.getLevel().useBreakOn(block.getPosition());

                return Level.BLOCK_UPDATE_NORMAL;
            }
        }

        return 0;
    }

    @Override
    public BlockColor getColor(Block block) {
        return getDyeColor(block).getColor();
    }

    public DyeColor getDyeColor(Block block) {
        return block.getState().ensureTrait(BlockTraits.COLOR);
    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }
}
