package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.Block;

@FunctionalInterface
public interface GenericBlockHandler<T> {

    T execute(Block block);
}
