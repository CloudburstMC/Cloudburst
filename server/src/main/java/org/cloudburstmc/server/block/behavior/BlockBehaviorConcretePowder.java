package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;

import static org.cloudburstmc.server.block.BlockTypes.*;

public class BlockBehaviorConcretePowder extends BlockBehaviorFallable {

    @Override
    public float getResistance() {
        return 2.5f;
    }

    @Override
    public float getHardness() {
        return 0.5f;
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_SHOVEL;
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL) {
            super.onUpdate(Level.BLOCK_UPDATE_NORMAL);

            for (int side = 1; side <= 5; side++) {
                BlockState blockState = this.getSide(Direction.fromIndex(side));
                if (blockState.getId() == FLOWING_WATER || blockState.getId() == WATER || blockState.getId() == FLOWING_LAVA || blockState.getId() == LAVA) {
                    this.level.setBlock(this.getPosition(), BlockState.get(CONCRETE, this.meta), true, true);
                }
            }

            return Level.BLOCK_UPDATE_NORMAL;
        }
        return 0;
    }

    @Override
    public boolean place(Item item, BlockState b, BlockState target, Direction face, Vector3f clickPos, Player player) {
        boolean concrete = false;

        for (int side = 1; side <= 5; side++) {
            BlockState blockState = this.getSide(Direction.fromIndex(side));
            if (blockState.getId() == FLOWING_WATER || blockState.getId() == WATER || blockState.getId() == FLOWING_LAVA || blockState.getId() == LAVA) {
                concrete = true;
                break;
            }
        }

        if (concrete) {
            this.level.setBlock(this.getPosition(), BlockState.get(CONCRETE, this.getMeta()), true, true);
        } else {
            this.level.setBlock(this.getPosition(), this, true, true);
        }

        return true;
    }
}
