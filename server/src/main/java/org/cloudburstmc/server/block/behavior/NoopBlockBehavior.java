package org.cloudburstmc.server.block.behavior;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.behavior.BlockBehavior;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class NoopBlockBehavior extends BlockBehavior {
    public static final NoopBlockBehavior INSTANCE = new NoopBlockBehavior();

    @Override
    public boolean placeBlock(Block block, BlockState newState, boolean update) {
        return false;
    }

    @Override
    public float getBreakTime(BlockState state, ItemStack item, Player player) {
        return 0;
    }
}
