package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.item.ItemStack;

import java.util.random.RandomGenerator;

@FunctionalInterface
public interface ResourceBlockHandler {

    ItemStack execute(Block block, RandomGenerator random, int bonusLevel);
}
