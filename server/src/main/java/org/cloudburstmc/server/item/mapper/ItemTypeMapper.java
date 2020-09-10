package org.cloudburstmc.server.item.mapper;

import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.utils.Identifier;

public interface ItemTypeMapper {

    Identifier convert(ItemStack item);
}
