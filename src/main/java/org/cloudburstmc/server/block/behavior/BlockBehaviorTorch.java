package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import lombok.val;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockCategory;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.BlockColor;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.player.CloudPlayer;

import static org.cloudburstmc.api.block.BlockTypes.STONE_WALL;

public class BlockBehaviorTorch extends FloodableBlockBehavior {


    @Override
    public int onUpdate(Block block, int type) {
        if (type == CloudLevel.BLOCK_UPDATE_NORMAL) {
            BlockState below = block.down().getState();

            val direction = block.getState().ensureTrait(BlockTraits.TORCH_DIRECTION);
            if (block.getSide(direction.getOpposite()).getState().inCategory(BlockCategory.TRANSPARENT)
                    && !(direction == Direction.UP && (below.inCategory(BlockCategory.FENCE) || below.getType() == STONE_WALL))) {
                block.getLevel().useBreakOn(block.getPosition());

                return CloudLevel.BLOCK_UPDATE_NORMAL;
            }
        }

        return 0;
    }

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, CloudPlayer player) {
        BlockState below = block.down().getState();

        if (block.getState().inCategory(BlockCategory.LIQUID)) {
            return false;
        }

        if (!target.getState().inCategory(BlockCategory.TRANSPARENT) && face != Direction.DOWN) {
            return placeBlock(block, item.getBehavior().getBlock(item).withTrait(BlockTraits.TORCH_DIRECTION, face.getOpposite()));
        } else if (!below.inCategory(BlockCategory.TRANSPARENT) || below.inCategory(BlockCategory.FENCE) || below.getType() == STONE_WALL) {
            return placeBlock(block, item.getBehavior().getBlock(item));
        }
        return false;
    }

    @Override
    public int getLightLevel(Block block) {
        return block.getState().ensureTrait(BlockTraits.IS_SOUL) ? 10 : 14;
    }

    @Override
    public ItemStack toItem(Block block) {
        return ItemStack.get(block.getState().defaultState());
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.AIR_BLOCK_COLOR;
    }

    public Direction getBlockFace(BlockState state) {
        return state.ensureTrait(BlockTraits.TORCH_DIRECTION);
    }

    public Direction getBlockFace(int meta) {
        switch (meta) {
            case 1:
                return Direction.EAST;
            case 2:
                return Direction.WEST;
            case 3:
                return Direction.SOUTH;
            case 4:
                return Direction.NORTH;
            default:
                return Direction.UP;
        }
    }

}
