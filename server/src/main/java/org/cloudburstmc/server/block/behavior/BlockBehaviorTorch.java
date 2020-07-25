package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.math.BlockFace;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;

import static org.cloudburstmc.server.block.BlockTypes.COBBLESTONE_WALL;

public class BlockBehaviorTorch extends FloodableBlockBehavior {

    @Override
    public int getLightLevel() {
        return 14;
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL) {
            BlockState below = this.down();
            int side = this.getMeta();
            int[] faces = new int[]{
                    0, //0
                    4, //1
                    5, //2
                    2, //3
                    3, //4
                    0, //5
                    0  //6
            };

            if (this.getSide(BlockFace.fromIndex(faces[side])).isTransparent() && !(side == 0 && (below instanceof BlockBehaviorFence || below.getId() == COBBLESTONE_WALL))) {
                this.getLevel().useBreakOn(this.getPosition());

                return Level.BLOCK_UPDATE_NORMAL;
            }
        }

        return 0;
    }

    @Override
    public boolean place(Item item, Block block, Block target, BlockFace face, Vector3f clickPos, Player player) {
        BlockState below = this.down();

        if (!target.isTransparent() && face != BlockFace.DOWN) {
            int[] faces = new int[]{
                    0, //0, nerver used
                    5, //1
                    4, //2
                    3, //3
                    2, //4
                    1, //5
            };
            this.setMeta(faces[face.getIndex()]);
            this.getLevel().setBlock(blockState.getPosition(), this, true, true);

            return true;
        } else if (!below.isTransparent() || below instanceof BlockBehaviorFence || below.getId() == COBBLESTONE_WALL) {
            this.setMeta(0);
            this.getLevel().setBlock(blockState.getPosition(), this, true, true);

            return true;
        }
        return false;
    }

    @Override
    public Item toItem(BlockState state) {
        return Item.get(id, 0);
    }

    @Override
    public BlockColor getColor(BlockState state) {
        return BlockColor.AIR_BLOCK_COLOR;
    }

    @Override
    public BlockFace getBlockFace() {
        return getBlockFace(this.getMeta() & 0x07);
    }

    public BlockFace getBlockFace(int meta) {
        switch (meta) {
            case 1:
                return BlockFace.EAST;
            case 2:
                return BlockFace.WEST;
            case 3:
                return BlockFace.SOUTH;
            case 4:
                return BlockFace.NORTH;
            default:
                return BlockFace.UP;
        }
    }

}
