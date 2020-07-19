package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.math.BlockFace;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Identifier;

import static org.cloudburstmc.server.block.BlockTypes.PURPUR_BLOCK;

public class BlockBehaviorPurpur extends BlockBehaviorSolid {

    public static final int PURPUR_NORMAL = 0;
    public static final int PURPUR_PILLAR = 2;

    public BlockBehaviorPurpur(Identifier id) {
        super(id);
    }

    @Override
    public float getHardness() {
        return 1.5f;
    }

    @Override
    public float getResistance() {
        return 30;
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_PICKAXE;
    }

    @Override
    public boolean place(Item item, BlockState blockState, BlockState target, BlockFace face, Vector3f clickPos, Player player) {
        if (this.getMeta() != PURPUR_NORMAL) {
            short[] faces = new short[]{
                    0,
                    0,
                    0b1000,
                    0b1000,
                    0b0100,
                    0b0100
            };

            this.setMeta(((this.getMeta() & 0x03) | faces[face.getIndex()]));
        }
        this.getLevel().setBlock(blockState.getPosition(), this, true, true);

        return true;
    }

    @Override
    public Item[] getDrops(Item hand) {
        if (hand.isPickaxe() && hand.getTier() >= ItemTool.TIER_WOODEN) {
            return new Item[]{
                    toItem()
            };
        } else {
            return new Item[0];
        }
    }

    @Override
    public Item toItem() {
        return Item.get(PURPUR_BLOCK, this.getMeta() & 0x03, 1);
    }

    @Override
    public BlockColor getColor() {
        return BlockColor.MAGENTA_BLOCK_COLOR;
    }
}
