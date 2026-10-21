package org.cloudburstmc.server.block.util;

import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.level.Level;

@UtilityClass
public class BlockDestruction {

    /**
     * Removes a block and its validated partner without resolving a second set of drops.
     * The caller handles the initiating block's break effect.
     */
    public static void removePair(Block block, @Nullable Block partner) {
        if (partner == null) {
            block.set(BlockStates.AIR);
            return;
        }

        BlockState partnerState = partner.getState();
        block.set(BlockStates.AIR, false, false);
        partner.set(BlockStates.AIR, false, false);

        Level level = block.getLevel();
        level.playBlockBreakEffect(partner.getPosition(), partnerState);
        level.updateAround(block.getPosition());
        level.updateAround(partner.getPosition());
    }
}
