package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.server.registry.CloudItemRegistry;

public class BlockBehaviorRedstoneRepeater extends BlockBehaviorRedstoneDiode {

    @Override
    protected boolean isAlternateInput(Block block) {
        return isDiode(block.getState().getBehavior());
    }

    @Override
    public ItemStack toItem(Block block) {
        return CloudItemRegistry.get().getItem(ItemTypes.REPEATER);
    }

    @Override
    protected int getDelay(BlockState state) {
        return (1 + state.ensureTrait(BlockTraits.REPEATER_DELAY)) * 2;
    }


    @Override
    public boolean onActivate(Block block, ItemStack item, Player player) {
        var state = block.getState();

        block.set(state.withTrait(BlockTraits.REPEATER_DELAY, (state.ensureTrait(BlockTraits.REPEATER_DELAY) + 1) % 4), true, false);
        return true;
    }

    @Override
    public boolean isLocked(Block block) {
        return this.getPowerOnSides(block) > 0;
    }
}
