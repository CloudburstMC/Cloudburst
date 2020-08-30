package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.item.behavior.Item;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;

public class BlockBehaviorStrippedLog extends BlockBehaviorLog {

    @Override
    public boolean canBeActivated(Block block) {
        return false;
    }

    @Override
    public boolean place(Item item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        return placeBlock(block, item.getBlock().withTrait(BlockTraits.AXIS, face.getAxis()));
    }

    @Override
    public Item toItem(Block block) {
        return Item.get(block.getState().resetTrait(BlockTraits.AXIS));
    }
}
