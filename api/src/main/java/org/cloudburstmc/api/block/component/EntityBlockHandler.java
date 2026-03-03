package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.entity.Entity;

@FunctionalInterface
public interface EntityBlockHandler {

    void execute(Block block, Entity entity);
}
