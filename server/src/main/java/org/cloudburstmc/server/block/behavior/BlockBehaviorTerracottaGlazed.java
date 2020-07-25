package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;

public class BlockBehaviorTerracottaGlazed extends BlockBehaviorSolid {

    @Override
    public float getResistance() {
        return 7;
    }

    @Override
    public float getHardness() {
        return 1.4f;
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_PICKAXE;
    }

    @Override
    public Item[] getDrops(BlockState blockState, Item hand) {
        return hand.getTier() >= ItemTool.TIER_WOODEN ? new Item[]{this.toItem(blockState)} : new Item[0];
    }

    @Override
    public boolean place(Item item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        int[] faces = {2, 5, 3, 4};
        this.setMeta(faces[player != null ? player.getDirection().getHorizontalIndex() : 0]);
        return this.getLevel().setBlock(blockState.getPosition(), this, true, true);
    }

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }

    @Override
    public Direction getBlockFace() {
        return Direction.fromHorizontalIndex(this.getMeta() & 0x07);
    }
}
