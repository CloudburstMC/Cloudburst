package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.GenericMath;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.blockentity.BlockEntity;
import org.cloudburstmc.server.blockentity.Chest;
import org.cloudburstmc.server.math.Direction;

public class BlockBehaviorTrappedChest extends BlockBehaviorChest {

    @Override
    public int getWeakPower(Block block, Direction face) {
        int playerCount = 0;

        BlockEntity blockEntity = block.getLevel().getBlockEntity(block.getPosition());

        if (blockEntity instanceof Chest) {
            playerCount = ((Chest) blockEntity).getInventory().getViewers().size();
        }

        return GenericMath.clamp(playerCount, 0, 15);
    }

    @Override
    public int getStrongPower(Block block, Direction side) {
        return side == Direction.UP ? this.getWeakPower(block, side) : 0;
    }


}
