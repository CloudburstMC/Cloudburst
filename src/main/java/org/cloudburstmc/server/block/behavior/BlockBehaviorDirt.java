package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Identifier;

/**
 * author: MagicDroidX
 * AMAZING COARSE DIRT added by kvetinac97
 * Nukkit Project
 */
public class BlockBehaviorDirt extends BlockBehaviorSolid {

    public BlockBehaviorDirt(Identifier id) {
        super(id);
    }

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    public float getResistance() {
        return 2.5f;
    }

    @Override
    public float getHardness() {
        return 0.5f;
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_SHOVEL;
    }

    @Override
    public boolean onActivate(Item item, Player player) {
        if (item.isHoe()) {
            item.useOn(this);
            this.getLevel().setBlock(this.getPosition(), this.getMeta() == 0 ? BlockState.get(BlockTypes.FARMLAND) : BlockState.get(BlockTypes.DIRT), true);

            return true;
        }

        return false;
    }

    @Override
    public Item[] getDrops(Item hand) {
        return new Item[]{Item.get(BlockTypes.DIRT)};
    }

    @Override
    public BlockColor getColor() {
        return BlockColor.DIRT_BLOCK_COLOR;
    }
}
