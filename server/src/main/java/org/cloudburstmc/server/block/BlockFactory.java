package org.cloudburstmc.server.block;

import org.cloudburstmc.server.utils.Identifier;

@FunctionalInterface
public interface BlockFactory {

    BlockState create(Identifier id);
}
