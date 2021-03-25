package org.cloudburstmc.server.block.behavior;

import lombok.val;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.trait.BlockTrait;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.TierTypes;
import org.cloudburstmc.api.util.data.BlockColor;
import org.cloudburstmc.api.util.data.StoneSlabType;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import static org.cloudburstmc.server.block.behavior.BlockBehaviorSlab.COLORS;

public class BlockBehaviorDoubleSlab extends BlockBehaviorSolid {

    protected BlockType slabType;
    @SuppressWarnings("rawtypes")
    protected BlockTrait typeTrait;

    public BlockBehaviorDoubleSlab(BlockType slabType) {
        this(slabType, null);
    }

    public BlockBehaviorDoubleSlab(BlockType slabType, BlockTrait<?> typeTrait) {
        this.slabType = slabType;
        this.typeTrait = typeTrait;
    }


    @SuppressWarnings("unchecked")
    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        val behavior = hand.getBehavior();
        if (behavior.isPickaxe() && behavior.getTier(hand).compareTo(TierTypes.WOOD) > 0) {
            BlockState state = CloudBlockRegistry.get().getBlock(slabType);
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
