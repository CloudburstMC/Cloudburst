package org.cloudburstmc.server.item;

import org.cloudburstmc.api.util.Identifier;

@FunctionalInterface
public interface ItemFactory {

    ItemStack create(Identifier identifier);
}
