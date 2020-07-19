package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.math.BlockFace;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.Faceable;
import org.cloudburstmc.server.utils.Identifier;

/**
 * Created by Leonidius20 on 18.08.18.
 */
public class BlockBehaviorObserver extends BlockBehaviorSolid implements Faceable {

    public BlockBehaviorObserver(Identifier id) {
        super(id);
    }

    @Override
    public boolean place(Item item, BlockState blockState, BlockState target, BlockFace face, Vector3f clickPos, Player player) {
        int[] faces = {2, 5, 3, 4};
        this.setMeta(faces[player != null ? player.getDirection().getHorizontalIndex() : 0]);
        this.getLevel().setBlock(blockState.getPosition(), this, true, true);
        return true;
    }

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_PICKAXE;
    }

    @Override
    public float getHardness() {
        return 3.5f;
    }

    @Override
    public float getResistance() {
        return 17.5f;
    }

    @Override
    public BlockFace getBlockFace() {
        return BlockFace.fromHorizontalIndex(this.getMeta() & 0x07);
    }

}
