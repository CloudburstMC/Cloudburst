package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import lombok.val;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockCategory;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.item.behavior.Item;
import org.cloudburstmc.server.world.World;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;

import static org.cloudburstmc.server.block.BlockIds.COBBLESTONE_WALL;

public class BlockBehaviorTorch extends FloodableBlockBehavior {

    @Override
    public int getLightLevel(Block block) {
        return 14;
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == World.BLOCK_UPDATE_NORMAL) {
            BlockState below = block.down().getState();

            val direction = block.getState().ensureTrait(BlockTraits.TORCH_DIRECTION);
            if (block.getSide(direction.getOpposite()).getState().inCategory(BlockCategory.TRANSPARENT)
                    && !(direction == Direction.UP && (below.inCategory(BlockCategory.FENCE) || below.getType() == COBBLESTONE_WALL))) {
                block.getWorld().useBreakOn(block.getPosition());

                return World.BLOCK_UPDATE_NORMAL;
            }
        }

        return 0;
    }

    @Override
    public boolean place(Item item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        BlockState below = block.down().getState();

        if (block.getState().inCategory(BlockCategory.LIQUID)) {
            return false;
        }

        if (!target.getState().inCategory(BlockCategory.TRANSPARENT) && face != Direction.DOWN) {
            return placeBlock(block, item.getBlock().withTrait(BlockTraits.TORCH_DIRECTION, face.getOpposite()));
        } else if (!below.inCategory(BlockCategory.TRANSPARENT) || below.inCategory(BlockCategory.FENCE) || below.getType() == COBBLESTONE_WALL) {
            return placeBlock(block, item.getBlock());
        }
        return false;
    }

    @Override
    public Item toItem(Block block) {
        return Item.get(block.getState().defaultState());
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
