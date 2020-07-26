package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.BlockRegistry;

public class BlockBehaviorLog2 extends BlockBehaviorLog {


    public static void upgradeLegacyBlock(int[] blockState) {
        if ((blockState[1] & 0b1100) == 0b1100) { // old full bark texture
            blockState[0] = BlockRegistry.get().getLegacyId(BlockTypes.WOOD);
            blockState[1] = (blockState[1] & 0x03) + 4; // gets only the log type and set pillar to y
        }
    }

    @Override
    public Item toItem(Block block) {
        if ((getMeta() & 0b1100) == 0b1100) {
            return Item.get(BlockTypes.WOOD, this.getMeta() & 0x3 + 4);
        } else {
            return Item.get(id, this.getMeta() & 0x03);
        }
    }

    @Override
    public boolean place(Item item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        // Convert the old log bark to the new wood block
        if ((this.getMeta() & 0b1100) == 0b1100) {
            BlockState woodBlockState = BlockState.get(BlockTypes.WOOD, (this.getMeta() & 0x01) + 4, this.getPosition(), this.getLevel());
            return woodBlockState.place(item, blockState, target, face, clickPos, player);
        }

        return super.place(item, blockState, target, face, clickPos, player);
    }
}
