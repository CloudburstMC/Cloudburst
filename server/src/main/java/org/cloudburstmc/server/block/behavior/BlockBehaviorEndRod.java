package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.item.behavior.Item;
import org.cloudburstmc.server.item.behavior.ItemTool;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.ItemRegistry;

public class BlockBehaviorEndRod extends BlockBehaviorTransparent {

    @Override
    public float getHardness() {
        return 0;
    }

    @Override
    public float getResistance() {
        return 0;
    }

    @Override
    public int getLightLevel(Block block) {
        return 14;
    }

    @Override
    public boolean canBePushed() {
        return true;
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_PICKAXE;
    }

//    @Override
//    public float getMinX() {
//        return this.getX() + 0.4f;
//    }
//
//    @Override
//    public float getMinZ() {
//        return this.getZ() + 0.4f;
//    }
//
//    @Override
//    public float getMaxX() {
//        return this.getX() + 0.6f;
//    }
//
//    @Override
//    public float getMaxZ() {
//        return this.getZ() + 0.6f;
//    }

    @Override
    public boolean place(Item item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        placeBlock(block, item.getBlock().withTrait(BlockTraits.FACING_DIRECTION, player != null ? player.getHorizontalDirection() : Direction.NORTH));
        return true;
    }

    @Override
    public Item toItem(Block block) {
        return ItemRegistry.get().getItem(block.getState().defaultState());
    }

    @Override
    public boolean canWaterlogFlowing() {
        return true;
    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }
}
