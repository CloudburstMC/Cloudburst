package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Identifier;

/**
 * Created by Pub4Game on 26.12.2015.
 */
public class BlockBehaviorTrapdoorIron extends BlockBehaviorTrapdoor {

    public BlockBehaviorTrapdoorIron(Identifier id) {
        super(id);
        this.blockColor = BlockColor.IRON_BLOCK_COLOR;
    }

    @Override
    public float getHardness() {
        return 5;
    }

    @Override
    public float getResistance() {
        return 25;
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_PICKAXE;
    }

    @Override
    public boolean onActivate(Item item, Player player) {
        return false;
    }

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }
}
