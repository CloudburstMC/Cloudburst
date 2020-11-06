package org.cloudburstmc.server.block.behavior;

import lombok.val;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.player.Player;

public class BlockBehaviorRedstoneRepeater extends BlockBehaviorRedstoneDiode {

    @Override
    protected boolean isAlternateInput(Block block) {
        return isDiode(block.getState().getBehavior());
    }

    @Override
    public ItemStack toItem(Block block) {
        return ItemStack.get(ItemTypes.REPEATER);
    }

    @Override
    protected int getDelay(BlockState state) {
        return (1 + state.ensureTrait(BlockTraits.REPEATER_DELAY)) * 2;
    }


    @Override
    public boolean onActivate(Block block, ItemStack item, Player player) {
        val state = block.getState();

        block.set(state.withTrait(BlockTraits.REPEATER_DELAY, (state.ensureTrait(BlockTraits.REPEATER_DELAY) + 1) % 4), true, false);
        return true;
    }

    @Override
    public boolean isLocked(Block block) {
        return this.getPowerOnSides(block) > 0;
    }
}
