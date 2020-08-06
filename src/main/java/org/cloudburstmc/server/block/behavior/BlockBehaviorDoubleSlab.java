package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.block.trait.BlockTrait;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.registry.ItemRegistry;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Identifier;
import org.cloudburstmc.server.utils.data.StoneSlabType;

import static org.cloudburstmc.server.block.behavior.BlockBehaviorSlab.COLORS;

public class BlockBehaviorDoubleSlab extends BlockBehaviorSolid {

    protected Identifier slabType;
    protected BlockTrait typeTrait;

    public BlockBehaviorDoubleSlab(Identifier slabType, BlockTrait<?> typeTrait) {
        this.slabType = slabType;
        this.typeTrait = typeTrait;
    }

    @Override
    public float getResistance() {
        return 30;
    }

    @Override
    public float getHardness() {
        return 2;
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_PICKAXE;
    }

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }

    @Override
    public Item[] getDrops(Block block, Item hand) {
        if (hand.isPickaxe() && hand.getTier() >= ItemTool.TIER_WOODEN) {
            return new Item[]{ItemRegistry.get().getItem(BlockState.get(slabType).withTrait(typeTrait, block.getState().ensureTrait(typeTrait)))};
        } else {
            return new Item[0];
        }
    }

    @Override
    public BlockColor getColor(Block block) {
        StoneSlabType type = block.getState().ensureTrait(BlockTraits.STONE_SLAB_TYPE);
        return COLORS.get(type);
    }
}
