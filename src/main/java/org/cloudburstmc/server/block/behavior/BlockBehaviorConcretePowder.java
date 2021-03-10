package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockCategory;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.math.Direction.Plane;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.data.DyeColor;

import static org.cloudburstmc.api.block.BlockTypes.CONCRETE;

public class BlockBehaviorConcretePowder extends BlockBehaviorFallable {


    @Override
    public int onUpdate(Block block, int type) {
        if (type == CloudLevel.BLOCK_UPDATE_NORMAL) {
            super.onUpdate(block, CloudLevel.BLOCK_UPDATE_NORMAL);

            if (checkLiquid(block)) {
                block.set(BlockState.get(CONCRETE).withTrait(BlockTraits.COLOR, block.getState().ensureTrait(BlockTraits.COLOR)));
            }

            return CloudLevel.BLOCK_UPDATE_NORMAL;
        }
        return 0;
    }

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, CloudPlayer player) {
        if (checkLiquid(block)) {
            placeBlock(block, BlockState.get(CONCRETE).withTrait(BlockTraits.COLOR, item.getBehavior().getBlock(item).ensureTrait(BlockTraits.COLOR)));
        } else {
            placeBlock(block, item);
        }

        return true;
    }

    private boolean checkLiquid(Block block) {
        for (Direction direction : Plane.HORIZONTAL) {
            Block side = block.getSide(direction);

            if (side.getState().inCategory(BlockCategory.LIQUID)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public BlockColor getColor(Block block) {
        return getDyeColor(block).getColor();
    }

    public DyeColor getDyeColor(Block block) {
        return block.getState().ensureTrait(BlockTraits.COLOR);
    }
}
