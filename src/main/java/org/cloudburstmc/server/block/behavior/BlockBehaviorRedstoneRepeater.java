package org.cloudburstmc.server.block.behavior;

import lombok.val;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockIds;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.item.ItemIds;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.Identifier;

public class BlockBehaviorRedstoneRepeater extends BlockBehaviorRedstoneDiode {

    public BlockBehaviorRedstoneRepeater(Identifier type) {
        super(type);
        this.isPowered = true;
    }

    @Override
    protected boolean isAlternateInput(Block block) {
        return isDiode(block.getState().getBehavior());
    }

    @Override
    public ItemStack toItem(Block block) {
        return ItemStack.get(ItemIds.REPEATER);
    }

    @Override
    protected int getDelay(BlockState state) {
        return (1 + state.ensureTrait(BlockTraits.REPEATER_DELAY)) * 2;
    }

    @Override
    protected BlockState getPowered(BlockState state) {
        return BlockState.get(BlockIds.POWERED_REPEATER).copyTraits(state);
    }

    @Override
    protected BlockState getUnpowered(BlockState state) {
        return BlockState.get(BlockIds.UNPOWERED_REPEATER).copyTraits(state);
    }

    @Override
    public int getLightLevel(Block block) {
        return 7;
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
