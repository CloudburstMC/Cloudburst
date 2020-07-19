package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.math.BlockFace;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.Identifier;

public class BlockBehaviorStrippedLog extends BlockBehaviorLog {

    public BlockBehaviorStrippedLog(Identifier identifier) {
        super(identifier);
    }

    @Override
    public boolean canBeActivated() {
        return false;
    }

    @Override
    public boolean place(Item item, BlockState blockState, BlockState target, BlockFace face, Vector3f clickPos, Player player) {
        this.setMeta(FACES[face.getIndex()] >> 2);
        return this.getLevel().setBlock(this.getPosition(), this, true, true);
    }

    @Override
    public Item toItem() {
        return Item.get(id);
    }
}
