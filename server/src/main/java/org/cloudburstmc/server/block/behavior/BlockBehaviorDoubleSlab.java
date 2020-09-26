package org.cloudburstmc.server.block.behavior;

import lombok.val;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.block.BlockType;
import org.cloudburstmc.server.block.trait.BlockTrait;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.TierTypes;
import org.cloudburstmc.server.item.ToolType;
import org.cloudburstmc.server.item.behavior.ItemToolBehavior;
import org.cloudburstmc.server.registry.CloudItemRegistry;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.data.StoneSlabType;

import static org.cloudburstmc.server.block.behavior.BlockBehaviorSlab.COLORS;

public class BlockBehaviorDoubleSlab extends BlockBehaviorSolid {

    protected BlockType slabType;
    protected BlockTrait typeTrait;

    public BlockBehaviorDoubleSlab(BlockType slabType) {
        this(slabType, null);
    }

    public BlockBehaviorDoubleSlab(BlockType slabType, BlockTrait<?> typeTrait) {
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
    public ToolType getToolType() {
        return ItemToolBehavior.TYPE_PICKAXE;
    }

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        val behavior = hand.getBehavior();
        if (behavior.isPickaxe() && behavior.getTier(hand).compareTo(TierTypes.WOOD) > 0) {
            BlockState state = BlockState.get(slabType);
            if (typeTrait != null) {
                state = state.withTrait(typeTrait, block.getState().ensureTrait(typeTrait));
            }

            return new ItemStack[]{CloudItemRegistry.get().getItem(state)};
        } else {
            return new ItemStack[0];
        }
    }

    @Override
    public BlockColor getColor(Block block) {
        StoneSlabType type = block.getState().ensureTrait(BlockTraits.STONE_SLAB_TYPE);
        return COLORS.get(type);
    }
}
