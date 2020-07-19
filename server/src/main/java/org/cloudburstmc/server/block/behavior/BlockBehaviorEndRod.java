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
 * http://minecraft.gamepedia.com/End_Rod
 *
 * @author PikyCZ
 */
public class BlockBehaviorEndRod extends BlockBehaviorTransparent implements Faceable {

    public BlockBehaviorEndRod(Identifier id) {
        super(id);
    }

    @Override
    public float getHardness() {
        return 0;
    }

    @Override
    public float getResistance() {
        return 0;
    }

    @Override
    public int getLightLevel() {
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

    @Override
    public float getMinX() {
        return this.getX() + 0.4f;
    }

    @Override
    public float getMinZ() {
        return this.getZ() + 0.4f;
    }

    @Override
    public float getMaxX() {
        return this.getX() + 0.6f;
    }

    @Override
    public float getMaxZ() {
        return this.getZ() + 0.6f;
    }

    @Override
    public boolean place(Item item, BlockState blockState, BlockState target, BlockFace face, Vector3f clickPos, Player player) {
        int[] faces = {0, 1, 3, 2, 5, 4};
        this.setMeta(faces[player != null ? face.getIndex() : 0]);
        this.getLevel().setBlock(blockState.getPosition(), this, true, true);

        return true;
    }

    @Override
    public Item toItem() {
        return Item.get(id, 0);
    }

    @Override
    public BlockFace getBlockFace() {
        return BlockFace.fromHorizontalIndex(this.getMeta() & 0x07);
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
