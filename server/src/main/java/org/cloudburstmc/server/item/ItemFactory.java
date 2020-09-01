package org.cloudburstmc.server.item;

import org.cloudburstmc.server.item.behavior.Item;
import org.cloudburstmc.server.utils.Identifier;

@FunctionalInterface
public interface ItemFactory {

    Item create(Identifier identifier);
}
