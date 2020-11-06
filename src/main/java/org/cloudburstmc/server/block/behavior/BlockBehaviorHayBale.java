package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorHayBale extends BlockBehaviorSolid {


    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        return placeBlock(block, item.getBehavior().getBlock(item).withTrait(BlockTraits.AXIS, face.getAxis()));
    }

    @Override
    public BlockColor getColor(Block state) {
        return BlockColor.YELLOW_BLOCK_COLOR;
    }
}
