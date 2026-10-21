package org.cloudburstmc.api.block.component;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.FertilizationResult;
import org.cloudburstmc.api.player.Player;

import java.util.random.RandomGenerator;

/**
 * Proposes block changes and item drops from fertilization without applying them.
 */
@FunctionalInterface
public interface FertilizeBlockHandler {

    /**
     * Determines the changes produced by one fertilization attempt.
     *
     * @param block  the block being fertilized
     * @param player the player using fertilizer, or {@code null} for a non-player cause
     * @param random the random source for the attempt
     * @return the proposed result, or {@link FertilizationResult#none()} when fertilization is unsupported
     */
    FertilizationResult execute(Block block, @Nullable Player player, RandomGenerator random);
}
