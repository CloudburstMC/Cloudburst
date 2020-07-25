package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;

public class BlockBehaviorStrippedLog extends BlockBehaviorLog {

    @Override
    public boolean canBeActivated() {
        return false;
    }

    @Override
    public boolean place(Item item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        this.setMeta(FACES[face.getIndex()] >> 2);
        return this.getLevel().setBlock(this.getPosition(), this, true, true);
    }

    @Override
    public Item toItem(BlockState state) {
        return Item.get(id);
    }
}
