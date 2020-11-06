package org.cloudburstmc.server.item;

import org.cloudburstmc.server.utils.Identifier;

@FunctionalInterface
public interface ItemFactory {

    ItemStack create(Identifier identifier);
}
