package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import lombok.val;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockStates;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorNetherPortal extends FloodableBlockBehavior {

    @Override
    public boolean canPassThrough() {
        return true;
    }

    @Override
    public boolean isBreakable(Item item) {
        return false;
    }

    public static void spawnPortal(Vector3f pos, Level level) {
        int x = pos.getFloorX(), y = pos.getFloorY(), z = pos.getFloorZ();

        for (int xx = -1; xx < 4; xx++) {
            for (int yy = 1; yy < 4; yy++) {
                for (int zz = -1; zz < 3; zz++) {
                    level.setBlockAt(x + xx, y + yy, z + zz, BlockStates.AIR);
                    level.setBlockAt(x + xx, y + yy, z + zz, 1, BlockStates.AIR);
                }
            }
        }

        val obsidian = BlockState.get(BlockTypes.OBSIDIAN);
        val portal = BlockState.get(BlockTypes.PORTAL);

        level.setBlockAt(x + 1, y, z, obsidian);
        level.setBlockAt(x + 2, y, z, obsidian);

        z += 1;
        level.setBlockAt(x, y, z, obsidian);
        level.setBlockAt(x + 1, y, z, obsidian);
        level.setBlockAt(x + 2, y, z, obsidian);
        level.setBlockAt(x + 3, y, z, obsidian);

        z += 1;
        level.setBlockAt(x + 1, y, z, obsidian);
        level.setBlockAt(x + 2, y, z, obsidian);

        z -= 1;
        y += 1;
        level.setBlockAt(x, y, z, obsidian);
        level.setBlockAt(x + 1, y, z, portal);
        level.setBlockAt(x + 2, y, z, portal);
        level.setBlockAt(x + 3, y, z, obsidian);

        y += 1;
        level.setBlockAt(x, y, z, obsidian);
        level.setBlockAt(x + 1, y, z, portal);
        level.setBlockAt(x + 2, y, z, portal);
        level.setBlockAt(x + 3, y, z, obsidian);

        y += 1;
        level.setBlockAt(x, y, z, obsidian);
        level.setBlockAt(x + 1, y, z, portal);
        level.setBlockAt(x + 2, y, z, portal);
        level.setBlockAt(x + 3, y, z, obsidian);

        y += 1;
        level.setBlockAt(x, y, z, obsidian);
        level.setBlockAt(x + 1, y, z, obsidian);
        level.setBlockAt(x + 2, y, z, obsidian);
        level.setBlockAt(x + 3, y, z, obsidian);
    }

    @Override
    public int getLightLevel(Block block) {
        return 11;
    }

    @Override
    public float getHardness() {
        return -1;
    }

    @Override
    public boolean onBreak(Block block, Item item) {
        boolean result = super.onBreak(block, item);
        for (Direction face : Direction.values()) {
            Block b = block.getSide(face);
            if (b instanceof BlockBehaviorNetherPortal) {
                result &= b.getState().getBehavior().onBreak(b, item);
            }
        }
        return result;
    }

    @Override
    public boolean hasEntityCollision() {
        return true;
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.AIR_BLOCK_COLOR;
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }

//    @Override //TODO: bounding box
//    protected AxisAlignedBB recalculateBoundingBox() {
//        return this;
//    }

    @Override
    public Item toItem(Block block) {
        return Item.get(BlockTypes.AIR, 0, 0);
    }
}
