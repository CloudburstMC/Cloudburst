package org.cloudburstmc.api.item;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.data.DataKey;
import org.cloudburstmc.api.data.ListDataKey;
import org.cloudburstmc.api.data.SimpleDataKey;
import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.util.Identifier;

public final class ItemKeys {

    public static final SimpleDataKey<BlockState> BLOCK_STATE = DataKey.simple(Identifier.fromString("block_state"), BlockState.class);

    public static final SimpleDataKey<Integer> DAMAGE = DataKey.simple(Identifier.fromString("item_damage"), Integer.class);

    public static final ListDataKey<Enchantment> ENCHANTMENTS = DataKey.list(Identifier.fromString("enchantments"), Enchantment.class);
}
