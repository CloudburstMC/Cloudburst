package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.Block;

import java.util.random.RandomGenerator;

@FunctionalInterface
public interface ResourceCountBlockHandler {

    int execute(Block block, RandomGenerator random, int bonusLevel);
}
