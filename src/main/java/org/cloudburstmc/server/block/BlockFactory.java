package org.cloudburstmc.server.block;

import org.cloudburstmc.api.util.Identifier;

@FunctionalInterface
public interface BlockFactory {

    BlockState create(Identifier id);
}
